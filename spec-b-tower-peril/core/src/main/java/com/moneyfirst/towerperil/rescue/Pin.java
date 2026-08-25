package com.moneyfirst.towerperil.rescue;

/**
 * A pin plugs the exit of exactly one row or column. It always sits at the
 * grid-edge cell in its {@link #pullDirection} - pulling it removes that
 * plug and opens a path to the exit for anything sliding down that line.
 */
public final class Pin extends GridEntity {
    private final Orientation orientation;
    private final int lineIndex;
    private final Direction pullDirection;

    public Pin(String id, Orientation orientation, int lineIndex, Direction pullDirection, int col, int row) {
        super(id, col, row);
        if (orientation == Orientation.ROW && !pullDirection.isHorizontal()) {
            throw new IllegalArgumentException("Row pins must pull LEFT or RIGHT, got " + pullDirection);
        }
        if (orientation == Orientation.COLUMN && !pullDirection.isVertical()) {
            throw new IllegalArgumentException("Column pins must pull UP or DOWN, got " + pullDirection);
        }
        this.orientation = orientation;
        this.lineIndex = lineIndex;
        this.pullDirection = pullDirection;
    }

    public Orientation getOrientation() {
        return orientation;
    }

    public int getLineIndex() {
        return lineIndex;
    }

    public Direction getPullDirection() {
        return pullDirection;
    }

    @Override
    public String toString() {
        return "Pin{" + getId() + " " + orientation + "#" + lineIndex + " -> " + pullDirection + "}";
    }
}
