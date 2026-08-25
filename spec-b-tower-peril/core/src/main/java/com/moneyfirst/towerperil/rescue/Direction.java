package com.moneyfirst.towerperil.rescue;

/**
 * The four cardinal directions a pin can pull its line of entities toward.
 * Row-oriented pins must use {@link #LEFT} or {@link #RIGHT}; column-oriented
 * pins must use {@link #UP} or {@link #DOWN}.
 */
public enum Direction {
    UP(0, -1),
    DOWN(0, 1),
    LEFT(-1, 0),
    RIGHT(1, 0);

    public final int dx;
    public final int dy;

    Direction(int dx, int dy) {
        this.dx = dx;
        this.dy = dy;
    }

    public boolean isHorizontal() {
        return this == LEFT || this == RIGHT;
    }

    public boolean isVertical() {
        return this == UP || this == DOWN;
    }
}
