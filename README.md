# OgataMizuki Multiloader MODs

Minecraft **26.2** / NeoForge & Fabric 向けの公開 MOD 群です。Gradle マルチプロジェクトでまとめてビルドできます。

| MOD | Gradle | mod id | License |
|-----|--------|--------|---------|
| Guide Lib | `:guide-lib` | `guide_lib` | MIT |
| Deconstructor | `:deconstructor` | `deconstructor` | All Rights Reserved |
| Radial Teleport | `:radial-teleport` | `radial_teleport` | All Rights Reserved |
| Good Sleep | `:good-sleep` | `good_sleep` | All Rights Reserved |
| Elytra Slot | `:elytra-slot` | `elytra_slot` | All Rights Reserved |
| Private Locker Chest | `:private-chest` | `privatechest` | All Rights Reserved |
| Nickname | `:nickname` | `nickname` | All Rights Reserved |
| Economy | `:economy` | `economy` | All Rights Reserved |
| Lookalike | `:lookalike` | `lookalike` | All Rights Reserved |
| Instant Structure | `:instant-structure` | `instant_structure` | All Rights Reserved |

各 MOD の説明・依存関係は `mods/<name>/README.md` を参照してください。

## Requirements

**All public MODs require:**

- **Minecraft 26.2**
- **NeoForge 26.2+** and/or **Fabric Loader + Fabric API** for 26.2
- **Java 25** (included with the Minecraft 26.2 launcher)

Older Minecraft / Forge versions are not supported on this branch (`multiloader-26.2`).  
For 26.1.2 builds, use branch `multiloader-26.1.2`.

## Build

```powershell
# 全 MOD
.\gradlew build

# 単体
.\gradlew :deconstructor:build
```

JAR は `mods/<name>/build/libs/` に出力されます。

## Optional dependencies

一部 MOD は任意依存（JEI / Mod Menu 等）を持ちます。詳細は各 `mods/<name>/README.md` を参照。

## License

リポジトリ全体ではなく、**各 `mods/<name>/LICENSE`** を正とします（Guide Lib = MIT、他は All Rights Reserved が多い）。

## Source

- GitHub: https://github.com/ogata-mizuki-99/multiloader-mods
- Branch for MC 26.2: `multiloader-26.2`
