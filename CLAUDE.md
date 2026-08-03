# Kepler's Harvest — working notes

A 2D top-down space-colony life sim. Java 21 + libGDX + Gradle, no server, no
network, no downloaded assets. `core` holds the game, `lwjgl3` launches it.

## Commands

```bash
./gradlew build          # compile + test; this is what CI runs
./gradlew test           # tests only, ~5s, never opens a window
./gradlew :lwjgl3:run    # play it
./gradlew :lwjgl3:fatJar # self-contained jar in lwjgl3/build/libs
```

Boot it unattended (opens a window, plays, screenshots, exits):

```bash
./gradlew :lwjgl3:fatJar
cd assets && java -jar ../lwjgl3/build/libs/keplers-harvest-*-all.jar --smoke-test 12
```

Headless, the way CI does it:

```bash
LIBGL_ALWAYS_SOFTWARE=1 xvfb-run -a --server-args="-screen 0 1280x720x24" \
  java -jar ../lwjgl3/build/libs/keplers-harvest-*-all.jar --smoke-test 12
```

## The rule that matters

**Nothing in a rules package touches `Gdx.*` statics.** `render/`, `ui/` and
`screens/` may; everything below them may not.

That is what makes the windowless tests possible, and it is the first thing to
check before putting code somewhere. `configuration/GdxResourceReader` is the
one sanctioned exception — it is the seam where file access enters, paired with
`FileResourceReader` for tests.

`GameSession` is the aggregate root. It exposes itself to other systems only
through two narrow interfaces — `StoryState` (what dialogue may see and change)
and `QuestWorldView` (what objectives may read). Do not widen them casually.
`PlayerActions` is separate on purpose: the session holds state and applies
rules, `PlayerActions` decides what a keypress means.

Systems talk through `GameEvent` on the `EventBus`, not by calling each other.
Farming does not know quests exist. Keep it that way.

Full detail in `docs/ARCHITECTURE.md`. Read it before restructuring anything.

## Content is data

Crops, items, recipes, modules, quests, colonists, dialogue and maps are JSON in
`assets/config`. Adding any of them needs **no Java**. The README's "Adding
content" section lists the required fields per type.

`ContentLoader` cross-validates everything at start-up — a seed naming a missing
crop, a portal to a map that does not exist. The tests load the shipped JSON, so
a content mistake fails the build, not the player.

## Art without an image editor

- World sprites: palette-index grids in `render/SpriteShapes.java`
- Font: glyph table in `tools/PixelGlyphs.java`, baked by `./gradlew generatePixelFont`
- Panels and slots: nine-patches built in code by `ui/UiSkinFactory`

A new crop needs a colour in `crops.json`, not a drawing. See `docs/ASSETS.md`.

## Saves

One slot, at `~/.keplers-harvest/slot1.json`, written as readable JSON via a
temp file and an atomic move. Loading never throws — problems come back as a
`LoadOutcome`.

If you change the shape of `SaveData`: bump `SaveData.CURRENT_VERSION`, add a
`SaveMigrations.Step`, and add a test that loads a file in the *old* format.

The random stream is saved as a state, not just a seed (`WorldRandom`), so
reloading does not rewind rolls the player already made. Use `session.random()`
for anything that needs to be reproducible.

## Conventions

- Four-space indent, ~110 columns, `-Xlint:all` clean — do not add warnings.
- Import types; do not write them fully qualified inline.
- Comments say *why*. Match the density and voice of the surrounding code; it is
  consistent, and worth reading a neighbouring file before writing a new one.
- Dependency versions live in `gradle/libs.versions.toml`, not in build files.
- Every rule change needs a windowless test. `TestContent.newSession()` and the
  `TestPlayer` helpers are how tests set up a scenario.
