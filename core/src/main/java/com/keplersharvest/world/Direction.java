package com.keplersharvest.world;

/** Facing used for movement, sprite selection and directional interaction. */
public enum Direction {
    UP(0, 1),
    DOWN(0, -1),
    LEFT(-1, 0),
    RIGHT(1, 0);

    private final int dx;
    private final int dy;

    Direction(int dx, int dy) {
        this.dx = dx;
        this.dy = dy;
    }

    public int dx() {
        return dx;
    }

    public int dy() {
        return dy;
    }

    public Direction opposite() {
        return switch (this) {
            case UP -> DOWN;
            case DOWN -> UP;
            case LEFT -> RIGHT;
            case RIGHT -> LEFT;
        };
    }

    /** Facing implied by a movement vector; {@code null} when there is no movement. */
    public static Direction fromVector(float dx, float dy) {
        if (dx == 0f && dy == 0f) {
            return null;
        }
        if (Math.abs(dx) >= Math.abs(dy)) {
            return dx > 0 ? RIGHT : LEFT;
        }
        return dy > 0 ? UP : DOWN;
    }
}
