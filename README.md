# Unique Relics

**Unique Relics** is a Minecraft Java Edition 1.20.1 Fabric mod for turning existing items into unique, collectible relics without creating replacement item types.

## Current implementation

The first gameplay feature uses the existing smithing table and the game's trim recipes:

- Keep the original item ID (a sword remains that sword, armor remains armor).
- Use the smithing-table template/base/material flow from a valid vanilla or compatible modded smithing trim recipe.
- When a valid recipe produces an item with a real armor trim, Unique Relics keeps the selected pattern and randomizes the trim material from the active trim-material registry.
- The item's NBT receives a `unique_relics` compound with the template, pattern, randomized material and rarity.
- When the player takes the result, the mod records the player's name and forge date.
- The relic tooltip is displayed directly below the item name, with the real template/material item icons and localized pattern name.
- The duplicate vanilla armor-trim tooltip is hidden without changing the item's visible trim.
- The initial rarity is always **Rare**.
- Forging is currently guaranteed whenever the underlying smithing trim recipe succeeds. A probability/failure mechanic is not defined yet.

Only the trim metadata and trim material are changed. The output remains the same underlying item and its existing enchantments, name, durability and other NBT are preserved by the vanilla smithing recipe.

## Tool trim compatibility

Unique Relics does not bundle or compile against Tool Trims. It uses Minecraft's shared `ArmorTrim` API, which Tool Trims extends for supported tools.

For Tool Trims **1.1.0+mc1.20.1**, use its dedicated templates:

- Linear
- Tracks
- Charge
- Frost

That release provides tool-trim recipes for its own four templates and a defined tag of supported tools/materials. Vanilla armor-trim templates such as Coast or Silence do not automatically become tool recipes in that release. Unique Relics writes a diagnostic to the log when this specific mismatch is detected.

The exact Tool Trims supported item/material lists are controlled by Tool Trims, not by Unique Relics.

## Important limits of the first version

- The input must produce a real trim result through vanilla or a compatible modded smithing recipe. The addition slot is limited by the active recipe's accepted ingredients.
- Vanilla armor trims work without extra trim mods.
- Tool trims require a compatible mod such as Tool Trims.
- Elytra support needs Elytra Trims installed on both server and client for server-side application and visible rendering.
- This mod does not bundle third-party mod JARs or depend on them at compile time.
- Compatibility with specific Elytra/tool trim mod versions still needs an in-game test.

## Environment

| Component | Version |
| --- | --- |
| Minecraft | 1.20.1 |
| Fabric Loader | 0.19.5 |
| Fabric API | 0.92.12+1.20.1 |
| Java | 17 |
| Gradle Wrapper | 8.12 |
| Fabric Loom | 1.9.2 |

## Build

**Windows**
```bat
gradlew.bat build --no-daemon
```

**Linux / macOS**
```bash
./gradlew build --no-daemon
```

The JAR is created in `build/libs/`.

## Identifiers

- Mod ID: `unique_relics`
- Java package: `com.mortylovelly.uniquerelics`
- Maven group: `com.mortylovelly.uniquerelics`
