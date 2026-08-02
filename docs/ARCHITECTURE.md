# Architecture

## The one rule

Game rules live in plain Java classes that never touch `Gdx.*` statics. Rendering, input and Scene2D
sit on top and are the only things that do.

That is not architectural taste for its own sake — it is what makes
`FirstMilestoneTest` possible. That test gathers salvage, tills a bed, plants a seed, waters it,
sleeps three nights, harvests, repairs a module in two stages, holds a conversation, recovers a crew
log, saves and reloads, and asserts on the result. It runs in under a second with no window and no
GL context. Any rule that cannot be tested that way is a rule in the wrong place.

## Layers

```
screens/  PlayScreen, TitleScreen            input -> calls, state -> pixels
ui/       Hud, Overlay + 7 panels            Scene2D, reads session state
render/   WorldRenderer, Placeholders        draws the map from game state
------------------------------------------- no Gdx statics below this line
game/     GameSession, PlayerActions, events the aggregate root
farming/ colony/ inventory/ crafting/ npc/ dialogue/ quests/ mystery/ time/ world/ player/
save/     capture, restore, validate, migrate
configuration/  load and cross-validate all content
```

## GameSession

`GameSession` is the aggregate root. It owns the clock, the player, the inventory, farmland per map,
colony repair state, relationships, colonists, the quest log, the journal and world-object state,
and it wires them together. Screens hold one.

It implements two narrow interfaces rather than exposing itself wholesale:

- **`StoryState`** — what dialogue conditions and effects are allowed to see and change.
- **`QuestWorldView`** — what quest objectives are allowed to read.

Those seams mean a dialogue tree or an objective can be tested against a five-line stub, and that
neither system can reach into parts of the world it has no business touching.

`PlayerActions` is deliberately separate. `GameSession` holds state and applies rules; `PlayerActions`
decides what "the player pressed E while facing this tile" means. Keeping them apart stopped the
session from growing an input-shaped bulge.

## Events

Systems do not call each other. Farming does not know quests exist. When something happens, the
session publishes a `GameEvent` and interested systems react:

```java
session.publish(new GameEvent.CropHarvested("lumen_pod", "lumen_pod", 2));
```

`EventBus` queues events raised from inside a handler and drains them afterwards, so a chain of
reactions (harvest → quest completes → reward granted → another quest starts) cannot re-enter a
half-updated listener list.

## Objectives: poll versus accumulate

Quest objectives split into two kinds, and the distinction matters:

- **Conditions** ("hold 4 Scrap Alloy", "the recycler is online") poll `QuestWorldView` every time
  they are read. Spending the alloy un-completes the step, which is correct.
- **Acts** ("harvest 2 Lumen Pods", "fabricate a Circuit Lattice") accumulate a counter from events.
  Having done it once counts forever, even after the produce is spent.

Each `Objective` variant knows which it is and how to read its own progress, so the quest engine has
no switch statement over objective types and adding one is a local change.

The trade-off: an act objective only counts while its quest is active. That is why
`breaking_ground` has no prerequisite — a player who farms before meeting anyone would otherwise
have to harvest twice. Any future act-based quest needs the same consideration.

## Content

Everything authored is JSON under `assets/config`, loaded once by `ContentLoader` into an immutable
`GameContent` record.

Loading ends with a cross-validation pass. A seed that names a missing crop, a recipe that consumes
an item nobody defines, a quest that repairs a module that does not exist, a colonist scheduled onto
a map that is not registered, a portal pointing at a missing spawn — all of these fail at start-up
with a message naming the file and the id. The alternative is a silent no-op discovered an hour into
a playthrough.

Content is read through a `ResourceReader` interface: `Gdx.files` in the game, the filesystem in
tests. Tests load the real shipped JSON, so a content mistake fails the build.

## Maps

Maps are Tiled JSON (`.tmj`), parsed by a hand-written loader rather than libGDX's, for two reasons:
the parser stays pure Java so maps load in unit tests, and the game reads only the subset it needs.

Tile layers named `ground`, `overlay`, `collision` and `farmable` are recognised; anything else is
ignored as decoration. One object layer supplies spawns, portals, modules, resource nodes, crafting
stations, crew logs, beds, landmarks and signs, keyed by Tiled's object class with behaviour driven
by custom properties.

Tiled stores rows top-down; the game grid is bottom-up to match the renderer. The loader flips on
load so neither side has to think about it again.

## Time

`GameClock` converts real seconds into in-game minutes at a configurable rate and emits one event
per elapsed minute, so nothing is dropped on a long frame. Menus call `setPaused(true)`.

Day rollover is a clock listener, not a special case inside `sleep()`. Crops advance, colonists move
to their new posts and a `DayStarted` event fires whether the day turned over because the player
slept or because they stayed up past midnight. Passing out at 02:00 sets a flag that is handled
after the clock finishes advancing, rather than re-entering the clock from inside its own loop.

## Saving

`SaveMapper` converts between the live session and a flat `SaveData` of public fields, which libGDX's
`Json` writes and reads by reflection. The domain classes stay free of serialisation concerns and
the file format can change independently of them.

Three deliberate behaviours:

- **Writes are atomic.** The file is written to `slot1.json.tmp` and moved into place, so a crash
  mid-write cannot leave a truncated save.
- **Reads never throw.** Missing, unparseable, structurally invalid and future-format saves all come
  back as a `LoadOutcome` variant the UI shows to the player.
- **Loading is forgiving about content, strict about structure.** An entry naming an item, quest or
  module that no longer exists is skipped so old saves survive content edits. A negative day counter
  or a stack of zero items is rejected.

`SaveMigrations` runs version-to-version steps on the raw JSON tree before it is mapped, so a step
can rename or default fields that no longer exist as Java members.

## Rendering

World art is hand-drawn 16x16 pixel art stored as palette-index text in `SpriteShapes`, baked by
`WorldSprites` into a single texture at start-up. Storing indices rather than colours is the load-
bearing decision: one crop shape serves all four crops, tinted from the colour each declares in
JSON, so a new crop is a content edit and not an art task. Everything lands in one texture, so the
world draws in a single bind rather than a flush per sprite type.

`Placeholders` survives for the few things that are not sprites — the interaction outline, drop
shadows, the landmark glow.

`WorldRenderer` reads game state and writes none of it. Day/night is a tint factor computed from the
clock and multiplied through every draw.

`UiSkinFactory` builds the Scene2D skin in code: nine-patch panels drawn as concentric bevelled
rings into a Pixmap, plus a bitmap font baked from the original glyph table in `PixelGlyphs`. The
whole interface style - palette, frames, typography - is changed from that one file.

The interface and the world share a 640x360 canvas and nearest-neighbour filtering. That is what
keeps the pixel look honest: at 720p and 1080p a source pixel is exactly two or three screen pixels,
so glyph edges and panel frames never land between them.

## What was deliberately not built

No entity-component system, no generic scripting layer, no scene graph for the world, no reflection
based plugin loading. The brief was a playable vertical slice; a general-purpose engine would have
cost the gameplay. The seams that exist (`ResourceReader`, `StoryState`, `QuestWorldView`,
`GameEvent`, the map object registry) are the ones that were needed to keep the rules testable and
the content in data.
