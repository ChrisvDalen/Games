package com.moneyfirst.pourperfect.model;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PuzzleSolverTest {

    @Test
    void alreadyWonStateSolvesWithZeroMoves() {
        Tube solved = Tube.of(4, LiquidColor.RED, LiquidColor.RED, LiquidColor.RED, LiquidColor.RED);
        Tube empty = new Tube(4);

        Optional<List<Move>> solution = PuzzleSolver.solve(List.of(solved, empty), 1000);

        assertTrue(solution.isPresent());
        assertTrue(solution.get().isEmpty());
        assertTrue(PuzzleSolver.isSolvable(List.of(solved, empty), 1000));
    }

    @Test
    void oneMoveAwayFromWinIsFoundImmediately() {
        Tube almost = Tube.of(4, LiquidColor.RED, LiquidColor.RED, LiquidColor.RED);
        Tube receiver = Tube.of(4, LiquidColor.RED);

        Optional<List<Move>> solution = PuzzleSolver.solve(List.of(almost, receiver), 1000);

        assertTrue(solution.isPresent());
        assertEquals(1, solution.get().size());
        assertEquals(new Move(0, 1), solution.get().get(0));
    }

    @Test
    void unsolvableConfigurationIsCorrectlyReportedAsNotSolvable() {
        // Only 3 total RED units for a capacity-4 tube: RED can never fill a whole tube, and
        // there's no other color to pair it with either, so this can never reach a won state.
        Tube a = Tube.of(4, LiquidColor.RED, LiquidColor.RED);
        Tube b = Tube.of(4, LiquidColor.RED);

        Optional<List<Move>> solution = PuzzleSolver.solve(List.of(a, b), 10_000);

        assertTrue(solution.isEmpty());
        assertFalse(PuzzleSolver.isSolvable(List.of(a, b), 10_000));
    }

    @Test
    void deadlockedStateWithNoLegalMovesAtAllIsUnsolvable() {
        // Two tubes, each full with a different single color already - technically both tubes
        // are individually "complete", so this is actually already won; use a genuinely stuck,
        // not-won configuration instead: two different colors each split across two tubes with
        // mismatched tops and no empty tube to maneuver into.
        Tube a = Tube.of(2, LiquidColor.RED, LiquidColor.BLUE);
        Tube b = Tube.of(2, LiquidColor.BLUE, LiquidColor.RED);

        assertFalse(PuzzleSolver.isSolvable(List.of(a, b), 10_000));
    }

    @Test
    void solverRespectsStateBudgetAndReturnsEmptyRatherThanHanging() {
        Tube a = Tube.of(4, LiquidColor.RED, LiquidColor.RED);
        Tube b = Tube.of(4, LiquidColor.RED);

        // Budget of 0 means "explore nothing beyond the start state" - must return quickly and
        // report no solution found rather than throwing or looping.
        Optional<List<Move>> solution = PuzzleSolver.solve(List.of(a, b), 0);

        assertTrue(solution.isEmpty());
    }
}
