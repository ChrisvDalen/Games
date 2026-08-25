package com.moneyfirst.pourperfect.game;

import com.moneyfirst.pourperfect.model.Level;
import com.moneyfirst.pourperfect.model.LevelGenerator;
import com.moneyfirst.pourperfect.model.LiquidColor;
import com.moneyfirst.pourperfect.model.Move;
import com.moneyfirst.pourperfect.model.Tube;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameSessionTest {

    /**
     * Hand-built fixture for pour/undo mechanics tests: only tube-content bookkeeping matters
     * here, not whether the puzzle is fully solvable end to end.
     */
    private static Level twoTubeLevel() {
        Tube a = Tube.of(4, LiquidColor.BLUE, LiquidColor.RED, LiquidColor.RED);
        Tube b = Tube.of(4, LiquidColor.RED);
        return new Level(0L, 1, List.of(a, b));
    }

    /**
     * Generator-built fixture (guaranteed solvable - see {@code LevelGeneratorTest}) for hint
     * tests, which must exercise a puzzle {@link com.moneyfirst.pourperfect.model.PuzzleSolver}
     * can actually solve.
     */
    private static Level solvableLevel() {
        LevelGenerator.GenerationConfig config = new LevelGenerator.GenerationConfig(2, 4, 1);
        return LevelGenerator.generate(1, 555L, config);
    }

    @Test
    void legalPourSucceedsAndAdvancesMoveCount() {
        GameSession session = new GameSession(twoTubeLevel(), 3, 3);

        PourResult result = session.pour(0, 1);

        assertEquals(PourResult.POURED, result);
        assertEquals(1, session.getMovesMade());
        assertEquals(3, session.getLevel().getTube(1).size());
    }

    @Test
    void illegalPourIsRejectedAndLeavesStateUnchanged() {
        GameSession session = new GameSession(twoTubeLevel(), 3, 3);
        // tube 1 top is RED, tube 0 top is RED too, but pour into itself is what we test instead:
        PourResult illegal = session.pour(1, 1);

        assertEquals(PourResult.ILLEGAL_MOVE, illegal);
        assertEquals(0, session.getMovesMade());
        assertFalse(session.canUndo());
    }

    @Test
    void pourAfterWinIsRejected() {
        Tube solved = Tube.of(2, LiquidColor.RED, LiquidColor.RED);
        Tube empty = new Tube(2);
        GameSession session = new GameSession(new Level(0, 1, List.of(solved, empty)), 3, 3);

        assertTrue(session.isWon());
        assertEquals(PourResult.ALREADY_WON, session.pour(0, 1));
    }

    @Test
    void undoRevertsLastMoveAndConsumesOneCredit() {
        GameSession session = new GameSession(twoTubeLevel(), 3, 2);
        session.pour(0, 1);

        UndoResult result = session.undo();

        assertEquals(UndoResult.UNDONE, result);
        assertEquals(1, session.getUndosRemaining());
        assertEquals(0, session.getMovesMade());
        assertEquals(1, session.getLevel().getTube(1).size(), "tube 1 should be back to its pre-pour state");
    }

    @Test
    void undoWithNoHistoryReportsNothingToUndo() {
        GameSession session = new GameSession(twoTubeLevel(), 3, 3);
        assertEquals(UndoResult.NOTHING_TO_UNDO, session.undo());
    }

    @Test
    void undoNeverGoesNegativeEvenWhenCalledRepeatedly() {
        GameSession session = new GameSession(twoTubeLevel(), 3, 1);
        session.pour(0, 1);

        assertEquals(UndoResult.UNDONE, session.undo());
        assertEquals(0, session.getUndosRemaining());

        // History might be empty now anyway, but even if it weren't, credits must not go negative.
        UndoResult second = session.undo();
        assertTrue(second == UndoResult.NOTHING_TO_UNDO || second == UndoResult.NO_UNDOS_REMAINING);
        assertEquals(0, session.getUndosRemaining());
    }

    @Test
    void undoWithZeroCreditsIsRefusedEvenWithHistoryAvailable() {
        GameSession session = new GameSession(twoTubeLevel(), 3, 0);
        session.pour(0, 1);

        assertEquals(UndoResult.NO_UNDOS_REMAINING, session.undo());
        assertEquals(0, session.getUndosRemaining());
        assertEquals(1, session.getMovesMade(), "refused undo must not revert the move");
    }

    @Test
    void hintNeverGoesNegativeAndReturnsEmptyWhenExhausted() {
        GameSession session = new GameSession(solvableLevel(), 1, 3);

        Optional<Move> first = session.requestHint();
        assertTrue(first.isPresent());
        assertEquals(0, session.getHintsRemaining());

        Optional<Move> second = session.requestHint();
        assertTrue(second.isEmpty());
        assertEquals(0, session.getHintsRemaining());
    }

    @Test
    void hintReturnsALegalMove() {
        GameSession session = new GameSession(solvableLevel(), 3, 3);

        Optional<Move> hint = session.requestHint();

        assertTrue(hint.isPresent());
        Move move = hint.get();
        Tube from = session.getLevel().getTube(move.from());
        Tube to = session.getLevel().getTube(move.to());
        assertTrue(from.canPourInto(to));
    }

    @Test
    void grantHintsAndUndosIncreaseRemainingCounts() {
        GameSession session = new GameSession(twoTubeLevel(), 0, 0);

        session.grantHints(2);
        session.grantUndos(3);

        assertEquals(2, session.getHintsRemaining());
        assertEquals(3, session.getUndosRemaining());
    }

    @Test
    void grantingNegativeAmountsNeverReducesCredits() {
        GameSession session = new GameSession(twoTubeLevel(), 5, 5);

        session.grantHints(-100);
        session.grantUndos(-100);

        assertEquals(5, session.getHintsRemaining());
        assertEquals(5, session.getUndosRemaining());
    }

    @Test
    void constructorClampsNegativeStartingCreditsToZero() {
        GameSession session = new GameSession(twoTubeLevel(), -5, -5);
        assertEquals(0, session.getHintsRemaining());
        assertEquals(0, session.getUndosRemaining());
    }

    @Test
    void addEmptyTubeGrowsTheLevelAndDoesNotBreakUndo() {
        GameSession session = new GameSession(twoTubeLevel(), 3, 3);
        session.pour(0, 1);

        session.addEmptyTube(4);
        assertEquals(3, session.getLevel().tubeCount());

        UndoResult result = session.undo();
        assertEquals(UndoResult.UNDONE, result);
    }

    @Test
    void undoAllRevertsEveryMoveInOneCredit() {
        Tube a = Tube.of(6, LiquidColor.RED, LiquidColor.RED, LiquidColor.BLUE, LiquidColor.BLUE);
        Tube b = new Tube(6);
        Tube c = new Tube(6);
        GameSession session = new GameSession(new Level(0, 1, List.of(a, b, c)), 3, 1);

        session.pour(0, 1); // moves the BLUE,BLUE run into b
        session.pour(0, 2); // moves the RED,RED run into c

        UndoResult result = session.undoAll();

        assertEquals(UndoResult.UNDONE, result);
        assertEquals(0, session.getUndosRemaining());
        assertEquals(0, session.getMovesMade());
        assertEquals(4, session.getLevel().getTube(0).size());
        assertTrue(session.getLevel().getTube(1).isEmpty());
        assertTrue(session.getLevel().getTube(2).isEmpty());
    }
}
