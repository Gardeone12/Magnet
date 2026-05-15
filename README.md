# Magnet

Language: English | [Russian](docs/README.ru.md)

Magnit is a Paper plugin for Minecraft that adds a magnet item. When a player holds the magnet in the main hand or off-hand, nearby dropped metal items are pulled toward the player.

## Features

- `/magnet` gives a player the magnet item.
- The magnet works from the main hand and off-hand.
- Iron-based items are attracted at full force.
- Netherite-based items are attracted with reduced force.
- The magnet item is stored with `PersistentDataContainer`, so the plugin can distinguish it from a regular amethyst shard.
- Built-in plugin messages support English as the primary language and Russian as a secondary localization.

## Requirements

- Minecraft / Paper: `1.21.11`
- Java: `21`
- Kotlin JVM: `2.4.0-RC`

## Commands

| Command | Description |
| --- | --- |
| `/magnet` | Gives the player a magnet item |

The command has no permission requirement configured.

## Localization

English is the default language. Russian is available as an additional localization.

The plugin selects the message language from the player's client locale. Unsupported locales fall back to English.

## How It Works

Every 2 ticks, the plugin checks online players. If a player is holding a magnet, the plugin searches for dropped items within a 7-block radius and applies velocity toward the player to supported materials.

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
build/libs/
```

## Installation

1. Build the plugin.
2. Copy the generated `.jar` from `build/libs/` to the Paper server `plugins` folder.
3. Start or restart the server.
4. Run `/magnet` in game.

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
src/main/kotlin/ru/garde/magnit/
  MagnetPlugin.kt
  MagnetPluginBootsTrap.kt

src/main/resources/
  plugin.yml
  lang/
    en.yml
    ru.yml
```

## Usage Notice

This is an open-source project with project-specific limitations. Check the repository license and distribution terms before redistributing modified builds.
