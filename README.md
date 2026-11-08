# Unique Relics

**Unique Relics** is a Minecraft Java Edition 1.20.1 Fabric mod for turning existing items into unique, collectible relics without creating replacement item types.

## Current implementation

The first gameplay feature uses the existing smithing table and the game's trim recipes:

- Keep the original item ID (a sword remains that sword, armor remains that armor).
- Use the normal smithing-table template/base/material flow.
- When a valid smithing recipe produces an item with a real armor trim, Unique Relics keeps the selected pattern and randomizes the trim material from the active trim-material registry.
- The item's NBT receives a `unique_relics` compound with the template, pattern, randomized material and rarity.
- When the player takes the result, the mod records the player's name and forge date.
- The tooltip shows the template, color/material, rarity, smith and date.
- The initial rarity is always **Rare**.
- Forging is currently guaranteed whenever the underlying smithing trim recipe succeeds. A probability/failure mechanic is not defined yet.

Only the trim metadata and trim material are changed. The output remains the same underlying item and its existing enchantments, name, durability and other NBT are preserved by the vanilla smithing recipe.

## Important limits of the first version

- The input must produce a real trim result through vanilla or a compatible modded smithing recipe. The addition slot is therefore limited by the active recipe's accepted ingredients; it is not literally any arbitrary item yet.
- Vanilla armor trims work without extra trim mods.
- Vanilla swords/tools need a compatible mod such as a Fabric tool-trim implementation.
- Elytra support needs Elytra Trims installed on both server and client for server-side application and visible rendering.
- This mod does not bundle third-party mod JARs or depend on them at compile time. It modifies the trim result through Minecraft's shared `ArmorTrim` API.
- Compatibility with specific Elytra/tool trim mod versions must still be tested in-game.

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
