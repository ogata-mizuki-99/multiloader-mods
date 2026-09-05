# Economy (NeoForge)

Standalone in-game economy for Minecraft **26.1.2** / NeoForge **26.1.2+**.

**No external server required** — Node.js, PostgreSQL, Docker, and HTTP APIs are not needed. All economy data is stored inside your Minecraft world (singleplayer, LAN, or dedicated server).

> Private monorepo: run Gradle from the `client/` directory.

## Features

- Currency (wallet, bank, debt) with HUD
- Action rewards (mining, combat, fishing, farming, etc.)
- NPC shops (buy / sell / ETF / flea market)
- ATM, loans, player flea market, rankings
- In-game admin block GUI (balance view, master config, reset)

## Requirements

- Minecraft **26.1.2**
- NeoForge **26.1.2+**
- Java **25**

## Optional dependencies

| MOD | Purpose |
|-----|---------|
| [Nickname](../nickname/) | Display names in rankings and admin views |

Works without Nickname — vanilla player names are used.

## Multiplayer

Install on **both client and server**. Works on integrated singleplayer, Open to LAN, and dedicated servers. No separate API process.

## What this repository does **not** include

This public tree contains only the standalone `economy` mod (`mod id: economy`).

It does **not** include:

- `economy-legacy` (`mizukieconomy`) — archived HTTP + PostgreSQL reference under `mods/economy-legacy/` (**not in Gradle build**; see its README)
- Node.js / Express API (`server/` in the private dev repo)

Those remain private and are not required to play with this mod.

## Build

```powershell
# from repo root (public clone) or client/ (private monorepo)
.\gradlew :economy:build
.\gradlew :economy:test
```

JAR: `mods/economy/build/libs/economy-26.1.2-<version>.jar` (current: `1.0.0`)

Automated tests: `mods/economy/run-automated-tests.ps1`

## Configuration

On **first world load**, the mod exports the bundled master to:

```text
config/economy/economy_master.json
```

Edit this file to add items (including from other mods), shop listings, and prices. The in-game admin block GUI can adjust existing values and add new rows via **行を追加** on the Master tab (Rewards / Prices / Shop assignment / ETF composition).

Restart the world after manual JSON changes. GUI saves apply immediately.

`config/economy-common.toml` only controls reward chat aggregation timing.

## License

All Rights Reserved — see [LICENSE](./LICENSE).
