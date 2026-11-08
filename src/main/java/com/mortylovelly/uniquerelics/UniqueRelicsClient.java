package com.mortylovelly.uniquerelics;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Item;
import net.minecraft.item.trim.ArmorTrimMaterial;
import net.minecraft.item.trim.ArmorTrimPattern;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.Registries;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Formatting;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class UniqueRelicsClient implements ClientModInitializer {
    private static final DateTimeFormatter FORGE_DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm").withZone(ZoneId.systemDefault());

    private static final Identifier ICON_FONT = new Identifier(UniqueRelics.MOD_ID, "icons");

    /*
     * These glyphs use the actual 16x16 item textures from Minecraft and Tool Trims.
     * Unknown third-party templates/materials simply fall back to text without an icon.
     */
    private static final Map<String, String> ITEM_ICONS = Map.ofEntries(
            Map.entry("minecraft:sentry_armor_trim_smithing_template", "\uE000"),
            Map.entry("minecraft:dune_armor_trim_smithing_template", "\uE001"),
            Map.entry("minecraft:coast_armor_trim_smithing_template", "\uE002"),
            Map.entry("minecraft:wild_armor_trim_smithing_template", "\uE003"),
            Map.entry("minecraft:ward_armor_trim_smithing_template", "\uE004"),
            Map.entry("minecraft:eye_armor_trim_smithing_template", "\uE005"),
            Map.entry("minecraft:vex_armor_trim_smithing_template", "\uE006"),
            Map.entry("minecraft:tide_armor_trim_smithing_template", "\uE007"),
            Map.entry("minecraft:snout_armor_trim_smithing_template", "\uE008"),
            Map.entry("minecraft:rib_armor_trim_smithing_template", "\uE009"),
            Map.entry("minecraft:spire_armor_trim_smithing_template", "\uE00A"),
            Map.entry("minecraft:wayfinder_armor_trim_smithing_template", "\uE00B"),
            Map.entry("minecraft:shaper_armor_trim_smithing_template", "\uE00C"),
            Map.entry("minecraft:raiser_armor_trim_smithing_template", "\uE00D"),
            Map.entry("minecraft:host_armor_trim_smithing_template", "\uE00E"),
            Map.entry("minecraft:silence_armor_trim_smithing_template", "\uE00F"),
            Map.entry("tooltrims:linear_tool_trim_smithing_template", "\uE010"),
            Map.entry("tooltrims:tracks_tool_trim_smithing_template", "\uE011"),
            Map.entry("tooltrims:charge_tool_trim_smithing_template", "\uE012"),
            Map.entry("tooltrims:frost_tool_trim_smithing_template", "\uE013"),
            Map.entry("minecraft:quartz", "\uE020"),
            Map.entry("minecraft:iron_ingot", "\uE021"),
            Map.entry("minecraft:netherite_ingot", "\uE022"),
            Map.entry("minecraft:redstone", "\uE023"),
            Map.entry("minecraft:copper_ingot", "\uE024"),
            Map.entry("minecraft:gold_ingot", "\uE025"),
            Map.entry("minecraft:emerald", "\uE026"),
            Map.entry("minecraft:diamond", "\uE027"),
            Map.entry("minecraft:lapis_lazuli", "\uE028"),
            Map.entry("minecraft:amethyst_shard", "\uE029")
    );

    @Override
    public void onInitializeClient() {
        ItemTooltipCallback.EVENT.register((stack, context, lines) -> {
            if (!stack.hasNbt()) {
                return;
            }

            NbtCompound root = stack.getNbt();
            if (root == null || !root.contains("unique_relics")) {
                return;
            }

            NbtCompound relicData = root.getCompound("unique_relics");
            if (relicData.isEmpty()) {
                return;
            }

            MinecraftClient client = MinecraftClient.getInstance();

            String patternId = relicData.getString("pattern");
            Identifier parsedPatternId = Identifier.tryParse(patternId);
            Text patternName = Text.literal(parsedPatternId == null
                    ? patternId
                    : humanizeIdentifier(parsedPatternId.getPath()));
            Item templateItem = null;

            if (parsedPatternId != null && client.world != null) {
                Registry<ArmorTrimPattern> patterns =
                        client.world.getRegistryManager().get(RegistryKeys.TRIM_PATTERN);
                ArmorTrimPattern pattern = patterns.get(parsedPatternId);
                if (pattern != null) {
                    patternName = Text.literal(cleanPatternName(pattern.description().getString()));
                    templateItem = pattern.templateItem().value();
                }
            }

            if (templateItem == null) {
                Identifier storedTemplateId = Identifier.tryParse(relicData.getString("template"));
                if (storedTemplateId != null && Registries.ITEM.containsId(storedTemplateId)) {
                    templateItem = Registries.ITEM.get(storedTemplateId);
                }
            }

            Text templateValue = withItemIcon(templateItem, patternName);
            Text colorName = Text.literal(humanizeIdentifier(relicData.getString("material")));
            Item materialItem = null;
            Identifier parsedMaterialId = Identifier.tryParse(relicData.getString("material"));

            if (parsedMaterialId != null && client.world != null) {
                Registry<ArmorTrimMaterial> materials =
                        client.world.getRegistryManager().get(RegistryKeys.TRIM_MATERIAL);
                ArmorTrimMaterial material = materials.get(parsedMaterialId);
                if (material != null) {
                    colorName = material.description();
                    materialItem = material.ingredient().value();
                }
            }

            Text colorValue = withItemIcon(materialItem, colorName);

            List<Text> relicLines = new ArrayList<>();
            relicLines.add(Text.translatable("tooltip.unique_relics.header")
                    .formatted(Formatting.GOLD, Formatting.BOLD));
            relicLines.add(Text.translatable("tooltip.unique_relics.template", templateValue)
                    .formatted(Formatting.GRAY));
            relicLines.add(Text.translatable("tooltip.unique_relics.color", colorValue)
                    .formatted(Formatting.GRAY));
            relicLines.add(Text.translatable(
                    "tooltip.unique_relics.rarity",
                    Text.translatable("tooltip.unique_relics.rarity.rare").formatted(Formatting.DARK_PURPLE)
            ).formatted(Formatting.GRAY));

            if (relicData.contains("smith")) {
                relicLines.add(Text.translatable(
                        "tooltip.unique_relics.smith",
                        relicData.getString("smith")
                ).formatted(Formatting.GRAY));
            }

            long forgedAt = relicData.getLong("forged_at");
            if (forgedAt > 0L) {
                String formattedDate = FORGE_DATE_FORMAT.format(Instant.ofEpochMilli(forgedAt));
                relicLines.add(Text.translatable("tooltip.unique_relics.date", formattedDate)
                        .formatted(Formatting.GRAY));
            }

            // Insert immediately after the item's name, before vanilla lore and attributes.
            lines.addAll(Math.min(1, lines.size()), relicLines);
        });
    }

    private static Text withItemIcon(Item item, Text value) {
        if (item == null) {
            return value;
        }

        Identifier itemId = Registries.ITEM.getId(item);
        String glyph = ITEM_ICONS.get(itemId.toString());
        if (glyph == null) {
            return value;
        }

        return Text.empty()
                .append(Text.literal(glyph).setStyle(Style.EMPTY.withFont(ICON_FONT)))
                .append(Text.literal(" "))
                .append(value);
    }

    private static String cleanPatternName(String rawName) {
        String value = rawName == null ? "" : rawName.trim();

        String[] prefixes = {"Отделка", "Armor Trim", "Tool Trim", "Trim"};
        for (String prefix : prefixes) {
            if (value.regionMatches(true, 0, prefix, 0, prefix.length())) {
                value = value.substring(prefix.length()).trim();
                break;
            }
        }

        String[] suffixes = {" Armor Trim", " Tool Trim", " Trim"};
        for (String suffix : suffixes) {
            if (value.toLowerCase(Locale.ROOT).endsWith(suffix.toLowerCase(Locale.ROOT))) {
                value = value.substring(0, value.length() - suffix.length()).trim();
                break;
            }
        }

        if (value.length() >= 2) {
            char first = value.charAt(0);
            char last = value.charAt(value.length() - 1);
            if ((first == '«' && last == '»') || (first == '"' && last == '"')
                    || (first == '“' && last == '”')) {
                value = value.substring(1, value.length() - 1).trim();
            }
        }

        return value.isEmpty() ? rawName : value;
    }

    private static String humanizeIdentifier(String identifier) {
        if (identifier == null || identifier.isBlank()) {
            return "—";
        }

        String value = identifier;
        int namespaceSeparator = value.indexOf(':');
        if (namespaceSeparator >= 0) {
            value = value.substring(namespaceSeparator + 1);
        }

        value = value.replace('_', ' ');
        if (value.isEmpty()) {
            return "—";
        }

        return value.substring(0, 1).toUpperCase(Locale.ROOT) + value.substring(1);
    }
}
