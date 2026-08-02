package com.keplersharvest.world;

/** Immutable tile coordinate. Suitable as a map key. */
public record GridPoint(int x, int y) {

    public GridPoint step(Direction direction) {
        return new GridPoint(x + direction.dx(), y + direction.dy());
    }

    public int manhattanTo(GridPoint other) {
        return Math.abs(x - other.x) + Math.abs(y - other.y);
    }

    @Override
    public String toString() {
        return x + "," + y;
    }

    /** Parses the {@link #toString()} form, used by save files. */
    public static GridPoint parse(String text) {
        int comma = text.indexOf(',');
        if (comma < 0) {
            throw new IllegalArgumentException("Not a grid point: " + text);
        }
        return new GridPoint(
                Integer.parseInt(text.substring(0, comma).trim()),
                Integer.parseInt(text.substring(comma + 1).trim()));
    }
}
