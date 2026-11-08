package com.mortylovelly.uniquerelics.mixin;

import com.mortylovelly.uniquerelics.UniqueRelics;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.trim.ArmorTrim;
import net.minecraft.item.trim.ArmorTrimMaterial;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.screen.SmithingScreenHandler;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
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
 * Applies Unique Relics metadata to the actual smithing handler implementation.
 * Third-party tool-trim support is detected through Tool Trims' public item tags;
 * the mod is not a compile-time dependency.
 */
@Mixin(SmithingScreenHandler.class)
public abstract class SmithingScreenHandlerMixin {
    @Shadow
    @Final
    private World world;

    @Unique
    private static final TagKey<Item> uniqueRelics$TOOLTRIMS_TOOLS =
            TagKey.of(RegistryKeys.ITEM, new Identifier("tooltrims", "trimmable_tools"));

    @Unique
    private static final TagKey<Item> uniqueRelics$TOOLTRIMS_MATERIALS =
            TagKey.of(RegistryKeys.ITEM, new Identifier("tooltrims", "tool_trim_materials"));

    @Unique
    private String uniqueRelics$cachedInputSignature = "";

    @Unique
    private Identifier uniqueRelics$cachedMaterialId;

    @Unique
    private String uniqueRelics$lastToolDiagnosticSignature = "";

    @Inject(method = "updateResult", at = @At("TAIL"))
    private void uniqueRelics$applyRelicPreview(CallbackInfo ci) {
        if (this.world == null || this.world.isClient) {
            return;
        }

        ForgingScreenHandlerAccessor accessor = (ForgingScreenHandlerAccessor) (Object) this;
        ItemStack template = accessor.uniqueRelics$getInput().getStack(0);
        ItemStack base = accessor.uniqueRelics$getInput().getStack(1);
        ItemStack addition = accessor.uniqueRelics$getInput().getStack(2);
        ItemStack result = accessor.uniqueRelics$getOutput().getStack(0);

        if (template.isEmpty() || base.isEmpty() || addition.isEmpty()) {
            this.uniqueRelics$clearCachedRoll();
            return;
        }

        if (result.isEmpty()) {
            this.uniqueRelics$logToolTrimDiagnostic(template, base, addition, "no_recipe_result");
            this.uniqueRelics$clearCachedRoll();
            return;
        }

        Optional<ArmorTrim> currentTrim = ArmorTrim.getTrim(this.world.getRegistryManager(), result);
        if (currentTrim.isEmpty()) {
            this.uniqueRelics$logToolTrimDiagnostic(template, base, addition, "result_has_no_trim");
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

        if (!ArmorTrim.apply(this.world.getRegistryManager(), result, randomizedTrim)) {
            UniqueRelics.LOGGER.debug(
                    "The smithing result {} rejected a randomized ArmorTrim; leaving the recipe result intact.",
                    Registries.ITEM.getId(result.getItem())
            );
            this.uniqueRelics$logToolTrimDiagnostic(template, base, addition, "trim_apply_rejected");
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
        result.getOrCreateNbt().put("unique_relics", relicData);
        this.uniqueRelics$hideVanillaTrimTooltip(result);
        this.uniqueRelics$lastToolDiagnosticSignature = "";
    }

    @Inject(method = "onTakeOutput", at = @At("HEAD"))
    private void uniqueRelics$recordForge(PlayerEntity player, ItemStack stack, CallbackInfo ci) {
        if (player.getWorld().isClient || stack.isEmpty()) {
            return;
        }

        ForgingScreenHandlerAccessor accessor = (ForgingScreenHandlerAccessor) (Object) this;
        ItemStack template = accessor.uniqueRelics$getInput().getStack(0);
        Optional<ArmorTrim> appliedTrim =
                ArmorTrim.getTrim(player.getWorld().getRegistryManager(), stack);

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
        this.uniqueRelics$hideVanillaTrimTooltip(stack);

        UniqueRelics.LOGGER.info(
                "Forged Unique Relic: item={}, template={}, pattern={}, material={}, smith={}",
                Registries.ITEM.getId(stack.getItem()),
                templateId,
                patternId,
                materialId,
                player.getName().getString()
        );

        this.uniqueRelics$clearCachedRoll();
        this.uniqueRelics$lastToolDiagnosticSignature = "";
    }

    @Unique
    private void uniqueRelics$logToolTrimDiagnostic(
            ItemStack template,
            ItemStack base,
            ItemStack addition,
            String reason
    ) {
        if (!FabricLoader.getInstance().isModLoaded("tooltrims")) {
            return;
        }

        Identifier templateId = Registries.ITEM.getId(template.getItem());
        Identifier baseId = Registries.ITEM.getId(base.getItem());
        Identifier additionId = Registries.ITEM.getId(addition.getItem());

        boolean toolTemplate = "tooltrims".equals(templateId.getNamespace());
        boolean vanillaArmorTemplate = "minecraft".equals(templateId.getNamespace())
                && templateId.getPath().endsWith("_armor_trim_smithing_template");
        boolean baseIsSupportedTool = base.isIn(uniqueRelics$TOOLTRIMS_TOOLS);

        if (!toolTemplate && !(vanillaArmorTemplate && baseIsSupportedTool)) {
            return;
        }

        String signature = templateId + "|" + baseId + "|" + additionId + "|" + reason;
        if (signature.equals(this.uniqueRelics$lastToolDiagnosticSignature)) {
            return;
        }
        this.uniqueRelics$lastToolDiagnosticSignature = signature;

        boolean additionIsSupportedMaterial = addition.isIn(uniqueRelics$TOOLTRIMS_MATERIALS);
        if (vanillaArmorTemplate && baseIsSupportedTool) {
            UniqueRelics.LOGGER.warn(
                    "Tool Trims check: {} has no tool-trim recipe for vanilla template {}. " +
                            "Use one of Tool Trims' dedicated templates (linear, tracks, charge or frost). " +
                            "base={}, addition={}.",
                    reason, templateId, baseId, additionId
            );
            return;
        }

        UniqueRelics.LOGGER.warn(
                "Tool Trims check: reason={}, template={}, base={}, base_in_tooltrims_tag={}, " +
                        "addition={}, addition_in_tooltrims_materials_tag={}. " +
                        "Tool Trims 1.1.0 only supplies recipes for its four dedicated templates.",
                reason, templateId, baseId, baseIsSupportedTool,
                additionId, additionIsSupportedMaterial
        );
    }

    @Unique
    private void uniqueRelics$hideVanillaTrimTooltip(ItemStack stack) {
        NbtCompound root = stack.getOrCreateNbt();
        root.putInt("HideFlags", root.getInt("HideFlags") | 128);
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
