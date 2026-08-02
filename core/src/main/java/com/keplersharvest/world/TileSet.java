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

    private final String name;
    private final int firstGid;
    private final Map<Integer, String> colours;
    private final Map<Integer, String> labels;

    public TileSet(String name, int firstGid, Map<Integer, String> colours, Map<Integer, String> labels) {
        this.name = Objects.requireNonNull(name, "name");
        this.firstGid = firstGid;
        this.colours = Map.copyOf(Objects.requireNonNull(colours, "colours"));
        this.labels = Map.copyOf(Objects.requireNonNull(labels, "labels"));
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
}
