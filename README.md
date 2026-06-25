# Magnet

Language: English | [Russian](docs/README.ru.md)

Magnet is a Paper/Spigot plugin for Minecraft that adds portable magnets and stationary magnetic cores. Portable magnets pull nearby dropped metal items while held by a player. Stationary cores are 2x2x2 block structures that pull dropped metal items toward their center.

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

- Supported Minecraft range: `1.16.5 - 1.21.x`
- Plugin bytecode target: Java 8
- Build JDK: Java 21
- Compile API: `org.spigotmc:spigot-api:1.16.5-R0.1-SNAPSHOT`
- Gradle Wrapper: 9.4.1

Use the Java version required by the server itself. In practice, modern Paper 1.20.5+ requires Java 21, Paper 1.18-1.20.4 generally uses Java 17, and older servers need their matching legacy Java runtime.

## Compatibility

Magnet is distributed as one universal jar:

`build/libs/Magnet-Universal-1.16.5-1.21.x.jar`

The common code compiles against Spigot API 1.16.5. New item-model APIs are detected through reflection, messages use legacy Bukkit strings, materials are resolved by name with safe fallbacks, and commands are declared in `plugin.yml`.

Runtime verification completed in this workspace:

- Paper 1.16.5 build 794 on June 25, 2026
- Paper 1.21.11 build 132 on June 25, 2026
- `/magnet`, `/magnet reload`, console-safe player-only handling, and `/magnet core list`
- clean plugin shutdown on both tested versions

The code is intended for Paper/Spigot 1.16.5 through 1.21.x. Intermediate versions, Spigot, and Leaf should still be tested on the target production server before deployment.

Compatibility behavior:

- Portable magnets are identified through `PersistentDataContainer`, available in 1.16.5.
- Minecraft 1.17+ uses `AMETHYST_SHARD`; 1.16.5 falls back to `COMPASS`.
- Modern servers use the item model/custom model data component APIs when present.
- Older servers use integer custom model data `9001001`.
- Materials unavailable in the running Minecraft version are skipped safely.
- Versions below 1.16.5 are rejected with a clear startup error when the server reaches the plugin entry point.

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

All commands use `magnet.use`. It defaults to `true`, preserving the previous unrestricted behavior; server owners can revoke it through their permissions plugin.

## Portable Magnet

The portable magnet is marked with `PersistentDataContainer`, so the plugin can distinguish it from a regular item. It uses `AMETHYST_SHARD` on Minecraft 1.17+ and falls back to `COMPASS` on 1.16.5. Modern item-model APIs are used when available; otherwise the plugin writes custom model data `9001001`.

Every 2 ticks, the plugin checks online players. If a player is holding a portable magnet, nearby supported dropped items within 7 blocks are pulled toward the player.

## Resource Pack Setup

The bundled example pack in `docs/resource-pack` uses the same namespace as the plugin: `magnit`. A pack that uses `assets/magnet/...` will not match `magnit:portable_magnet` and can render as a missing texture.

For Minecraft `1.21.x`, a pack that supports both the modern item model path and the custom model data fallback should include these files:

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

Put the texture at `assets/magnit/textures/item/portable_magnet.png`. Newly issued magnets get this model automatically; older magnets are updated when a player holds them. For 1.16.5, add an equivalent custom-model-data override for `minecraft:compass`, because amethyst shards do not exist in that version.

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

Use `/magnet debug item` while holding the magnet to check the base material, `PersistentDataContainer` marker, item model key, custom model data, expected model key, best available visual path, and plugin version.

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

Build every artifact with the wrapper:

```powershell
.\gradlew.bat clean build
```

The build produces one shaded universal jar:

```text
build/libs/Magnet-Universal-1.16.5-1.21.x.jar
```

The jar includes the Kotlin runtime and contains Java 8 bytecode (class-file major version 52).

## Installation

1. Run `.\gradlew.bat clean build`.
2. Copy `build/libs/Magnet-Universal-1.16.5-1.21.x.jar` to the server's `plugins` folder.
3. Start or restart the server.
4. Run `/magnet` and `/magnet give`.
5. Build a 2x2x2 core and use `/magnet core create <id>` if needed.

## Local Test Server

The test servers use isolated directories under `run/`:

```powershell
.\gradlew.bat runLegacyServer
.\gradlew.bat runModernServer
```

A specific Paper version can also be selected:

```powershell
.\gradlew.bat runServer '-PrunMinecraftVersion=1.16.5'
.\gradlew.bat runServer '-PrunMinecraftVersion=1.20.4'
.\gradlew.bat runServer '-PrunMinecraftVersion=1.21.11'
```

The legacy task sets Paper's unsupported-JVM override for local Java 21 smoke tests. For production, use the Java runtime recommended by that Minecraft/Paper version.

## Project Structure

```text
src/main/kotlin/ru/garde/magnet/
  MagnetPlugin.kt
  command/
  compat/
  config/
  core/
  message/
  portable/
  resourcepack/

src/main/resources/
  plugin.yml
  config.yml
  lang/
    en.yml
    ru.yml

schematic/
  Magnet.litematic
```

See [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) for the contributor-oriented package guide.

## Known Limitations

- Runtime smoke tests were completed on Paper 1.16.5 and Paper 1.21.11; the entire intermediate matrix, Spigot, and Leaf were not launched in this workspace.
- Minecraft 1.16.5 has no amethyst, copper, deepslate, or other later materials. Those configured entries are ignored, and available profiles such as iron, gold, redstone, obsidian, netherite, lodestone, beacon, and respawn anchor remain usable.
- The 1.16.5 resource-pack fallback uses a compass model; modern packs can keep the amethyst-shard/item-model path.
- In-game attraction and item issuance still require a player smoke test; console tests only verify command routing and safe error handling.

## License

This project is licensed under the GNU General Public License v3.0 only.

Copyright (C) 2026 Garde1 / Gardeone12.

See the [LICENSE](LICENSE) file for details.

