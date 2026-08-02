package com.keplersharvest.world;

/**
 * A continuous position in tile units: {@code (5.5, 3.5)} is the centre of tile {@code (5, 3)}.
 *
 * <p>Keeping the simulation in tile units rather than pixels means collision, interaction and the
 * renderer's zoom level stay independent of each other.
 */
public record WorldPosition(float x, float y) {

    public GridPoint tile() {
        return new GridPoint((int) Math.floor(x), (int) Math.floor(y));
    }

    public WorldPosition plus(float dx, float dy) {
        return new WorldPosition(x + dx, y + dy);
    }

    /** Centre of the given tile. */
    public static WorldPosition centreOf(GridPoint tile) {
        return new WorldPosition(tile.x() + 0.5f, tile.y() + 0.5f);
    }

    public float distanceTo(WorldPosition other) {
        float dx = x - other.x;
        float dy = y - other.y;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }
}
