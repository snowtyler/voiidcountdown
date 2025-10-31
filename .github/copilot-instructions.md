# Copilot instructions for Voiid Countdown Timer

This repo ships three deliverables plus docs. Keep them separate when editing:
- Datapack: `DarkWither-datapack/` (Minecraft functions + tags)
- Resource pack: `PROPHECY-resourcepack\` (assets for the datapack; downloaded by players upon joining the server)
- Bukkit/Paper plugin: `voiidcountdown-plugin/` (Java, Maven)
- Docs: `voiidcountdown-docs/` (MDX content; built/published externally)

## Architecture in short
- Datapack entrypoints
  - Load: `data/minecraft/tags/function/load.json` → `vct:internal/init/setup`
  - Tick: `data/minecraft/tags/function/tick.json` → `vct:internal/api/tick`
  - Self-ticking: `internal/api/addtick.mcfunction` does `schedule function vct:internal/api/addtick 1t`
- Datapack timer model (scoreboard objective `Timer`)
  - Digits: `Hour`, `Minute1`, `Minute2`, `Second1`, `Second2`
  - State: `Tick`, `TimerSecs`, `MaxTimerSecs`, `PausedTimer`, `EndedTimer`, `Style`, `LanguageTimer`
  - Bossbar id: `voiidtimer:bar` (value/max/name/color set per tick)
  - Rollover: `internal/api/normalize.mcfunction` (carry 10s/6s → next unit). Negative guards live in `internal/api/tick.mcfunction`.
  - Finish: `internal/api/ended.mcfunction` → `custom/on_end.mcfunction` → `internal/api/reset.mcfunction`
  - Styles: `custom/styles_manager.mcfunction` dispatches `Style` → `custom/styles/*.mcfunction` (don’t delete this file)
  - Versioned book: `function/book.mcfunction` calls `internal/api/3940/*` or `4298/*` by `McVersionToNBTNew`
- Plugin overview
  - Main: `src/main/java/voiidstudios/vct/VoiidCountdownTimer.java` (commands, events, PlaceholderAPI, bStats, update check, version mapping)
  - Command: `commands/MainCommand.java` implements `/vct` (`help|reload|set <HH:MM:SS> [timerId]|pause|resume|stop|modify ...`)
  - API: `api/VCTActions.java`, `api/Timer.java`, events via `api/VCTEvent.java`
  - Config/messages: `src/main/resources/config.yml` (timers under `Timers:*`, messages under `Messages.*`)

## Conventions
- Time format is `HHH:MM:SS` (hours can be 1-3 digits, max 999:59:59) across plugin and datapack.
- Bossbar styles
  - Datapack: add a style at `.../custom/styles/<name>.mcfunction` and wire it in `custom/styles_manager.mcfunction` with `execute if score Style Timer matches <n> run function vct:custom/styles/<name>`.
  - Plugin: `bossbar_style` one of `SOLID|SEGMENTED_6|SEGMENTED_10|SEGMENTED_12|SEGMENTED_20`; `bossbar_color` must match Bukkit enum.
- Text formatting (plugin): `Config.text_format = MINIMESSAGE|LEGACY|UNIVERSAL`. Preserve legacy color codes in default messages.
- Placeholders: `%HH%` shows full hours (001-999), `%H1%`/`%H2%`/`%H3%` expose individual digits. PAPI: `%vct_timer_h1%`, `%vct_timer_h2%`, `%vct_timer_h3%`.
- Keep scoreboard math consistent: if you add digits/logic, update both `normalize.mcfunction` and the negative clamps in `tick.mcfunction`.

## Developer workflows
- Plugin build (Maven, Java 8): outputs `target/VoiidCountdownTimer-<version>.jar` with provided deps (Spigot API, Adventure, PlaceholderAPI).
- Run plugin: drop jar in a Spigot/Paper/Purpur 1.13+ server `plugins/` and restart; main command is `voiidcountdowntimer` (alias `vct`). Permissions: `voiidcountdowntimer.admin`.
- Datapack: zip `voiidcountdown-datapack/` (keep `pack.mcmeta` at root) → place in world `datapacks/`; use `/function vct:book` and `/scoreboard` to tweak `Style`/`LanguageTimer`.
- Docs: edit MDX under `voiidcountdown-docs/`; images live in `voiidcountdown-docs/images/**`.

## Pointers and examples
- Bossbar paused label per language: see `custom/styles/white.mcfunction` (checks `PausedTimer` + `LanguageTimer`).
- Tick flow: `internal/api/tick.mcfunction` updates bossbar max/value, decrements seconds, calls style manager, normalizes, and triggers finish.
- Command tab-complete: extend builders at bottom of `commands/MainCommand.java` (`onTabComplete`).

# Deletions

- If we request to clean up or remove a feature, ensure to delete all related code in both the plugin and/or datapack (e.g., styles, commands, config entries). Files can be removed from the respective directories as needed using PowerShell or command line.

## Gotchas
- Do not remove `custom/styles_manager.mcfunction`; the tick loop depends on it every tick.
- Keep namespaced function paths stable when referenced from `data/minecraft/tags/**` or `schedule function` calls.
- `plugin.yml` defines command name (`voiidcountdowntimer`) and alias (`vct`); keep in sync with code when renaming.

## MCP Context7 docs helper
- When you need up-to-date library docs or usage examples, use the Context7 MCP server instead of generic web search.
- Typical flow:
  - Resolve the library ID for what you need (e.g., Spigot/Paper API, Kyori Adventure, PlaceholderAPI).
  - Fetch docs with a focused topic and a sensible token cap (2k–5k), then apply to the codebase.
- Useful targets for this repo:
  - Spigot/Paper: bossbar API, scheduler, command/TabCompleter, event lifecycle.
  - Kyori Adventure: MiniMessage formatting, legacy serializer interoperability.
  - PlaceholderAPI: registering expansions, placeholder formatting.
- Prefer stable APIs and versions matching `pom.xml` (`spigot-api 1.13.2`, `adventure 4.24.0`) and `plugin.yml` (`api-version: 1.13`). If docs conflict, follow existing code patterns here.

use context7