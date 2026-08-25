package com.moneyfirst.pourperfect.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TubeTest {

    @Test
    void emptyTubeHasNoTopColorAndZeroRunLength() {
        Tube tube = new Tube(4);
        assertTrue(tube.isEmpty());
        assertNull(tube.peekTop());
        assertEquals(0, tube.topRunLength());
        assertFalse(tube.isComplete());
    }

    @Test
    void topRunLengthCountsOnlyContiguousTopColor() {
        Tube tube = Tube.of(4, LiquidColor.RED, LiquidColor.RED, LiquidColor.BLUE, LiquidColor.BLUE);
        assertEquals(2, tube.topRunLength());
        assertEquals(LiquidColor.BLUE, tube.peekTop());
    }

    @Test
    void isCompleteOnlyWhenFullAndSingleColor() {
        Tube full = Tube.of(4, LiquidColor.RED, LiquidColor.RED, LiquidColor.RED, LiquidColor.RED);
        assertTrue(full.isComplete());

        Tube fullMixed = Tube.of(4, LiquidColor.RED, LiquidColor.RED, LiquidColor.RED, LiquidColor.BLUE);
        assertFalse(fullMixed.isComplete());

        Tube partial = Tube.of(4, LiquidColor.RED, LiquidColor.RED);
        assertFalse(partial.isComplete());

        Tube empty = new Tube(4);
        assertFalse(empty.isComplete());
        assertTrue(empty.isSolvedOrEmpty());
        assertTrue(full.isSolvedOrEmpty());
        assertFalse(partial.isSolvedOrEmpty());
    }

    @Test
    void canPourIntoEmptyTargetWhenSourceNonEmptyAndNotComplete() {
        Tube source = Tube.of(4, LiquidColor.BLUE, LiquidColor.RED);
        Tube target = new Tube(4);
        assertTrue(source.canPourInto(target));
    }

    @Test
    void cannotPourFromEmptySource() {
        Tube source = new Tube(4);
        Tube target = new Tube(4);
        assertFalse(source.canPourInto(target));
    }

    @Test
    void cannotPourIntoFullTarget() {
        Tube source = Tube.of(4, LiquidColor.RED);
        Tube target = Tube.of(4, LiquidColor.RED, LiquidColor.RED, LiquidColor.RED, LiquidColor.RED);
        assertFalse(source.canPourInto(target));
    }

    @Test
    void cannotPourWhenTopColorsMismatch() {
        Tube source = Tube.of(4, LiquidColor.BLUE, LiquidColor.RED);
        Tube target = Tube.of(4, LiquidColor.GREEN);
        assertFalse(source.canPourInto(target));
    }

    @Test
    void canPourWhenTopColorsMatch() {
        Tube source = Tube.of(4, LiquidColor.BLUE, LiquidColor.RED);
        Tube target = Tube.of(4, LiquidColor.RED);
        assertTrue(source.canPourInto(target));
    }

    @Test
    void pouringFromACompletedTubeIsStructurallyLegal() {
        // Never useful during play (see Tube javadoc - GameSession/UI may choose to grey this
        // out, and PuzzleSolver deliberately never explores it), but the pour rule itself doesn't
        // forbid it: a full pure-color tube can still legally pour into an empty target.
        Tube source = Tube.of(4, LiquidColor.RED, LiquidColor.RED, LiquidColor.RED, LiquidColor.RED);
        Tube target = new Tube(4);
        assertTrue(source.canPourInto(target));

        source.pourInto(target);
        assertTrue(source.isEmpty());
        assertTrue(target.isComplete());
    }

    @Test
    void cannotPourIntoSelf() {
        Tube tube = Tube.of(4, LiquidColor.RED);
        assertFalse(tube.canPourInto(tube));
    }

    @Test
    void pourMovesEntireTopRunWhenTargetHasCapacity() {
        Tube source = Tube.of(4, LiquidColor.GREEN, LiquidColor.RED, LiquidColor.RED);
        Tube target = new Tube(4);

        int moved = source.pourInto(target);

        assertEquals(2, moved);
        assertEquals(1, source.size());
        assertEquals(LiquidColor.GREEN, source.peekTop());
        assertEquals(2, target.size());
        assertEquals(LiquidColor.RED, target.peekTop());
    }

    @Test
    void pourIsCappedByTargetFreeSpace() {
        Tube source = Tube.of(4, LiquidColor.RED, LiquidColor.RED, LiquidColor.RED);
        Tube target = Tube.of(4, LiquidColor.RED, LiquidColor.RED, LiquidColor.RED);

        int moved = source.pourInto(target);

        assertEquals(1, moved);
        assertEquals(2, source.size());
        assertTrue(target.isFull());
    }

    @Test
    void pourIntoThrowsWhenIllegal() {
        Tube source = new Tube(4);
        Tube target = new Tube(4);
        assertThrows(IllegalStateException.class, () -> source.pourInto(target));
    }

    @Test
    void copyIsIndependentOfOriginal() {
        Tube original = Tube.of(4, LiquidColor.RED, LiquidColor.BLUE);
        Tube copy = original.copy();

        Tube receiver = new Tube(4);
        copy.pourInto(receiver);

        assertEquals(2, original.size(), "mutating the copy must not affect the original");
        assertEquals(1, copy.size());
    }

    @Test
    void freeSpaceReflectsCapacityMinusSize() {
        Tube tube = Tube.of(4, LiquidColor.RED);
        assertEquals(3, tube.freeSpace());
    }

    @Test
    void constructorRejectsNonPositiveCapacity() {
        assertThrows(IllegalArgumentException.class, () -> new Tube(0));
        assertThrows(IllegalArgumentException.class, () -> new Tube(-1));
    }
}
