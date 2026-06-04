# Magnet

Language: English | [Russian](docs/README.ru.md)

Magnet is a Paper plugin for Minecraft that adds portable magnets and stationary magnetic cores. Portable magnets pull nearby dropped metal items while held by a player. Stationary cores are 2x2x2 block structures that pull dropped metal items toward their center.

## Features

- Portable magnet item issued with `/magnet give`.
- Portable magnets work from the main hand and off hand.
- Stationary 2x2x2 magnetic cores with configurable radius and strength.
- Core radius and strength are calculated from the eight configured core material blocks.
- Core material profiles are configurable in `config.yml` and can be changed in game.
- Magnetic frames are scanned around cores and saved with critical and optional blocks.
- Breaking a core block or critical frame block disables the core until it is repaired or rescanned.
- Core diagnostics show loaded world/chunk state, frame state, core blocks, and nearby magnetic items.
- Magnetic cores are saved to `cores.yml` in the plugin data folder.
- English and Russian plugin messages are included.
- `schematic/Magnet.litematic` contains an example magnetic structure.

## Requirements

- Minecraft / Paper: `1.21.11`
- Java: `21`
- Kotlin JVM: `2.4.0-RC`

## Commands

| Command | Description |
| --- | --- |
| `/magnet` | Shows help |
| `/magnet give` | Gives the player a portable magnet |
| `/magnet reload` | Reloads plugin config and magnetic cores |
| `/magnet debug item` | Shows Portable Magnet model diagnostics for the item in your main hand |
| `/magnet core create <id> [radius] [strength]` | Creates a core from the 2x2x2 core material structure the player is looking at |
| `/magnet core createat <id> <x> <y> <z> [radius] [strength]` | Creates a core at exact coordinates in the player's current world |
| `/magnet core createat <id> <world> <x> <y> <z> [radius] [strength]` | Creates a core at exact coordinates from console or another world |
| `/magnet core remove <id>` | Removes a saved core |
| `/magnet core list` | Lists saved cores and their status |
| `/magnet core info <id>` | Shows diagnostics for a core |
| `/magnet core rescan <id>` | Rescans the frame around a core |
| `/magnet core refresh <id>` | Recalculates a core after replacing core materials |
| `/magnet core override <id> <true\|false>` | Toggles manual radius and strength override |
| `/magnet core set <id> <radius\|strength> <value>` | Sets one core's radius or strength and enables override |
| `/magnet core reload` | Reloads cores and settings from config |
| `/magnet profile list` | Lists configured core material profiles |
| `/magnet profile info <material>` | Shows one material profile |
| `/magnet profile set <material> <radius\|strength\|priority> <value>` | Updates a material profile and recalculates matching cores |
| `/magnet profile reload` | Reloads material profiles from config |

The command has no permission requirement configured.

## Portable Magnet

The portable magnet is an amethyst shard marked with `PersistentDataContainer`, so the plugin can distinguish it from a regular item. On Paper/Leaf `1.21.11`, the primary visual hook is the item model key `magnit:portable_magnet`. The plugin also writes custom model data `9001001` as a fallback for packs that still include an amethyst shard override.

Every 2 ticks, the plugin checks online players. If a player is holding a portable magnet, nearby supported dropped items within 7 blocks are pulled toward the player.

## Resource Pack Setup

The bundled example pack in `docs/resource-pack` uses the same namespace as the plugin: `magnit`. A pack that uses `assets/magnet/...` will not match `magnit:portable_magnet` and can render as a missing texture.

For Minecraft `1.21.11`, the pack should include these files:

```text
pack.mcmeta
assets/magnit/items/portable_magnet.json
assets/magnit/models/item/portable_magnet.json
assets/magnit/textures/item/portable_magnet.png
assets/minecraft/models/item/amethyst_shard.json
```

`assets/magnit/items/portable_magnet.json`:

```json
{
  "model": {
    "type": "minecraft:model",
    "model": "magnit:item/portable_magnet"
  }
}
```

`assets/magnit/models/item/portable_magnet.json`:

```json
{
  "parent": "minecraft:item/generated",
  "textures": {
    "layer0": "magnit:item/portable_magnet"
  }
}
```

Put the texture at `assets/magnit/textures/item/portable_magnet.png`. Newly issued magnets get this model automatically; older magnets are updated when a player holds them.

