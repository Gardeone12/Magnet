# Magnet Architecture

The project produces one universal plugin jar for Minecraft 1.16.5 through 1.21.x without duplicating gameplay code.

## Main Flow

1. `MagnetPlugin` validates the server version and wires services.
2. `MagnetConfig` centralizes typed config reads.
3. `MessageService` loads English and Russian YAML and emits legacy Bukkit text that works on 1.16.5+.
4. `MagnetItemFactory` creates and identifies PDC-backed portable magnets.
5. `PortableMagnetService` handles held-magnet attraction.
6. `MagnetCoreManager` handles stationary 2x2x2 cores, persistence, frame scans, and physics.
7. `ResourcePackService` validates resource-pack settings.
8. `ServerVersion`, `LegacyText`, and `ItemModelCompatibility` isolate version-sensitive behavior.

## Compatibility Boundary

The common code compiles against Spigot API 1.16.5 and Java 8 bytecode. Do not add direct calls to APIs introduced after 1.16.5.

- Resolve optional materials with `Material.matchMaterial`.
- Keep PDC as the stable item identity mechanism.
- Put modern item model/custom model data component access behind reflection in `compat/`.
- Use `LegacyText`/plain Bukkit strings instead of Adventure in common code.
- Add a safe fallback when a sound, particle, material, or metadata API is not available.

The portable item uses `AMETHYST_SHARD` when present and `COMPASS` on 1.16.5. Modern visual metadata is best-effort; PDC identity remains authoritative.

## Commands

`/magnet` is declared in `src/main/resources/plugin.yml`. `MagnetPlugin` attaches the executor and tab completer through `getCommand`; it never accesses Bukkit's internal command map.

All current subcommands use `magnet.use`, which defaults to `true` to preserve previous behavior.

## Lifecycle

- `PortableMagnetService.start()` and `MagnetCoreManager.start()` cancel an existing task before scheduling.
- Reload refreshes configuration and restarts only the core task.
- `onDisable` cancels both services and saves core state.
- Listeners are registered once during `onEnable`.

## Project Areas

- Commands: `src/main/kotlin/ru/garde/magnet/command/`
- Portable magnet: `src/main/kotlin/ru/garde/magnet/portable/`
- Stationary cores: `src/main/kotlin/ru/garde/magnet/core/`
- Version adapters/fallbacks: `src/main/kotlin/ru/garde/magnet/compat/`
- Config: `src/main/kotlin/ru/garde/magnet/config/`
- Messages: `src/main/kotlin/ru/garde/magnet/message/`
- Resource-pack validation: `src/main/kotlin/ru/garde/magnet/resourcepack/`

## Build And Runtime Matrix

`.\gradlew.bat clean build` creates `Magnet-Universal-1.16.5-1.21.x.jar`.

- `runLegacyServer`: Paper 1.16.5, isolated in `run/legacy-1.16.5`
- `runModernServer`: Paper 1.21.11, isolated in `run/modern-1.21.11`
- `runServer -PrunMinecraftVersion=<version>`: arbitrary Paper version in `run/<version>`

Runtime smoke tests currently cover Paper 1.16.5 and Paper 1.21.11. Do not claim untested server implementations as verified.
