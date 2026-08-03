# What is licensed how

Kepler's Harvest is dual-licensed. The engine is meant to be reusable; the world
it runs is not.

## MIT — the code

Everything that makes the game *work*, regardless of where it lives:

- `core/src/**` and `lwjgl3/src/**` — all Java source, including the tests
- `build.gradle`, `settings.gradle`, `gradle/**`, `gradlew`, `gradlew.bat`
- `tools/**` — the map generation scripts
- `.github/**` and other build configuration

Take it, fork it, ship a farming game with it. Keep the copyright notice.

Note that two Java files sit on the line: `SpriteShapes.java` and
`PixelGlyphs.java` are source files whose *contents* are artwork — the sprite
grids and the glyph table. The classes around them are MIT; the pixel data
inside them is content, and falls under CC BY-NC-SA below.

## CC BY-NC-SA 4.0 — the game content

Everything that makes it *this* game:

- `assets/config/**` — the crops, items, recipes, modules, quests, colonists,
  dialogue trees, crew logs and the reveal
- `assets/maps/**` — the maps and the tileset
- `assets/ui/**` — the pixel font atlas and descriptor
- the sprite grids in `core/src/main/java/com/keplersharvest/render/SpriteShapes.java`
- the glyph table in `core/src/main/java/com/keplersharvest/tools/PixelGlyphs.java`
- `README.md` and `docs/**` prose

Meridian Station, Ilyra, the colonists, the crew logs and what happened to the
original eleven are the story this project exists to tell. Share and adapt them
with credit, non-commercially, under the same terms.

## Rebuilding the engine into your own game

That is the intended use of the split. Delete `assets/config`, `assets/maps` and
the sprite and glyph tables, write your own, and everything you keep is MIT.
The loader validates content at start-up, so it will tell you plainly what is
still missing.

## Contributions

Contributions are accepted under these same terms — code as MIT, content as
CC BY-NC-SA 4.0. See [CONTRIBUTING.md](CONTRIBUTING.md).
