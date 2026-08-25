package com.moneyfirst.pourperfect.model;

/** A single pour, identified by source/target tube index within a {@link Level}'s tube list. */
public record Move(int from, int to) {
    public Move {
        if (from == to) {
            throw new IllegalArgumentException("a move cannot pour a tube into itself: " + from);
        }
    }
}
