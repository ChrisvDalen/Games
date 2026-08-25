package com.moneyfirst.pourperfect.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A single test tube: a fixed-capacity stack of {@link LiquidColor} units. Index 0 of the
 * backing list is the bottom of the tube; the last element is the top (the pourable end).
 *
 * <p>Pour rules (see {@link #canPourInto(Tube)}):
 * <ul>
 *   <li>the source tube must be non-empty;</li>
 *   <li>the target must have spare capacity;</li>
 *   <li>the target must either be empty or have the same top color as the source's top run.</li>
 * </ul>
 * A pour moves the source's entire top run of same-colored units (or as many as fit in the
 * target, whichever is smaller). Pouring out of an already-{@link #isComplete()} tube is legal
 * per these rules (it's never useful during play, since it can only ever be undone at a net cost
 * of moves, but nothing stops it) - {@code GameSession}/the UI are free to grey out a completed
 * tube as a source for player clarity, and {@code PuzzleSolver} deliberately never explores such
 * a move since it can never appear in a shortest solution, but the rule itself stays simple and
 * unconditional here.
 */
public final class Tube {

    private final int capacity;
    private final List<LiquidColor> contents;

    public Tube(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("capacity must be positive, was " + capacity);
        }
        this.capacity = capacity;
        this.contents = new ArrayList<>(capacity);
    }

    /** Convenience factory: a tube already containing the given colors, bottom-to-top order. */
    public static Tube of(int capacity, LiquidColor... colorsBottomToTop) {
        Tube tube = new Tube(capacity);
        for (LiquidColor color : colorsBottomToTop) {
            tube.push(color);
        }
        return tube;
    }

    public int getCapacity() {
        return capacity;
    }

    /** Read-only view of the contents, bottom (index 0) to top (last index). */
    public List<LiquidColor> getContents() {
        return Collections.unmodifiableList(contents);
    }

    public int size() {
        return contents.size();
    }

    public boolean isEmpty() {
        return contents.isEmpty();
    }

    public boolean isFull() {
        return contents.size() == capacity;
    }

    public int freeSpace() {
        return capacity - contents.size();
    }

    /** The color at the top (pourable end) of the tube, or {@code null} if empty. */
    public LiquidColor peekTop() {
        return contents.isEmpty() ? null : contents.get(contents.size() - 1);
    }

    /** Number of consecutive same-colored units at the top of the tube. 0 if empty. */
    public int topRunLength() {
        if (contents.isEmpty()) {
            return 0;
        }
        LiquidColor top = peekTop();
        int run = 0;
        for (int i = contents.size() - 1; i >= 0 && contents.get(i) == top; i--) {
            run++;
        }
        return run;
    }

    /** True when the tube is full and every unit in it is the same color. Empty tubes are not complete. */
    public boolean isComplete() {
        if (contents.isEmpty() || !isFull()) {
            return false;
        }
        LiquidColor first = contents.get(0);
        for (LiquidColor c : contents) {
            if (c != first) {
                return false;
            }
        }
        return true;
    }

    /** A tube counts toward the win condition whether it's empty or a completed single color. */
    public boolean isSolvedOrEmpty() {
        return isEmpty() || isComplete();
    }

    void push(LiquidColor color) {
        if (isFull()) {
            throw new IllegalStateException("cannot push into a full tube");
        }
        contents.add(color);
    }

    private LiquidColor pop() {
        if (isEmpty()) {
            throw new IllegalStateException("cannot pop an empty tube");
        }
        return contents.remove(contents.size() - 1);
    }

    /** Can {@code this} tube legally pour into {@code target} right now? See class javadoc for the rules. */
    public boolean canPourInto(Tube target) {
        if (target == null || target == this) {
            return false;
        }
        if (isEmpty()) {
            return false;
        }
        if (target.isFull()) {
            return false;
        }
        if (!target.isEmpty() && target.peekTop() != peekTop()) {
            return false;
        }
        return true;
    }

    /**
     * Pours the maximal legal amount of the top run from {@code this} into {@code target}.
     *
     * @return the number of units actually moved (always &gt;= 1)
     * @throws IllegalStateException if {@link #canPourInto(Tube)} would return false
     */
    public int pourInto(Tube target) {
        if (!canPourInto(target)) {
            throw new IllegalStateException("illegal pour from " + this + " into " + target);
        }
        LiquidColor color = peekTop();
        int amount = Math.min(topRunLength(), target.freeSpace());
        for (int i = 0; i < amount; i++) {
            pop();
            target.push(color);
        }
        return amount;
    }

    /** Deep copy, independent of this tube - used for undo snapshots and solver state exploration. */
    public Tube copy() {
        Tube copy = new Tube(capacity);
        copy.contents.addAll(this.contents);
        return copy;
    }

    @Override
    public String toString() {
        return "Tube" + contents;
    }
}
