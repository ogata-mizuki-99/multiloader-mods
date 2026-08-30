# Good Sleep

Quality-of-life **sleep** tweaks for **NeoForge 26.x** (Minecraft 26.1.2+). Rest in bed during the day, heal while sleeping, and skip nights with one player in multiplayer.

**Mod ID**: `good_sleep`

## Requirements

| | |
|---|---|
| Minecraft | 26.1.2 – 26.1.x |
| Loader | NeoForge 26.1.2+ |
| Java | 25 |
| Side | Client and server |

## Features

| Feature | Config key | Default | Description |
|---------|------------|---------|-------------|
| Day sleep | `allowDaySleep` | `true` | Sleep during day/dawn — time advances to night or morning |
| Health regen | `healWhileSleeping` | `true` | Recover HP while in bed |
| Heal interval | `healIntervalTicks` | `40` | Ticks between heal ticks (20 = 1 sec). `0` = full heal on lie down |
| One-player skip | `onePlayerSkip` | `true` | One sleeping player skips the night in multiplayer |

Configure in the **Mods list → Good Sleep → Config** (NeoForge mod config UI).

## Server note

When **one-player skip** is enabled, the mod sets `playersSleepingPercentage` to **0** on server start and config reload. Turn the option off if you manage sleep rules manually or use another sleep mod.

## Build

```powershell
.\gradlew :good-sleep:build
```

Output: `mods/good-sleep/build/libs/good_sleep-26.1.2-*.jar`

## Distribution

Download from [Modrinth](https://modrinth.com) or [CurseForge](https://www.curseforge.com), or [GitHub Releases](https://github.com/ogata-mizuki-99/multiloader-mods/releases).

## License

All Rights Reserved — see [LICENSE](./LICENSE).

## Author

Ogata Mizuki ([@Ogata_Mizuki_99](https://www.youtube.com/@Ogata_Mizuki_99))

## Credits

This mod was developed with partial assistance from AI (Claude, Gemini, etc.) to improve development efficiency and optimize code. All game balance decisions and playtesting were performed manually by the developer. If you encounter any unexpected issues, please report them via [GitHub Issues](https://github.com/ogata-mizuki-99/multiloader-mods/issues).

---

## 日本語概要

昼間睡眠・睡眠中 HP 回復・1 人夜スキップ。各機能は Mod 設定から個別に ON/OFF できます。

本MODは、開発効率の向上およびコードの最適化のために、一部AI（Claude / Gemini等）による支援を受けて開発されています。ゲームバランスや動作テストはすべて開発者本人が手動で行っていますが、万が一予期せぬ不具合が見つかった場合は、GitHubの [Issue](https://github.com/ogata-mizuki-99/multiloader-mods/issues) までご報告いただけますと幸いです。
