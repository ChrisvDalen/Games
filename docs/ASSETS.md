# Placeholder art, and how to replace it

## What ships today

The project contains **no artwork files** except one small generated PNG, and the game never loads
even that one.

Every tile, crop, colonist, module, resource node and interface panel is drawn as a flat tinted
rectangle, disc, ring or diamond. The shapes are five small textures built in memory at start-up by
`core/src/main/java/com/keplersharvest/render/Placeholders.java`. The colours come from the content
files:

| What | Where its colour is declared |
| --- | --- |
| Terrain tiles | `assets/maps/meridian_tileset.tsj`, the `colour` property on each tile |
| Items | `assets/config/items.json`, `colour` |
| Crops | `assets/config/crops.json`, `colour` |
| Colony modules | `assets/config/modules.json`, `colour` |
| Colonists | `assets/config/colonists.json`, `colour` |
| Map ambient light | the `ambientColour` map property in each `.tmj` |
| Interface | `core/src/main/java/com/keplersharvest/ui/UiSkinFactory.java` |

Retinting the whole game is therefore a JSON edit. Nothing in `render/` needs to change to add a
fifth crop or a fourth module — the renderer asks the content for a colour.

`assets/maps/meridian_tileset.png` is the one image file. It exists **only** so the `.tmj` maps open
with visible tiles when you edit them in Tiled; the running game does not read it. It is generated
from the tileset colours by:

```bash
./gradlew generateTilesetImage
```

Re-run that after changing a tile colour so Tiled keeps matching what the game draws.

## Swapping in real sprites

The placeholder system is contained on purpose. Three changes replace it:

**1. Load a texture atlas.** Add the atlas to `assets/`, load it in
`KeplersHarvestGame.create()` alongside `Placeholders`, and dispose it in `dispose()`.

**2. Give content a sprite name.** Every definition that has a `colour` field can take a `sprite`
field beside it — `ItemDefinition`, `CropDefinition`, `ModuleDefinition`, `ColonistDefinition`, and
the tileset's per-tile properties. Read it in `ContentLoader` the same way `colour` is read. Keeping
`colour` as a fallback means a half-finished art pass still renders.

**3. Draw the region instead of the shape.** `WorldRenderer` has one drawing call per kind of thing
(`drawCrop`, `drawModule`, `drawResourceNode`, `drawColonists`, `drawPlayer`, `drawTile`). Each
becomes "look up the region, fall back to the placeholder shape". The tint multiply that produces
day/night stays as it is and works unchanged on real art.

For animation, the crop renderer already receives the growth stage index and the player renderer
already receives facing and a movement flag, so those are the hooks a sprite sheet would use.

**4. For the interface**, replace `UiSkinFactory.create()` with `new Skin(Gdx.files.internal(...))`
pointing at a real skin JSON. The style names the panels ask for are `default`, `muted`, `accent`,
`warn`, `heading` and `title` for labels; `default` and `menu` for buttons; `default` for scroll
panes; and the drawables `panel`, `panel-soft`, `scrim`, `slot`, `slot-selected`, `bar-track`,
`bar-fill`, `bar-low`, `divider`, `button`, `button-over` and `button-down`. Provide those names and
nothing else has to change.

## Licensing note

If you add third-party art, check its licence before committing it. The point of the current
approach is that this repository can be cloned and run by anyone with no asset downloads and no
licensing question to answer.