In `server.properties`, `resource-pack` must be a direct download URL to the final `.zip`. `resource-pack-prompt` must be a JSON text component. Plain text such as `Для отображения портативного магнита нужен ресурспак Magnet.` causes `MalformedJsonException` on Leaf/Paper.

Russian prompt example:

```properties
resource-pack=PASTE_DIRECT_DOWNLOAD_LINK_HERE
resource-pack-id=7bb7e1e4-c4e6-42b4-9c8e-8f1e9c8a6f02
resource-pack-prompt={"text":"Для отображения портативного магнита нужен ресурспак Magnet.","color":"aqua"}
resource-pack-sha1=f19f038ec8b744579cc692e5b7a9e41d6df0e8fb
require-resource-pack=false
```

English prompt variant:

```properties
resource-pack-prompt={"text":"This server uses a resource pack to display the Portable Magnet texture.","color":"aqua"}
```

If you change the pack zip, update `resource-pack-sha1` to the SHA-1 of that exact zip.

Use `/magnet debug item` while holding the magnet to check the base material, `PersistentDataContainer` marker, item model key, custom model data, expected model key, and plugin version.

Attracted at full force:

- iron ingots, nuggets, blocks, raw iron, and iron ores;
- iron tools, weapons, and armor;
- chainmail armor;
- anvils, iron bars, iron doors, and iron trapdoors;
- rails and minecarts;
- buckets, shears, compasses, and similar metal items.

Attracted with reduced force:

- netherite ingots, scraps, and blocks;
- netherite tools, weapons, and armor.

## Magnetic Cores

A stationary magnetic core is created from a complete 2x2x2 cube of configured core materials. Look at one of the eight blocks and run:

```text
/magnet core create <id>
```

The plugin calculates the core center, scans the surrounding frame, saves the core, and starts attracting supported dropped items toward the center.

Core strength comes from material profiles:

- radius and strength are averaged from the eight core blocks;
- the displayed profile is chosen by material profile count, then by priority;
- optional radius and strength arguments create a manual override;
- `/magnet core set` also enables manual override;
- disabling override recalculates values from the current core blocks.

Core IDs may contain only lowercase letters, digits, underscores, and hyphens.

## Core Materials And Frames

`config.yml` contains two important sections:

- `core-materials` defines which block materials can form a 2x2x2 core and what profile, base radius, base strength, and priority each material has.
- `frame-materials` defines which blocks count as the surrounding magnetic frame.

Default core materials include copper variants, iron, gold, redstone, lapis, diamond, emerald, amethyst, prismarine, obsidian, netherite, lodestone, beacon, and respawn anchor blocks.

The frame scanner searches around the core using the configured scan radii. Frame corner blocks can be treated as optional with `ignore-frame-corners`. Breaking a core block or a critical frame block marks the core damaged and disables attraction until the structure is repaired and refreshed or rescanned.

## Localization

English is the default language. Russian is available as an additional localization.

The plugin selects the message language from the player's client locale. Unsupported locales fall back to English.

## Build

If Gradle is installed globally:

```powershell
gradle build
```

If Gradle Wrapper files are available:

```powershell
.\gradlew.bat build
```

The compiled plugin jar is generated in:

```text
build/libs/Magnit-0.1.2.jar
```

## Installation

1. Build the plugin.
2. Copy `build/libs/Magnit-0.1.2.jar` to the Paper server `plugins` folder.
3. Start or restart the server.
4. Run `/magnet give` in game.
5. Build a 2x2x2 core structure and use `/magnet core create <id>` if you want a stationary magnetic core.

## Local Test Server

The project uses `xyz.jpenilla.run-paper`, so a local Paper test server can be started with:

```powershell
gradle runServer
```

If Gradle Wrapper files are available:

```powershell
.\gradlew.bat runServer
```

## Project Structure

```text
src/main/kotlin/ru/garde/magnet/
  CoreMaterialRegistry.kt
  MagnetCoreManager.kt
  MagnetPlugin.kt
  MagnetPluginBootsTrap.kt
  MagnetStructureBreakListener.kt

src/main/resources/
  paper-plugin.yml
  config.yml
  lang/
    en.yml
    ru.yml

schematic/
  Magnet.litematic
```

## License

This project is licensed under the GNU General Public License v3.0 only.

Copyright (C) 2026 Garde1 / Gardeone12.

See the [LICENSE](LICENSE) file for details.

