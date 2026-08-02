package com.keplersharvest.render;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Disposable;
import com.keplersharvest.configuration.GameContent;
import com.keplersharvest.world.MapObject;
import com.keplersharvest.world.MapObjectKind;
import com.keplersharvest.world.TileSet;
import com.keplersharvest.world.WorldMap;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Bakes {@link SpriteShapes} into a single texture atlas, one entry per shape-and-colour pair the
 * loaded content actually needs.
 *
 * <p>A shape stores palette indices, not colours. Here each index becomes a tone derived from the
 * base colour the content file declares, so four crops share one set of shapes and a fifth crop
 * needs a JSON entry rather than new artwork.
 *
 * <p>Everything lands in one texture, so drawing the whole world is a single texture bind rather
 * than a flush per sprite type.
 */
public final class WorldSprites implements Disposable {

    /** World units per sprite pixel. Two keeps world art chunkier than interface text. */
    public static final int PIXEL_SCALE = 2;
    /** Size of a drawn sprite in world units: exactly one tile. */
    public static final int SPRITE_UNITS = SpriteShapes.SIZE * PIXEL_SCALE;

    /** Second cut of a terrain pattern, alternated across the grid to hide the repeat. */
    public static final String VARIANT_SUFFIX = "_b";

    /** One pixel of padding stops neighbouring atlas entries bleeding into each other. */
    private static final int CELL = SpriteShapes.SIZE + 2;
    private static final int COLUMNS = 12;

    // Fixed colours for things the content files do not describe.
    public static final String SUIT = "d8dde4";
    public static final String VISOR = "5fd8e8";
    public static final String DEAD_PLANT = "7a6a52";
    public static final String SPENT_NODE = "5a5248";
    public static final String BENCH = "8a6a42";
    public static final String BED = "9a7a9a";
    public static final String SLATE = "d8e070";
    public static final String SLATE_GLOW = "f8ffb0";
    public static final String SIGN = "9a8f7a";
    public static final String PORTAL = "7fd8e8";
    public static final String PORTAL_GLOW = "cdf5ff";

    private final Texture atlas;
    private final Map<String, TextureRegion> regions;
    private final TextureRegion fallback;

    private WorldSprites(Texture atlas, Map<String, TextureRegion> regions, TextureRegion fallback) {
        this.atlas = atlas;
        this.regions = regions;
        this.fallback = fallback;
    }

    /** Collects every sprite the loaded content can ask for and bakes them in one pass. */
    public static WorldSprites build(GameContent content) {
        Set<String> requests = new LinkedHashSet<>();

        TileSet tileSet = content.maps().tileSet();
        for (int localId = 0; localId < tileSet.tileCount(); localId++) {
            String pattern = tileSet.patternForLocal(localId);
            String colour = tileSet.colourForLocal(localId, "ff00ff");
            requests.add(key(pattern, colour, null));
            if (SpriteShapes.has(pattern + VARIANT_SUFFIX)) {
                requests.add(key(pattern + VARIANT_SUFFIX, colour, null));
            }
        }

        requests.add(key("soil_tilled", "7a5236", null));
        requests.add(key("soil_tilled", "4a2d1a", null));

        content.crops().values().forEach(crop -> {
            for (int stage = 0; stage <= 3; stage++) {
                requests.add(key("crop_" + stage, crop.colour(), null));
            }
        });
        requests.add(key("crop_dead", DEAD_PLANT, null));

        content.modules().values().forEach(module -> {
            requests.add(key("module_off", module.colour(), null));
            requests.add(key("module_on", module.colour(), null));
        });

        content.colonists().values().forEach(colonist -> {
            requests.add(key("colonist_a", colonist.colour(), null));
            requests.add(key("colonist_b", colonist.colour(), null));
        });

        for (WorldMap map : content.maps().maps()) {
            for (MapObject object : map.objectsOfKind(MapObjectKind.RESOURCE_NODE)) {
                String colour = content.item(object.property("item", ""))
                        .map(item -> item.colour())
                        .orElse("cccccc");
                requests.add(key(nodeShape(object), colour, null));
            }
        }
        requests.add(key("node_gone", SPENT_NODE, null));

        for (String shape : new String[] {"player_down_a", "player_down_b", "player_up_a",
                "player_up_b", "player_side_a", "player_side_b"}) {
            requests.add(key(shape, SUIT, VISOR));
        }

        requests.add(key("bench", BENCH, null));
        requests.add(key("bed", BED, null));
        requests.add(key("slate", SLATE, SLATE_GLOW));
        requests.add(key("sign", SIGN, null));
        requests.add(key("portal", PORTAL, PORTAL_GLOW));

        return bake(requests);
    }

