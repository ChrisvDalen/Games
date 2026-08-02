# Kepler's Harvest

A 2D top-down space-colony life simulation. You arrive at Meridian Station, an agricultural colony
on the planet Ilyra that was logged as abandoned three years ago, and find three people still living
in it and no explanation for where the original crew of eleven went.

You grow alien crops, salvage the wrecks, bring three broken colony modules back online, build
relationships with the colonists, and recover the data slates the old crew left behind. What they
were doing turns out to be a decision rather than a disaster, and the decision is now yours.

This is a playable vertical slice: one colony, one farming terrace, one alien biome, four crops,
three modules, three colonists, a crafting bench, a quest line, five crew logs, and one narrative
reveal.

Java 21 + libGDX + Gradle. Everything runs locally: no server, no database, no account, no
downloaded assets.

---

## Running it

Requirements: **JDK 21** and nothing else. The Gradle wrapper fetches Gradle itself, and Gradle
fetches libGDX.

```bash
./gradlew :lwjgl3:run
```

On Windows use `gradlew.bat :lwjgl3:run`.

To build a self-contained jar you can run anywhere:

```bash
./gradlew :lwjgl3:fatJar
java -jar lwjgl3/build/libs/keplers-harvest-0.1.0-all.jar
```

Run the tests:

```bash
./gradlew test
```

Boot the game unattended (opens a window, plays for a few seconds, writes `smoke-test.png`, exits) —
useful on a build machine, since the unit tests deliberately never open a window:

```bash
./gradlew :lwjgl3:fatJar
java -jar lwjgl3/build/libs/keplers-harvest-0.1.0-all.jar --smoke-test 10
```

## Controls

| Key | Action |
| --- | --- |
| `W` `A` `S` `D` or arrow keys | Move |
| `E` | Interact with whatever you are facing — talk, repair, gather, read, harvest, sleep |
| `F` | Use the equipped tool, or plant the selected seed |
| `1`–`8` | Select a toolbar slot |
| `I` | Pack |
| `Q` | Tasks |
| `J` | Evidence journal |
| `Esc` | Pause menu, or close the open panel |
| `Space` / `Enter` | Advance dialogue |
| `F5` | Quick save |

The tile you are facing is outlined, and the prompt at the bottom left tells you what `E` will do
there. Time only runs while you are actually playing — every panel pauses the clock.

## The first loop

1. Start a new game. You land on the pad east of the station with four tools and four Lumen Pod
   seeds.
2. Walk the apron and press `E` on the pale diamonds to cut salvage free — that is your Scrap Alloy.
3. Head east through the shimmer to the **South Terrace**.
4. Face a soil tile, equip the Soil Blade (`1`) and press `F` to break the bed.
5. Select the Lumen Pod Seed (`5`) and press `F` to plant.
6. Equip the Mist Canister (`2`) and press `F` to water. Watering only counts once a day and dries
   out overnight.
7. Sleep in the bunk inside the habitat block (`E` on the bed). Water the bed each morning.
8. After three watered days, face the plant and press `E` to harvest.
9. Gather Resin Fibre and Silica Shards on the terrace, then repair the **Oxygen Recycler** back at
   the station — two stages, one `E` press each once you are carrying the materials. The Field
   Scanner (`4`, then `F`) tells you exactly what a module still needs.
10. Talk to Sefa Odim on the terrace, read the data slate on the station apron, and save.

Sleeping autosaves. There is one manual save slot, written to `~/.keplers-harvest/slot1.json` as
readable JSON.

## Project layout

```
core/      All game logic and rendering. Platform-independent.
lwjgl3/    Desktop launcher (LWJGL3 backend).
assets/    Content: JSON configuration, Tiled maps. No code.
tools/     Developer scripts (map generation).
docs/      Architecture notes and the placeholder-asset guide.
```

`core` is organised by feature, not by layer:

