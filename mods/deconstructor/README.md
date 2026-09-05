# Deconstructor

Reclaim crafting materials and manage enchantments for **NeoForge 26.x** (Minecraft 26.1.2+).

## Requirements

| | |
|---|---|
| Minecraft | 26.1.2 – 26.1.x |
| Loader | NeoForge 26.1.2+ |
| Java | 25 |
| Side | Client and server |
| Guide Lib | Optional (in-game manual + Codex entry) |

## Blocks

| Block | Function |
|-------|----------|
| **Deconstructor** | Break one crafted item back into ingredients per operation |
| **Precision Deconstructor** | Select which output to recover from multi-output crafts |
| **Enchantment Manager** | Extract, separate, combine, and apply enchantments using books |

## Limitations

- **Crafting-table recipes only** — smelting, blasting, and similar processes are not supported
- If multiple crafting recipes produce the same item, only the **first indexed recipe** is used for deconstruction
- Exclude specific item IDs via mod config (`excludedItems`)

## Guide Integration (optional)

When [Guide Lib](../guide-lib/) is installed:

- Deconstructor guide appears in the **Codex**
- Craft the **Deconstructor Manual** (book + deconstructor) for direct access
- Guide JSON: `data/deconstructor/guide/deconstructor.json`

Deconstructor works fully without Guide Lib — guides are optional.

## Build

```powershell
.\gradlew :deconstructor:build
```

Output: `mods/deconstructor/build/libs/deconstructor-26.1.2-*.jar`

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

解体機・精密解体機でクラフト品を素材に戻し、エンチャント管理機で本を使ったエンチャント操作ができます。

本MODは、開発効率の向上およびコードの最適化のために、一部AI（Claude / Gemini等）による支援を受けて開発されています。ゲームバランスや動作テストはすべて開発者本人が手動で行っていますが、万が一予期せぬ不具合が見つかった場合は、GitHubの [Issue](https://github.com/ogata-mizuki-99/multiloader-mods/issues) までご報告いただけますと幸いです。
