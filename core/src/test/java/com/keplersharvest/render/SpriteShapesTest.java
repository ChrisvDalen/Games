package com.keplersharvest.render;

import com.keplersharvest.configuration.GameContent;
import com.keplersharvest.testing.TestContent;
import com.keplersharvest.world.MapObject;
import com.keplersharvest.world.MapObjectKind;
import com.keplersharvest.world.TileSet;
import com.keplersharvest.world.WorldMap;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Sprite shapes are hand-authored text, and content refers to them by name. These checks catch a
 * mistyped shape name or a malformed row before it becomes a magenta square in the world.
 */
class SpriteShapesTest {

    @Test
    @DisplayName("every shape is a well-formed grid of known palette indices")
    void shapesAreWellFormed() {
        assertTrue(SpriteShapes.ids().size() > 20, "the sprite set should not have shrunk by accident");
        for (String id : SpriteShapes.ids()) {
            String[] rows = SpriteShapes.rows(id);
            assertEquals(SpriteShapes.SIZE, rows.length, id + " has the wrong number of rows");
            for (int y = 0; y < rows.length; y++) {
                assertEquals(SpriteShapes.SIZE, rows[y].length(),
                        id + " row " + y + " is the wrong width");
                for (int x = 0; x < rows[y].length(); x++) {
                    char c = rows[y].charAt(x);
                    assertTrue(c == '.' || (c >= '0' && c <= '4'),
                            id + " row " + y + " uses unknown palette index '" + c + "'");
                }
            }
        }
    }

    @Test
    @DisplayName("every tile declares a sprite pattern that exists")
    void tilePatternsResolve() {
        TileSet tileSet = TestContent.load().maps().tileSet();
        for (int localId = 0; localId < tileSet.tileCount(); localId++) {
            String pattern = tileSet.patternForLocal(localId);
            assertTrue(SpriteShapes.has(pattern),
                    "tile " + tileSet.labelFor(localId) + " names unknown sprite '" + pattern + "'");
        }
    }

    @Test
    @DisplayName("every resource node declares a sprite that exists")
    void nodeSpritesResolve() {
        for (WorldMap map : TestContent.load().maps().maps()) {
            for (MapObject node : map.objectsOfKind(MapObjectKind.RESOURCE_NODE)) {
                String declared = node.property("sprite", "");
                assertTrue(declared.isEmpty() || SpriteShapes.has(declared),
                        "node " + node.id() + " on " + map.id() + " names unknown sprite '" + declared + "'");
                assertNotNull(WorldSprites.nodeShape(node));
            }
        }
    }

    @Test
    @DisplayName("the shapes the renderer hard-codes are all present")
    void rendererShapesExist() {
        for (String id : new String[] {
            "player_down_a", "player_down_b", "player_up_a", "player_up_b",
            "player_side_a", "player_side_b", "colonist_a", "colonist_b",
            "crop_0", "crop_1", "crop_2", "crop_3", "crop_dead",
            "soil_tilled", "module_off", "module_on", "node_gone",
            "bench", "bed", "slate", "sign", "portal"}) {
            assertTrue(SpriteShapes.has(id), "missing sprite shape: " + id);
        }
    }

    @Test
    @DisplayName("crops and modules each carry their own colour, so shapes can be shared")
    void contentSuppliesColours() {
        GameContent content = TestContent.load();
        content.crops().values().forEach(crop ->
                assertTrue(crop.colour().matches("[0-9a-fA-F]{6}"),
                        "crop " + crop.id() + " has an unusable colour: " + crop.colour()));
        content.modules().values().forEach(module ->
                assertTrue(module.colour().matches("[0-9a-fA-F]{6}"),
                        "module " + module.id() + " has an unusable colour: " + module.colour()));
        content.colonists().values().forEach(colonist ->
                assertTrue(colonist.colour().matches("[0-9a-fA-F]{6}"),
                        "colonist " + colonist.id() + " has an unusable colour: " + colonist.colour()));
    }

    @Test
    @DisplayName("terrain variants are spread rather than alternating like a chessboard")
    void terrainVariantsAreScattered() {
        // A hash whose low bit is just x xor y produces a visible lattice; check we do better.
        Map<String, Integer> counts = new HashMap<>();
        int diagonalMatches = 0;
        for (int y = 0; y < 24; y++) {
            for (int x = 0; x < 24; x++) {
                String shape = variantFor(x, y);
                counts.merge(shape, 1, Integer::sum);
                if (shape.equals(variantFor(x + 1, y + 1))) {
                    diagonalMatches++;
                }
            }
        }
        assertEquals(2, counts.size(), "both cuts of the pattern should be used");
        counts.values().forEach(count ->
                assertTrue(count > 24 * 24 / 4, "one variant dominates the grid: " + counts));
        assertTrue(diagonalMatches > 40 && diagonalMatches < 536,
                "variants repeat on a fixed diagonal, which reads as a pattern: " + diagonalMatches);
    }

    /** Mirrors {@code WorldSprites.terrainShape} without needing a GL context to build an atlas. */
    private String variantFor(int x, int y) {
        int hash = x * 0x1f1f1f1f ^ y;
        hash = (hash ^ (hash >>> 15)) * 0x2c1b3c6d;
        hash ^= hash >>> 12;
        return (hash & 1) == 0 ? "ground_grain" : "ground_grain_b";
    }
}
