# Contributing

Thanks for helping make Magnet easier to maintain.

## Build

Run:

```powershell
gradle build
```

If the Gradle wrapper files are available:

```powershell
.\gradlew.bat build
```

## Code Organization

- Keep `MagnetPlugin.kt` small.
- Put commands in `command/`.
- Put portable magnet behavior in `portable/`.
- Put stationary core behavior in `core/`.
- Put config parsing in `config/`.
- Put localization logic in `message/`.
- Put Paper/version fallback code in `compat/`.
- Put resource-pack diagnostics in `resourcepack/`.

Commands should call services instead of implementing gameplay logic directly. Do not access version-specific Paper APIs outside `compat/`.

## Refactor Rules

- Do not change gameplay behavior during structure-only refactors.
- Do not rename config keys or localization keys.
- Do not change core save formats.
- Do not add dependencies for simple organization work.
- Keep changes small enough to review.

## Commit Messages

Use short English commit messages:

- `feat: short description`
- `fix: short description`
- `refactor: short description`
- `docs: short description`
- `chore: short description`
