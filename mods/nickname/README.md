# Nickname

Change your **display name** on name tags, tab list, and chat for **NeoForge 26.x** (Minecraft 26.1.2+).

## Requirements

| | |
|---|---|
| Minecraft | 26.1.2 – 26.1.x |
| Loader | NeoForge 26.1.2+ |
| Java | 25 |
| Side | Server required (client for display sync) |

## Commands

| Command | Permission | Description |
|---------|------------|-------------|
| `/nickname <name>` | Anyone | Set your nickname |
| `/nickname set <name>` | Anyone | Same as above |
| `/nickname` | Anyone | Clear your nickname |
| `/nickname clear` | Anyone | Clear your nickname |
| `/nickname clearall` | OP (level 2+) | Clear all nicknames |

Nicknames persist in server world data and sync to all clients.

## Rules

- Maximum **32 characters**
- Formatting codes (`§`) are not allowed
- Plain text only (no color codes)

## Server note

Players can set nicknames that **match another player's username**, which may look like impersonation on tab list, chat, and name tags. Server admins should monitor abuse on competitive or PvP servers.

## Integration

Other mods (e.g. Radial Teleport) can read nicknames for display labels when the Nickname mod is installed.

## Build

```powershell
.\gradlew :nickname:build
```

Output: `mods/nickname/build/libs/nickname-26.1.2-*.jar`

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

`/nickname` でネームタグ・TAB・チャットの表示名を変更できます。OP 不要（個人設定）。最大 32 文字、書式コード不可。他プレイヤー名と同じニックネームも設定可能（なりすまし対策はサーバー運用側）。

本MODは、開発効率の向上およびコードの最適化のために、一部AI（Claude / Gemini等）による支援を受けて開発されています。ゲームバランスや動作テストはすべて開発者本人が手動で行っていますが、万が一予期せぬ不具合が見つかった場合は、GitHubの [Issue](https://github.com/ogata-mizuki-99/multiloader-mods/issues) までご報告いただけますと幸いです。
