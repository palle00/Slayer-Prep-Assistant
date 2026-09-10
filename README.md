# SlayerPrepAssistant

SlayerPrepAssistant is a RuneLite Plugin Hub plugin for preparing Slayer tasks. It detects or simulates the current assignment, resolves possible monsters, normalizes strategy recommendations, compares them with local player state, and presents a preparation checklist.

Version 1 is intentionally generic. Fixture guides prove the pipeline without coupling the architecture to one monster. Live OSRS Wiki and price integrations are isolated behind dedicated services and are opt-in.

## Privacy

Player-specific information stays local. The plugin must not send player name, bank contents, inventory, equipment, stats, position, Slayer progress, or account data to the Wiki or any third-party service. Wiki requests are only for public pages and public price data.

## Safety

The plugin is informational only. It does not click, equip, withdraw, move, select prayers, attack NPCs, inject input, or send menu actions.

## Attribution

Strategy data: Old School RuneScape Wiki.

## Development

Build and test:

```text
./gradlew test
```

Launch a development client:

```text
./gradlew run
```
