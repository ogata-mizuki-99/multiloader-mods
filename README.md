# OgataMizuki NeoForge MODs

NeoForge **26.1.2** 向けの公開 MOD 群です。Gradle マルチプロジェクトで 8 MOD をまとめてビルドできます。

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

各 MOD の説明・依存関係は `mods/<name>/README.md` を参照してください。

## Requirements

**All public MODs require:**

- **Minecraft 26.1.2**
- **NeoForge 26.1.2+**
- **Java 25** (included with the Minecraft 26.1.2 launcher)

Older Minecraft / Forge versions are not supported.

## Build

```powershell
# 全 MOD
.\gradlew build

# 単体
.\gradlew :deconstructor:build
```

JAR は `mods/<name>/build/libs/` に出力されます。

## Optional dependencies

| MOD | Optional | Purpose |
|-----|----------|---------|
| deconstructor | guide_lib | In-game manual / Codex |
| radial_teleport | guide_lib, nickname | Manual; player slice labels |
| private-chest | guide_lib | In-game manual / Codex |
| economy | nickname | Display names in rankings / admin |

**Economy** does not require Node.js, PostgreSQL, Docker, or any external API server. Economy data is saved inside your Minecraft world.

Minimum optional version: **1.0.0-beta.1**.  
いずれもハード依存はありません。単体インストールで動作します。

## Distribution

- [Modrinth](https://modrinth.com) / [CurseForge](https://www.curseforge.com) — 各 MOD のプロジェクトページ
- [GitHub Releases](https://github.com/ogata-mizuki-99/neoforge-mods/releases) — JAR 同期

Project icons (256×256): private repo `client/mods/<mod>/marketing/icon.png`（配布時に各プラットフォームへアップロード）

## Credits

These mods were developed with partial assistance from AI (Claude, Gemini, etc.) to improve development efficiency and optimize code. All game balance decisions and playtesting were performed manually by the developer. If you encounter any unexpected issues, please report them via [GitHub Issues](https://github.com/ogata-mizuki-99/neoforge-mods/issues).

本MOD群は、開発効率の向上およびコードの最適化のために、一部AI（Claude / Gemini等）による支援を受けて開発されています。ゲームバランスや動作テストはすべて開発者本人が手動で行っていますが、万が一予期せぬ不具合が見つかった場合は、GitHubの [Issue](https://github.com/ogata-mizuki-99/neoforge-mods/issues) までご報告いただけますと幸いです。

## Author

YouTube でゲーム実況配信を行っています。

**緒方 水輝（Ogata Mizuki）** — [YouTube @Ogata_Mizuki_99](https://www.youtube.com/@Ogata_Mizuki_99)

## Links

- [GitHub Releases](https://github.com/ogata-mizuki-99/neoforge-mods/releases) — JAR downloads
- [Issues](https://github.com/ogata-mizuki-99/neoforge-mods/issues) — bug reports & feedback
- License: 各 `mods/<name>/LICENSE` を参照（guide-lib のみ MIT）
