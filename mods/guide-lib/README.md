# Guide Lib

Lightweight, data-driven in-game guide library for **NeoForge 26.x** (Minecraft 26.1.2+).

Content mods register guide books with JSON under `data/<modid>/guide/` — **no hard dependency** on Guide Lib is required. Players browse all books from the **Codex** item.

> Patchouli-style documentation for the 26.x era. See [Roadmap](#roadmap) for planned features.

## Requirements

| | |
|---|---|
| Minecraft | 26.1.2 – 26.1.x |
| Loader | NeoForge 26.1.2+ |
| Java | 25 |
| JEI | Optional (recipe slot click-through) |

## Quick Start (Mod Authors)

### 1. Add a guide book

Create `src/main/resources/data/<modid>/guide/<book_id>.json`:

```json
{
  "name": "guide_lib.book.my_mod",
  "description": "guide_lib.book.my_mod.desc",
  "icon": "mymod:my_item",
  "sort_priority": 50,
  "list_columns": 2,
  "categories": [
    {
      "id": "main",
      "name": "guide_lib.category.my_mod.main",
      "entries": ["overview"]
    }
  ],
  "entries": {
    "overview": {
      "name": "guide_lib.entry.my_mod.overview",
      "icon": "mymod:my_item",
      "pages": [
        { "type": "text", "text": "guide_lib.mymod.page.overview" },
        { "type": "crafting", "recipe": "mymod:my_recipe" },
        { "type": "spotlight", "item": "mymod:my_item", "text": "guide_lib.mymod.page.spotlight" }
      ]
    }
  }
}
```

Book ID becomes `<modid>:<book_id>` (e.g. `mymod:my_mod`).

Add translation keys to `assets/<modid>/lang/en_us.json` (and `ja_jp.json` if needed).

#### Codex visibility (optional)

| field | default | effect |
|-------|---------|--------|
| `codex_visible` | `true` | `false` hides the book from the Codex list; `GuideLibMod.openBook()` still works |
| `dev_only` | `false` | `true` shows in Codex only in dev (`gradlew runClient`); hidden in production launcher / published JAR |

Guide Lib's built-in sample (`guide_lib:example`) sets `"dev_only": true`.

### 2. Optional — per-mod Manual item

Recipe result uses `minecraft:written_book` with a `guide_lib:guide_access` component:

```json
{
  "type": "minecraft:crafting_shapeless",
  "ingredients": ["minecraft:book", "mymod:my_item"],
  "result": {
    "count": 1,
    "id": "minecraft:written_book",
    "components": {
      "minecraft:custom_name": { "translate": "item.mymod.manual" },
      "guide_lib:guide_access": {
        "kind": "manual",
        "book": "mymod:my_mod",
        "theme": "guide_lib:book"
      }
    }
  }
}
```

Optional metadata: `data/<modid>/guide/manuals/<id>.json`

### 3. Declare dependency (recommended)

In `neoforge.mods.toml`:

```toml
[[dependencies.mymod]]
    modId="guide_lib"
    type="optional"
    versionRange="[1.0.0,)"
    ordering="AFTER"
    side="BOTH"
```

Your mod runs without Guide Lib; guides simply won't be readable.

### 4. Sample book

Guide Lib ships `data/guide_lib/guide/example.json` — open the Codex in-game to inspect it.

Real-world integration: [Deconstructor](../deconstructor/) (`data/deconstructor/guide/deconstructor.json`).

## Page Types

| type | fields | notes |
|------|--------|-------|
| `text` | `text` | Translation key or `literal:...` |
| `crafting` | `recipe` | Recipe ID (e.g. `mymod:foo`) |
| `spotlight` | `item`, `text` | Item icon + description |

Long `text` / `spotlight` content is split into multiple pages automatically.

## Themes

Built-in themes: `guide_lib:book` (parchment UI), `guide_lib:tablet` (scrollable cyber UI).

Custom themes: `data/<modid>/guide/themes/<id>.json` — see `data/guide_lib/guide/themes/book.json`.

Set per book: `"theme": "guide_lib:tablet"` in the book JSON.

## Codex Items

| Item | Recipe | Opens |
|------|--------|-------|
| Codex | Book + paper ×2 | All registered books |
| Codex Tablet | Codex + redstone + glass pane | Same (tablet theme) |

Items use `minecraft:written_book` + DataComponents for world-safe removal.

## Open a Book from Code (client)

```java
GuideLibMod.openBook(Identifier.fromNamespaceAndPath("mymod", "my_mod"));
```

## Gradle Dependency

After publishing to Modrinth / CurseForge, add one of:

**Modrinth Maven**

```gradle
repositories {
    maven { url = "https://api.modrinth.com/maven" }
}
dependencies {
    // runtimeOnly "maven.modrinth:guide-lib:<VERSION>+neoforge"
}
```

**CurseMaven**

```gradle
repositories {
    maven { url = "https://cursemaven.com" }
}
dependencies {
    // runtimeOnly "curse.maven:guide-lib-<PROJECT_ID>:<FILE_ID>"
}
```

Replace placeholders after the mod is listed on each platform. For local dev, copy the JAR into `run/mods/` or use a composite build.

## Build

```powershell
.\gradlew :guide-lib:build
```

Output: `mods/guide-lib/build/libs/guide_lib-26.1.2-*.jar`

## Distribution

Download from [Modrinth](https://modrinth.com) or [CurseForge](https://www.curseforge.com), or [GitHub Releases](https://github.com/ogata-mizuki-99/multiloader-mods/releases).

## Roadmap

| Phase | Status | Items |
|-------|--------|-------|
| 1–2 | Done | Codex, themes, Manual, JEI, 2-column index |
| 3 | Planned | GUI `?` → `openBook`, advancement Codex reward |
| 4 | Planned | Codex search |

## License

MIT — see [LICENSE](./LICENSE).

## Author

Ogata Mizuki ([@Ogata_Mizuki_99](https://www.youtube.com/@Ogata_Mizuki_99))

## Credits

This mod was developed with partial assistance from AI (Claude, Gemini, etc.) to improve development efficiency and optimize code. All game balance decisions and playtesting were performed manually by the developer. If you encounter any unexpected issues, please report them via [GitHub Issues](https://github.com/ogata-mizuki-99/multiloader-mods/issues).

---

## 日本語概要

NeoForge 26.x 向けの軽量ガイド基盤 MOD です。`data/<modid>/guide/*.json` に JSON を置くだけで Codex から参照できます。詳細は上記 Quick Start を参照してください。

本MODは、開発効率の向上およびコードの最適化のために、一部AI（Claude / Gemini等）による支援を受けて開発されています。ゲームバランスや動作テストはすべて開発者本人が手動で行っていますが、万が一予期せぬ不具合が見つかった場合は、GitHubの [Issue](https://github.com/ogata-mizuki-99/multiloader-mods/issues) までご報告いただけますと幸いです。
