# Elytra Slot

Adds a **dedicated elytra equipment slot** for **NeoForge 26.x** (Minecraft 26.1.2+). Fly with elytra while keeping your chestplate equipped.

## Requirements

| | |
|---|---|
| Minecraft | 26.1.2 – 26.1.x |
| Loader | NeoForge 26.1.2+ |
| Java | 25 |
| Side | Client and server |

## Features

- Separate elytra slot in the player inventory GUI
- Does not replace chest armor — wear elytra and chestplate together
- Elytra rendering on players and humanoid mobs (Mixin)
- Server-authoritative sync via data attachment

## Compatibility

Do **not** install alongside other elytra-slot mods (Curios elytra attachments, etc.) — Mixin conflicts may occur.

## Usage

1. Open your inventory
2. Place elytra in the dedicated elytra slot (beside armor)
3. Glide as usual — chestplate slot remains independent

## Build

```powershell
.\gradlew :elytra-slot:build
```

Output: `mods/elytra-slot/build/libs/elytra_slot-26.1.2-*.jar`

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

チェスト防具と干渉しないエリトラ専用スロットを追加する MOD です。他のエリトラスロット系 MOD との併用は非推奨です。

本MODは、開発効率の向上およびコードの最適化のために、一部AI（Claude / Gemini等）による支援を受けて開発されています。ゲームバランスや動作テストはすべて開発者本人が手動で行っていますが、万が一予期せぬ不具合が見つかった場合は、GitHubの [Issue](https://github.com/ogata-mizuki-99/multiloader-mods/issues) までご報告いただけますと幸いです。
