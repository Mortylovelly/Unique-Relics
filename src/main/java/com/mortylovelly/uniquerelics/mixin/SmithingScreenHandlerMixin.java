package com.mortylovelly.uniquerelics.mixin;

import com.mortylovelly.uniquerelics.UniqueRelics;
import net.minecraft.item.ItemStack;
import net.minecraft.item.trim.ArmorTrim;
import net.minecraft.item.trim.ArmorTrimMaterial;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.screen.SmithingScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.random.Random;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Adds Unique Relics metadata to successful smithing-trim results without
 * replacing the item or overriding the recipe that produced it.
 */
@Mixin(SmithingScreenHandler.class)
public abstract class SmithingScreenHandlerMixin {
    @Shadow
    @Final
    private World world;

    @Unique
    private String uniqueRelics$cachedInputSignature = "";

    @Unique
    private Identifier uniqueRelics$cachedMaterialId;

    /**
     * Decorates the normal smithing preview only when the recipe output already
     * contains a real ArmorTrim. Netherite upgrades and unrelated recipes are
     * therefore left untouched.
     */
    @Inject(method = "updateResult", at = @At("TAIL"))
    private void uniqueRelics$applyRelicPreview(CallbackInfo ci) {
        if (this.world == null || this.world.isClient) {
            return;
        }

        ItemStack template = this.getSlot(0).getStack();
        ItemStack base = this.getSlot(1).getStack();
        ItemStack addition = this.getSlot(2).getStack();
        ItemStack output = this.getSlot(3).getStack();

        if (template.isEmpty() || base.isEmpty() || addition.isEmpty() || output.isEmpty()) {
            this.uniqueRelics$clearCachedRoll();
            return;
        }

        Optional<ArmorTrim> currentTrim = ArmorTrim.getTrim(this.world.getRegistryManager(), output);
        if (currentTrim.isEmpty()) {
            this.uniqueRelics$clearCachedRoll();
            return;
        }

        Registry<ArmorTrimMaterial> materialRegistry =
                this.world.getRegistryManager().get(RegistryKeys.TRIM_MATERIAL);
        List<RegistryEntry.Reference<ArmorTrimMaterial>> materials =
                materialRegistry.streamEntries().toList();

        if (materials.isEmpty()) {
            UniqueRelics.LOGGER.warn("Could not apply a random relic trim: the trim material registry is empty.");
            this.uniqueRelics$clearCachedRoll();
            return;
        }

        String inputSignature = this.uniqueRelics$stackSignature(template)
                + "|" + this.uniqueRelics$stackSignature(base)
                + "|" + this.uniqueRelics$stackSignature(addition);

        if (!inputSignature.equals(this.uniqueRelics$cachedInputSignature)
                || this.uniqueRelics$cachedMaterialId == null) {
            this.uniqueRelics$cachedInputSignature = inputSignature;

            List<RegistryEntry.Reference<ArmorTrimMaterial>> choices = materials.stream()
                    .filter(entry -> entry.getKey()
                            .map(key -> !key.getValue().equals(
                                    currentTrim.get().getMaterial().getKey()
                                            .map(RegistryKey::getValue)
                                            .orElse(null)))
                            .orElse(false))
                    .toList();

            if (choices.isEmpty()) {
                choices = materials;
            }

            RegistryEntry.Reference<ArmorTrimMaterial> chosen =
                    choices.get(Random.create().nextInt(choices.size()));

            this.uniqueRelics$cachedMaterialId = chosen.getKey()
                    .map(RegistryKey::getValue)
                    .orElse(null);
        }

        if (this.uniqueRelics$cachedMaterialId == null) {
            this.uniqueRelics$clearCachedRoll();
            return;
        }

        Optional<RegistryEntry.Reference<ArmorTrimMaterial>> selectedEntry =
                materialRegistry.getEntry(RegistryKey.of(
                        RegistryKeys.TRIM_MATERIAL,
                        this.uniqueRelics$cachedMaterialId
                ));

        if (selectedEntry.isEmpty()) {
            this.uniqueRelics$clearCachedRoll();
            return;
        }

        ArmorTrim randomizedTrim = new ArmorTrim(
                selectedEntry.get(),
                currentTrim.get().getPattern()
        );

        if (!ArmorTrim.apply(this.world.getRegistryManager(), output, randomizedTrim)) {
            UniqueRelics.LOGGER.debug(
                    "The smithing result {} rejected a randomized ArmorTrim; leaving the vanilla result intact.",
                    Registries.ITEM.getId(output.getItem())
            );
            this.uniqueRelics$clearCachedRoll();
            return;
        }

        Identifier templateId = Registries.ITEM.getId(template.getItem());
        Identifier patternId = randomizedTrim.getPattern().getKey()
                .map(RegistryKey::getValue)
                .orElse(null);

        if (templateId == null || patternId == null) {
            this.uniqueRelics$clearCachedRoll();
            return;
        }

        NbtCompound relicData = new NbtCompound();
        relicData.putString("template", templateId.toString());
        relicData.putString("pattern", patternId.toString());
        relicData.putString("material", this.uniqueRelics$cachedMaterialId.toString());
        relicData.putString("rarity", "rare");
        output.getOrCreateNbt().put("unique_relics", relicData);
    }

    /**
     * Adds the player and real forge timestamp at the moment the result is
     * actually taken, not while the result is merely being previewed.
     */
    @Inject(method = "onTakeOutput", at = @At("HEAD"))
    private void uniqueRelics$recordForge(PlayerEntity player, ItemStack stack, CallbackInfo ci) {
        if (player.getWorld().isClient || stack.isEmpty()) {
            return;
        }

        ItemStack template = this.getSlot(0).getStack();
        Optional<ArmorTrim> appliedTrim = ArmorTrim.getTrim(player.getWorld().getRegistryManager(), stack);

        if (template.isEmpty() || appliedTrim.isEmpty()) {
            return;
        }

        Identifier templateId = Registries.ITEM.getId(template.getItem());
        Identifier patternId = appliedTrim.get().getPattern().getKey()
                .map(RegistryKey::getValue)
                .orElse(null);
        Identifier materialId = appliedTrim.get().getMaterial().getKey()
                .map(RegistryKey::getValue)
                .orElse(null);

        if (templateId == null || patternId == null || materialId == null) {
            return;
        }

        NbtCompound relicData = new NbtCompound();
        relicData.putString("template", templateId.toString());
        relicData.putString("pattern", patternId.toString());
        relicData.putString("material", materialId.toString());
        relicData.putString("rarity", "rare");
        relicData.putString("smith", player.getName().getString());
        relicData.putLong("forged_at", Instant.now().toEpochMilli());
        stack.getOrCreateNbt().put("unique_relics", relicData);

        UniqueRelics.LOGGER.info(
                "Forged Unique Relic: item={}, template={}, pattern={}, material={}, smith={}",
                Registries.ITEM.getId(stack.getItem()),
                templateId,
                patternId,
                materialId,
                player.getName().getString()
        );

        this.uniqueRelics$clearCachedRoll();
    }

    @Unique
    private String uniqueRelics$stackSignature(ItemStack stack) {
        String nbt = stack.hasNbt() ? stack.getNbt().toString() : "";
        return Registries.ITEM.getId(stack.getItem()) + "x" + stack.getCount() + "#" + nbt;
    }

    @Unique
    private void uniqueRelics$clearCachedRoll() {
        this.uniqueRelics$cachedInputSignature = "";
        this.uniqueRelics$cachedMaterialId = null;
    }
}
