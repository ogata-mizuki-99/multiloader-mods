# Private Locker Chest

Adds a **private 81-slot locker** for **NeoForge 26.x** (Minecraft 26.1.2+). Only the player who placed it can open the storage.

## Requirements

| | |
|---|---|
| Minecraft | 26.1.2 – 26.1.x |
| Loader | NeoForge 26.1.2+ |
| Java | 25 |
| Side | Client and server |

## Features

- Three-block-tall locker (bottom storage + middle storage + top head)
- **81 storage slots** across the bottom and middle sections
- Top block displays an **enlarged 3D skin head** of the owner
- Only the placing player can access — others see a locked message
- Linked break: breaking any part removes the entire structure
- Explosion-resistant (configurable behavior via block logic)

## Guide Integration (optional)

When [Guide Lib](../guide-lib/) is installed, a Codex entry and craftable manual are available.

## Usage

1. Craft and place the Private Locker (requires two empty blocks above)
2. Right-click the bottom or middle section to open your storage
3. Other players cannot access your locker

## Admin command (OP)

| Command | Permission | Description |
|---------|------------|-------------|
| `/privatechest give <targets> <ownerName>` | OP (level 2+) | Give an owner player head item (profile resolved from `ownerName`) |

## Build

```powershell
.\gradlew :private-chest:build
```

Output: `mods/private-chest/build/libs/privatechest-26.1.2-*.jar`

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

設置者のみ開けられる 81 スロットのプライベートロッカー。縦 3 マス構造で、最上段にオーナーの 3D スキンヘッドを表示します。OP 向け: `/privatechest give <targets> <ownerName>`。

本MODは、開発効率の向上およびコードの最適化のために、一部AI（Claude / Gemini等）による支援を受けて開発されています。ゲームバランスや動作テストはすべて開発者本人が手動で行っていますが、万が一予期せぬ不具合が見つかった場合は、GitHubの [Issue](https://github.com/ogata-mizuki-99/multiloader-mods/issues) までご報告いただけますと幸いです。
