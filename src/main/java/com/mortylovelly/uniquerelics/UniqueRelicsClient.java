package com.mortylovelly.uniquerelics;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.trim.ArmorTrimMaterial;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Formatting;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public final class UniqueRelicsClient implements ClientModInitializer {
    private static final DateTimeFormatter FORGE_DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm").withZone(ZoneId.systemDefault());

    @Override
    public void onInitializeClient() {
        ItemTooltipCallback.EVENT.register((stack, context, type, lines) -> {
            if (!stack.hasNbt()) {
                return;
            }

            NbtCompound root = stack.getNbt();
            if (root == null || !root.contains("unique_relics", NbtCompound.class)) {
                return;
            }

            NbtCompound relicData = root.getCompound("unique_relics");
            if (relicData.isEmpty()) {
                return;
            }

            lines.add(Text.empty());
            lines.add(Text.translatable("tooltip.unique_relics.header")
                    .formatted(Formatting.GOLD, Formatting.BOLD));

            String templateId = relicData.getString("template");
            Identifier parsedTemplateId = Identifier.tryParse(templateId);
            Text templateName = Text.literal(templateId);

            if (parsedTemplateId != null && Registries.ITEM.containsId(parsedTemplateId)) {
                templateName = Registries.ITEM.get(parsedTemplateId).getName();
            }

            lines.add(Text.translatable("tooltip.unique_relics.template", templateName)
                    .formatted(Formatting.GRAY));

            String materialId = relicData.getString("material");
            Text materialName = Text.literal(materialId);
            Identifier parsedMaterialId = Identifier.tryParse(materialId);
            MinecraftClient client = MinecraftClient.getInstance();

            if (parsedMaterialId != null && client.world != null) {
                Registry<ArmorTrimMaterial> materials =
                        client.world.getRegistryManager().get(RegistryKeys.TRIM_MATERIAL);
                ArmorTrimMaterial material = materials.get(parsedMaterialId);
                if (material != null) {
                    materialName = material.description();
                }
            }

            lines.add(Text.translatable("tooltip.unique_relics.color", materialName)
                    .formatted(Formatting.GRAY));

            lines.add(Text.translatable(
                    "tooltip.unique_relics.rarity",
                    Text.translatable("tooltip.unique_relics.rarity.rare").formatted(Formatting.DARK_PURPLE)
            ).formatted(Formatting.GRAY));

            if (relicData.contains("smith")) {
                lines.add(Text.translatable(
                        "tooltip.unique_relics.smith",
                        relicData.getString("smith")
                ).formatted(Formatting.GRAY));
            }

            long forgedAt = relicData.getLong("forged_at");
            if (forgedAt > 0L) {
                String formattedDate = FORGE_DATE_FORMAT.format(Instant.ofEpochMilli(forgedAt));
                lines.add(Text.translatable("tooltip.unique_relics.date", formattedDate)
                        .formatted(Formatting.GRAY));
            }
        });
    }
}
