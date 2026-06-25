# Manual Test Checklist

## Build Artifact

- [x] `.\gradlew.bat clean build` succeeds
- [x] Jar is named `Magnet-Universal-1.16.5-1.21.x.jar`
- [x] Jar contains `plugin.yml` and no `paper-plugin.yml`
- [x] Main class bytecode is Java 8 / class-file major 52

## Runtime Matrix

- [x] Paper 1.16.5 build 794 starts and enables Magnet
- [x] Paper 1.21.11 build 132 starts and enables Magnet
- [ ] Paper/Spigot 1.20.4
- [ ] Leaf 1.21.x
- [ ] Other intermediate versions required by production

## Commands

- [x] `/magnet` from console
- [x] `/magnet reload` from console
- [x] `/magnet give` rejects console safely
- [x] `/magnet debug item` rejects console safely
- [x] `/magnet core list` from console
- [ ] `/magnet give` as a player
- [ ] Existing core/profile mutation subcommands in game

## Portable Magnet

- [ ] Item is given with the expected localized name and lore
- [ ] PDC marker is present
- [ ] 1.16.5 base material is `COMPASS`
- [ ] 1.17+ base material is `AMETHYST_SHARD`
- [ ] Main hand and off-hand attract dropped metal items
- [ ] Non-metal items are ignored
- [ ] Legacy custom model data and modern item-model fallback render correctly

## Cores

- [ ] 2x2x2 core can be created
- [ ] Core attracts dropped metal items
- [ ] Core list/info commands work in game
- [ ] Core survives reload/restart
- [ ] Core removal works
- [ ] Broken core/frame behavior is unchanged

## Lifecycle And Config

- [x] Plugin disables cleanly on both tested Paper versions
- [x] Reload does not duplicate language-file warnings
- [ ] Reload does not duplicate tasks/listeners during a long-running player test
- [ ] English and Russian messages display correctly in clients
- [ ] Invalid custom material names warn without crashing
- [ ] Resource-pack namespace remains `magnit`
