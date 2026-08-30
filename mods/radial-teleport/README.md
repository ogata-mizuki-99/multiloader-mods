# Radial Teleport

**Teleport Compass** with a radial (pizza-slice) menu for **NeoForge 26.x** (Minecraft 26.1.2+). Jump to world spawn, online players, or saved waypoints — **no operator permissions required**.

## Requirements

| | |
|---|---|
| Minecraft | 26.1.2 – 26.1.x |
| Loader | NeoForge 26.1.2+ |
| Java | 25 |
| Side | Client and server |

## Features

- Hold **right-click** to open the radial teleport UI
- Destinations: **world spawn**, **online players**, **personal waypoints** (amber slices)
- **Waypoint save screen**: modifier key + right-click (default: Left Shift) — name defaults to coordinates
- **Waypoint edit screen**: left-click the center hub while the menu is open — rename, reorder, delete
- Hover a slice to see the **full label** (truncated when not hovered)
- Teleport cooldown (configurable)
- **Safe landing** — if the destination is inside blocks, searches nearby for standing room
- Integrates with **Nickname** mod when installed (shows nicknames on player slices)
- In-game **Config** screen (Mods list) + `config/radial_teleport-common.toml`
- Crafting recipe for Teleport Compass (toggle in config)
- No OP / permission nodes required for normal use

## Usage

1. Obtain the Teleport Compass (craft, creative tab, or `/radialteleport give`)
2. Hold **right-click** to open the radial menu
3. **Left-click** a slice to teleport (keep holding right-click until you click)
4. **Save waypoint**: hold modifier key + **right-click** (menu closed) → enter name → Save
5. **Edit waypoints**: open menu → **left-click center hub** → scroll list if needed → rename / ▲▼ / delete → Done

## Configuration

File: `config/radial_teleport-common.toml` (or **Mods → Radial Teleport → Config**)

| Key | Default | Description |
|-----|---------|-------------|
| `enableCraftingRecipe` | `true` | When `false`, the Teleport Compass recipe is disabled |
| `enableWaypoints` | `true` | Personal waypoint save / edit / radial display |
| `maxWaypointsPerPlayer` | `8` | Max saved waypoints per player (1–32) |
| `teleportCooldownTicks` | `0` | Cooldown between teleports in ticks (20 = 1 s; 0 = off) |

Key bind: **Options → Controls → Radial Teleport → Waypoint Modifier Key** (default: Left Shift)

## Crafting

When enabled (default):

```
 E       E = Ender Pearl
CPE      C = Compass, P = Echo Shard
 E
```

## Optional Dependencies

| Mod | Effect |
|-----|--------|
| Nickname | Player slices show nicknames instead of usernames |
| Guide Lib | Codex / manual for crafting and usage (JSON in this mod) |

## Build

```powershell
.\gradlew :radial-teleport:build
```

Output: `mods/radial-teleport/build/libs/radial_teleport-26.1.2-*.jar`

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

テレポートコンパス長押しでピザ型 UI を表示。スポーン・オンラインプレイヤー・個人ウェイポイントへ移動（OP 不要）。

本MODは、開発効率の向上およびコードの最適化のために、一部AI（Claude / Gemini等）による支援を受けて開発されています。ゲームバランスや動作テストはすべて開発者本人が手動で行っていますが、万が一予期せぬ不具合が見つかった場合は、GitHubの [Issue](https://github.com/ogata-mizuki-99/multiloader-mods/issues) までご報告いただけますと幸いです。

- **保存**: 修飾キー + 右クリック → 名称（座標が初期値）を入力して保存
- **編集**: メニュー中に中央ハブ左クリック → 名称変更・表示順（▲▼）・削除
- **設定**: MOD 一覧の Config または `config/radial_teleport-common.toml`