    /**
     * Picks between a terrain pattern and its variant from the tile's position.
     *
     * <p>A single pattern repeated across a field reads as a visible grid; alternating two cuts
     * deterministically breaks that up without storing per-tile data.
     */
    public String terrainShape(String pattern, int x, int y) {
        String variant = pattern + VARIANT_SUFFIX;
        if (!SpriteShapes.has(variant)) {
            return pattern;
        }
        // A plain xor of two odd multipliers alternates like a chessboard, which is its own
        // visible pattern; mixing the bits properly scatters the two cuts instead.
        int hash = x * 0x1f1f1f1f ^ y;
        hash = (hash ^ (hash >>> 15)) * 0x2c1b3c6d;
        hash ^= hash >>> 12;
        return (hash & 1) == 0 ? pattern : variant;
    }

    /** Which node sprite a map object uses: its own {@code sprite} property, else a scrap heap. */
    public static String nodeShape(MapObject object) {
        String declared = object.property("sprite", "");
        return SpriteShapes.has(declared) ? declared : "node_scrap";
    }

    private static WorldSprites bake(Set<String> requests) {
        int count = requests.size() + 1;
        int rows = (int) Math.ceil(count / (double) COLUMNS);
        Pixmap sheet = new Pixmap(COLUMNS * CELL, rows * CELL, Pixmap.Format.RGBA8888);
        sheet.setBlending(Pixmap.Blending.None);
        sheet.setColor(0f, 0f, 0f, 0f);
        sheet.fill();

        Map<String, TextureRegion> regions = new LinkedHashMap<>();
        int index = 0;
        for (String request : requests) {
            String[] parts = request.split("#", -1);
            drawShape(sheet, index, SpriteShapes.rows(parts[0]),
                    palette(parts[1], parts[2].isEmpty() ? null : parts[2]));
            index++;
        }

        // A visible marker for anything asked for that was never registered.
        int fallbackIndex = index;
        drawShape(sheet, fallbackIndex, SpriteShapes.rows("node_gone"), palette("ff00ff", null));

        Texture texture = new Texture(sheet);
        texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        sheet.dispose();

        index = 0;
        for (String request : requests) {
            regions.put(request, regionAt(texture, index++));
        }
        return new WorldSprites(texture, regions, regionAt(texture, fallbackIndex));
    }

    private static TextureRegion regionAt(Texture texture, int index) {
        int x = (index % COLUMNS) * CELL + 1;
        int y = (index / COLUMNS) * CELL + 1;
        return new TextureRegion(texture, x, y, SpriteShapes.SIZE, SpriteShapes.SIZE);
    }

    private static void drawShape(Pixmap sheet, int index, String[] rows, int[] palette) {
        int originX = (index % COLUMNS) * CELL + 1;
        int originY = (index / COLUMNS) * CELL + 1;
        for (int y = 0; y < SpriteShapes.SIZE; y++) {
            String row = rows[y];
            for (int x = 0; x < SpriteShapes.SIZE; x++) {
                char c = row.charAt(x);
                if (c == '.') {
                    continue;
                }
                sheet.drawPixel(originX + x, originY + y, palette[c - '0']);
            }
        }
    }

    /**
     * Builds the five-tone ramp a shape's palette indices refer to.
     *
     * <p>Index 2 is the colour the content declares; the others are derived from it so a new crop
     * or module only ever needs one value in JSON.
     */
    private static int[] palette(String baseHex, String accentHex) {
        Color base = parse(baseHex);
        Color accent = accentHex == null ? lighten(base, 0.55f) : parse(accentHex);
        return new int[] {
            rgba(scale(base, 0.24f)),
            rgba(scale(base, 0.62f)),
            rgba(base),
            rgba(lighten(base, 0.28f)),
            rgba(accent),
        };
    }

    private static Color parse(String hex) {
        try {
            return Color.valueOf(hex.length() == 6 ? hex + "ff" : hex);
        } catch (RuntimeException e) {
            return Color.MAGENTA.cpy();
        }
    }

    private static Color scale(Color colour, float factor) {
        return new Color(colour.r * factor, colour.g * factor, colour.b * factor, 1f);
    }

    private static Color lighten(Color colour, float amount) {
        return new Color(
                colour.r + (1f - colour.r) * amount,
                colour.g + (1f - colour.g) * amount,
                colour.b + (1f - colour.b) * amount,
                1f);
    }

    private static int rgba(Color colour) {
        return Color.rgba8888(colour);
    }

    private static String key(String shape, String baseHex, String accentHex) {
        return shape + "#" + baseHex + "#" + (accentHex == null ? "" : accentHex);
    }

    /** The baked region for a shape tinted from {@code baseHex}; accent tone derived. */
    public TextureRegion get(String shape, String baseHex) {
        return get(shape, baseHex, null);
    }

    public TextureRegion get(String shape, String baseHex, String accentHex) {
        return regions.getOrDefault(key(shape, baseHex, accentHex), fallback);
    }

    @Override
    public void dispose() {
        atlas.dispose();
    }
}
