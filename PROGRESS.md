# SlayerPrepAssistant Development Progress

## Completed

- Read root `AGENTS.md`; no nested `AGENTS.md` files were found.
- Audited the existing project and confirmed it was still based on the RuneLite example plugin.
- Documented the generic architecture, threading boundaries, privacy model, and readiness scoring.
- Renamed the Java package path to `com.slayerprepassistant` and removed template greeting behavior.
- Added a RuneLite sidebar panel with task display, target selection, manual monster search, method selection, loadout mode selection, readiness, equipment, inventory, issues, and Wiki attribution/open-link action.
- Added normalized models for task contexts, target options, monster guides, combat methods, gear slots, recommendation tiers, inventory recommendations, requirements, mechanics, travel, ownership, and parsing confidence.
- Added fixture-driven preparation pipeline covering multiple unrelated monster shapes: Bloodveld, Dust devil, Kurask, and Black dragon variants.
- Added player-state comparison for equipment, inventory, and last-known bank snapshot using RuneLite item containers.
- Added Best I Own and Max loadout behavior through a generic `LoadoutBuilder`.
- Added initial readiness scoring service with blockers, warnings, ready items, and unknowns.
- Added Wiki subsystem skeleton: client, response states, page resolver, cache, parser result, and generic strategy parser.
- Added unit tests for task resolution, loadout selection, unknown bank behavior, readiness, and parser method/section detection.
- Fixed plugin enablement risks by removing direct injection of RuneLite's internal `SlayerPluginService`.
- Updated the Gradle wrapper metadata to Gradle 9.1.0 so the project can run on the installed Java 25 runtime.
- Configured Gradle resources so `runelite-plugin.properties` is included in built jars.
- Fixed the observed in-game enable failure from RuneLite logs: startup called `client.getItemContainer(...)` from the AWT plugin-list thread. Task/player-state refreshes now run through `ClientThread.invokeLater`.
- Added optimized plugin navigation icon resource generated from the user-provided `icon.png`.
- Reworked the sidebar toward the provided `img.png` reference: header/status card, target/method/loadout controls, readiness badge, equipment status rows, inventory grid, requirements, mechanics, travel, costs, refresh/open Wiki/search actions.
- Added direct real Slayer task detection from RuneLite client varps/varbits and Slayer game DB tables.
- Added secondary task detection from RuneLite Slayer profile config and fallback parsing for recognized task/count chat messages.
- Added event-driven refresh on Slayer varp/varbit changes.
- Expanded player ownership matching with local item names from `client.getItemDefinition`, plus alias normalization for common variants.
- Replaced fixture item IDs with current `net.runelite.api.gameval.ItemID` constants and added common equivalent variants.

## Currently Working On

- User in-game validation of direct Slayer task detection and ownership matching.
- Refining UI section behavior, including true collapsible sections and item sprites where available.

## Remaining

- Expand live MediaWiki page resolution and strategy parsing beyond the initial subsystem skeleton.
- Add OSRS Wiki price mappings and bulk latest-price integration.
- Add broad parser regression fixtures from real Wiki layouts.
- Polish the sidebar into the full multi-section RuneLite UX.
- User must confirm behavior in-game after launching the development client.

## Known Problems

- Gradle now runs on Java 25 after updating the wrapper metadata to Gradle 9.1.0.
- Real in-game Slayer behavior cannot be verified by Codex; only the user can confirm it in RuneLite.

## Decisions

- Use lowercase Java package `com.slayerprepassistant`.
- Keep third-party Wiki and price features disabled by default because they contact external services.
- Treat bank state as unknown until the bank container has been observed.
- Keep task fallback mappings isolated in `TaskResolutionOverrides`.

## Testing Notes

- Run `./gradlew test` after each meaningful phase.
- Test fixtures should cover multiple unrelated monster and strategy shapes.
- Current direct verification command compiled main/test sources and ran 6 JUnit tests successfully.
- `./gradlew test shadowJar` now passes and the shadow jar contains `runelite-plugin.properties`.
- After the client-thread fix, `./gradlew test shadowJar` passes again.
- After the icon/UI pass, `./gradlew test shadowJar` passes and the jar includes `slayer-prep-assistant-icon.png`.
- After direct task detection and ownership matching changes, `./gradlew test shadowJar` passes.
