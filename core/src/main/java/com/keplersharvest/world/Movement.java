package com.keplersharvest.world;

/**
 * Axis-separated collision resolution against a map's solid tiles.
 *
 * <p>Moving each axis independently lets the player slide along walls instead of sticking. Pure
 * static maths, so it can be tested without a renderer.
 */
public final class Movement {

    private Movement() {
    }

    /**
     * Moves a square body and stops it at solid tiles.
     *
     * @param halfSize half the body's width, in tile units
     */
    public static WorldPosition resolve(WorldMap map, WorldPosition from, float dx, float dy, float halfSize) {
        float x = from.x();
        float y = from.y();
        if (dx != 0f) {
            float candidate = x + dx;
            if (!collides(map, candidate, y, halfSize)) {
                x = candidate;
            }
        }
        if (dy != 0f) {
            float candidate = y + dy;
            if (!collides(map, x, candidate, halfSize)) {
                y = candidate;
            }
        }
        return new WorldPosition(x, y);
    }

    /** True when a body centred at {@code (x, y)} overlaps any solid tile. */
    public static boolean collides(WorldMap map, float x, float y, float halfSize) {
        int minX = (int) Math.floor(x - halfSize);
        int maxX = (int) Math.floor(x + halfSize);
        int minY = (int) Math.floor(y - halfSize);
        int maxY = (int) Math.floor(y + halfSize);
        for (int tileY = minY; tileY <= maxY; tileY++) {
            for (int tileX = minX; tileX <= maxX; tileX++) {
                if (map.blocked(tileX, tileY)) {
                    return true;
                }
            }
        }
        return false;
    }

    /** Nearest free tile to {@code preferred}, searched in rings; used when a spawn is blocked. */
    public static GridPoint nearestFreeTile(WorldMap map, GridPoint preferred) {
        if (!map.blocked(preferred)) {
            return preferred;
        }
        int maxRadius = Math.max(map.width(), map.height());
        for (int radius = 1; radius <= maxRadius; radius++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dx = -radius; dx <= radius; dx++) {
                    if (Math.abs(dx) != radius && Math.abs(dy) != radius) {
                        continue;
                    }
                    GridPoint candidate = new GridPoint(preferred.x() + dx, preferred.y() + dy);
                    if (map.inBounds(candidate) && !map.blocked(candidate)) {
                        return candidate;
                    }
                }
            }
        }
        return preferred;
    }
}
