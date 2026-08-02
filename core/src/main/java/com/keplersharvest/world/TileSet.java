package com.keplersharvest.world;

import java.util.Map;
import java.util.Objects;

/**
 * Tile appearance data read from a Tiled {@code .tsj} tileset.
 *
 * <p>The renderer draws flat placeholder colours rather than sampling the tileset image, so the
 * game runs with no binary art at all. The referenced PNG exists only so the maps open in Tiled.
 */
public final class TileSet {

    /** Used when a tile does not name one, so a new tile is never invisible. */
    public static final String DEFAULT_PATTERN = "ground_flat";

    private final String name;
    private final int firstGid;
    private final Map<Integer, String> colours;
    private final Map<Integer, String> labels;
    private final Map<Integer, String> patterns;

    public TileSet(String name, int firstGid, Map<Integer, String> colours, Map<Integer, String> labels,
                   Map<Integer, String> patterns) {
        this.name = Objects.requireNonNull(name, "name");
        this.firstGid = firstGid;
        this.colours = Map.copyOf(Objects.requireNonNull(colours, "colours"));
        this.labels = Map.copyOf(Objects.requireNonNull(labels, "labels"));
        this.patterns = Map.copyOf(Objects.requireNonNull(patterns, "patterns"));
    }

    public String name() {
        return name;
    }

    public int tileCount() {
        return colours.size();
    }

    /** Placeholder colour for a global tile id, or {@code fallback} for empty/unknown tiles. */
    public String colourFor(int gid, String fallback) {
        if (gid <= 0) {
            return fallback;
        }
        return colours.getOrDefault(gid - firstGid, fallback);
    }

    /** Human-readable tile name, for debugging and for the Tiled-facing PNG generator. */
    public String labelFor(int localId) {
        return labels.getOrDefault(localId, "tile" + localId);
    }

    public String colourForLocal(int localId, String fallback) {
        return colours.getOrDefault(localId, fallback);
    }

    /** Sprite shape a tile is drawn with, for a global tile id. */
    public String patternFor(int gid) {
        return gid <= 0 ? DEFAULT_PATTERN : patternForLocal(gid - firstGid);
    }

    public String patternForLocal(int localId) {
        return patterns.getOrDefault(localId, DEFAULT_PATTERN);
    }
}