| Package | Responsibility |
| --- | --- |
| `configuration` | Reading and cross-validating every content file at start-up |
| `game` | `GameSession` (the aggregate root), events, interaction rules |
| `player` | Position, facing, energy |
| `world` | Tiled map loading, collision, movement, placed objects |
| `farming` | Soil, crops, growth over days |
| `colony` | Repairable modules and the benefits they unlock |
| `inventory` | Items, stacks, the pack, the toolbar |
| `crafting` | Recipes and the bench |
| `npc` | Colonists, schedules, relationships |
| `dialogue` | Conditional dialogue trees and their effects |
| `quests` | Quest definitions, objectives, progress |
| `mystery` | Crew logs, the evidence journal, the reveal |
| `time` | The colony clock, day phases, sleeping |
| `save` | Save format, validation, migration |
| `render` / `ui` / `screens` | Everything that draws. Reads game state, never writes it |

The rule that holds this together: **nothing in a rules package touches `Gdx.*` statics.** That is
why the whole first-milestone loop — gather, till, plant, water, sleep, harvest, repair, talk, read,
save, reload — is exercised by a unit test with no window open
(`core/src/test/java/com/keplersharvest/game/FirstMilestoneTest.java`).

## Adding content

Almost everything is data. Nothing below needs a recompile of engine code.

**A new crop** — add an entry to `assets/config/crops.json` with its `stageDays`, plus a seed item
and a produce item in `assets/config/items.json`. The seed's `plants` field names the crop.

**A new recipe** — add it to `assets/config/recipes.json` with a `station` matching a
`crafting_station` object in a map. Set `requiresModule` to gate it behind a repair.

**A new module** — add it to `assets/config/modules.json` with its repair stages and benefit, then
place a `module` object in a map with a `module` property naming its id.

**A new quest** — add it to `assets/config/quests.json`. Objective types are `collectItem`,
`repairModule`, `talkTo`, `discoverLandmark`, `findCrewLog`, `findAnyCrewLogs`, `craftItem`,
`harvestCrop` and `reachRelationship`.

**A new colonist** — add them to `assets/config/colonists.json` with a daily schedule, and write
`assets/config/dialogue/<id>.json`.

**A new map** — save a `.tmj` from Tiled into `assets/maps/` and add one line to
`assets/config/maps.json`. Layers named `ground`, `overlay`, `collision` and `farmable` are read, as
is one object layer. Portals into it are validated at start-up, so a typo fails loudly on launch
rather than quietly mid-walk.

The starting maps were written as code rather than by hand; `python3 tools/generate_maps.py`
regenerates them. Once generated they are ordinary Tiled files and can be edited in Tiled directly.

If any content file is malformed or refers to something that does not exist, the game says so on the
title screen instead of starting.

## Placeholder art

Every sprite and tile is a flat colour generated at runtime from the content files. The project
ships no binary artwork beyond one small generated PNG that exists purely so the maps open with
visible tiles in Tiled. See [docs/ASSETS.md](docs/ASSETS.md) for how to replace them with real
sprites.

## Testing

108 tests, all in `core/src/test`, none of which open a window:

```bash
./gradlew test
```

They cover inventory stacking and atomic removal, crop growth and withering, time progression and
sleeping, repair material requirements, crafting, relationship tiers, quest objectives and
prerequisites, conditional dialogue, journal and reveal gating, save validation and migration, map
and portal integrity, and the complete first-milestone loop end to end. The shipped JSON is loaded
by the tests, so a content mistake fails the build.

## Known limitations

- One save slot. The format is versioned and has a migration hook, but only the initial version
  exists.
- Colonists teleport between scheduled posts at each phase change rather than walking there.
- No audio.
- No controller or mouse-driven movement; keyboard only.
- The reveal ends the authored story. Repairing every module and finishing every quest is possible,
  but there is no post-slice content after it.
- Placeholder art throughout — readable and consistent, but geometric.

## Originality

The setting, characters, dialogue, crops, colony modules, maps, mystery and all artwork are
original to this project. It shares a genre and a broad structure (top-down, day cycle, farming,
relationships) with other life simulations; it contains no assets, names, text, art or content
copied from any of them.
