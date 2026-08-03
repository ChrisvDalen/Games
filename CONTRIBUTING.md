# Contributing

Thanks for looking. This is a small project with a strong opinion about where
code goes, so this file is mostly about that opinion.

## Getting set up

You need **JDK 21** and nothing else.

```bash
./gradlew build          # compile, run the tests, build the jars
./gradlew test           # tests only
./gradlew :lwjgl3:run    # play it
```

`./gradlew build` is what CI runs. If it passes locally it will pass there.

## The one rule

**Game rules never touch `Gdx.*` statics.** Rendering, input and Scene2D sit on
top and are the only things allowed to.

This is not taste. It is what lets `FirstMilestoneTest` gather salvage, till a
bed, plant, water, sleep three nights, harvest, repair a module, hold a
conversation, save and reload — in under a second, with no window and no GL
context. A rule that cannot be tested that way is a rule in the wrong place.

The dividing line is drawn in [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md).
`configuration/GdxResourceReader` is the deliberate exception: it is the seam
where file access enters, and `FileResourceReader` is its windowless twin used
by tests.

## Changing content versus changing the engine

Most changes need no Java at all. Crops, items, recipes, modules, quests,
colonists, dialogue and maps are JSON under `assets/config` — the README's
"Adding content" section covers each one.

Content is cross-validated at start-up. A seed naming a missing crop or a portal
pointing at a map that does not exist fails on the title screen rather than
quietly mid-walk, and because the tests load the shipped JSON, it fails the
build first.

## Tests

Every rule change needs a test, and it must not open a window. Tests live in
`core/src/test` and run against the real content files rather than fixtures.

Useful helpers:

- `TestContent.newSession()` — a fresh session on a fixed seed
- `TestPlayer.face(...)` / `faceFromAnySide(...)` — put the player where you need
  them, as if they had walked there
- `TestPlayer.equip(...)` — select a toolbar slot by item id

Coverage is reported by JaCoCo into `*/build/reports/jacoco/test`, and CI
uploads it as an artifact.

## Style

- Four-space indent, UTF-8, LF endings, ~110 column lines. `.editorconfig` has
  the details and most editors will pick it up.
- `-Xlint:all` is on. Do not add warnings.
- Import types rather than writing them fully qualified inline.
- Comments explain *why*. The code already says what.
- Match the surrounding code. It is consistent; keep it that way.

## Art

There is no image editor in this workflow, and that is on purpose. World sprites
are palette-index grids in `SpriteShapes.java`, the font is a glyph table in
`PixelGlyphs.java`, and panels are nine-patches built in code by
`UiSkinFactory`. See [docs/ASSETS.md](docs/ASSETS.md).

The font atlas is generated, not hand-edited:

```bash
./gradlew generatePixelFont     # assets/ui/pixel-font.{png,fnt}
./gradlew generateTilesetImage  # assets/maps/meridian_tileset.png, for Tiled only
```

Commit the regenerated files along with the table you changed.

## Saves

The save format is versioned. If you change the shape of `SaveData`, bump
`SaveData.CURRENT_VERSION` and add a `SaveMigrations.Step` that upgrades older
files — with a test that loads a file in the old format. Someone's colony is in
there.

## Pull requests

- Branch off `main`, keep the change focused.
- Say what a reviewer should look at, and what you deliberately left out.
- Green CI, including the headless smoke test.

## Licensing of contributions

By contributing you agree your work is licensed as the rest of the project is:
code under MIT, game content under CC BY-NC-SA 4.0. See
[LICENSE-ASSETS.md](LICENSE-ASSETS.md) for which is which.
