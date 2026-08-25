package com.moneyfirst.pourperfect.game;

import com.moneyfirst.pourperfect.model.Level;
import com.moneyfirst.pourperfect.model.Move;
import com.moneyfirst.pourperfect.model.PuzzleSolver;
import com.moneyfirst.pourperfect.model.Tube;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Optional;

/**
 * Orchestrates play of a single {@link Level}: pouring, undo, hints and win detection. Holds
 * only in-memory session state (the current level, an undo history of full tube snapshots, and
 * this session's hint/undo credit counters) - persistence of credits across sessions is the
 * caller's job (see {@code LevelProgressionState}), keeping this class trivially unit-testable.
 *
 * <p>Hint/undo counters are guarded so they can never go negative: every consuming operation
 * checks the remaining count before decrementing.
 */
public final class GameSession {

    private static final int HINT_SOLVER_STATE_BUDGET = 40_000;

    private final Level level;
    private final Deque<List<Tube>> history = new ArrayDeque<>();
    private int hintsRemaining;
    private int undosRemaining;
    private int movesMade;

    public GameSession(Level level, int hintsRemaining, int undosRemaining) {
        this.level = level;
        this.hintsRemaining = Math.max(0, hintsRemaining);
        this.undosRemaining = Math.max(0, undosRemaining);
    }

    public Level getLevel() {
        return level;
    }

    public boolean isWon() {
        return level.isWon();
    }

    public int getHintsRemaining() {
        return hintsRemaining;
    }

    public int getUndosRemaining() {
        return undosRemaining;
    }

    public int getMovesMade() {
        return movesMade;
    }

    public boolean canUndo() {
        return !history.isEmpty();
    }

    /** Grants extra hint credits, e.g. after a rewarded-video watch or an IAP purchase. */
    public void grantHints(int amount) {
        hintsRemaining += Math.max(0, amount);
    }

    /** Grants extra undo credits, e.g. after a rewarded-video watch or an IAP purchase. */
    public void grantUndos(int amount) {
        undosRemaining += Math.max(0, amount);
    }

    /** Adds an extra empty tube to the level, mid-session (the "extra tube" rewarded-video perk). */
    public void addEmptyTube(int capacity) {
        level.addTube(new Tube(capacity));
    }

    /** Attempts to pour tube {@code from} into tube {@code to}. */
    public PourResult pour(int from, int to) {
        if (isWon()) {
            return PourResult.ALREADY_WON;
        }
        Tube source = level.getTube(from);
        Tube target = level.getTube(to);
        if (!source.canPourInto(target)) {
            return PourResult.ILLEGAL_MOVE;
        }

        history.push(snapshot());
        source.pourInto(target);
        movesMade++;
        return PourResult.POURED;
    }

    /** Reverts the most recent pour, consuming one undo credit. */
    public UndoResult undo() {
        if (history.isEmpty()) {
            return UndoResult.NOTHING_TO_UNDO;
        }
        if (undosRemaining <= 0) {
            return UndoResult.NO_UNDOS_REMAINING;
        }
        List<Tube> previous = history.pop();
        restoreFrom(previous);
        undosRemaining--;
        movesMade = Math.max(0, movesMade - 1);
        return UndoResult.UNDONE;
    }

    /**
     * Reverts every move made this session (the "undo-all" rewarded-video perk), consuming a
     * single undo credit regardless of how many moves are unwound.
     */
    public UndoResult undoAll() {
        if (history.isEmpty()) {
            return UndoResult.NOTHING_TO_UNDO;
        }
        if (undosRemaining <= 0) {
            return UndoResult.NO_UNDOS_REMAINING;
        }
        List<Tube> initial = history.peekLast();
        restoreFrom(initial);
        history.clear();
        undosRemaining--;
        movesMade = 0;
        return UndoResult.UNDONE;
    }

    /**
     * Consumes one hint credit and returns a legal next move toward the solution, if the solver
     * can find one within its search budget. Returns {@link Optional#empty()} - without consuming
     * a credit - when the player has no hints left or the current position is a dead end (no
     * solution reachable, e.g. after a mistaken manual move).
     */
    public Optional<Move> requestHint() {
        if (hintsRemaining <= 0) {
            return Optional.empty();
        }
        Optional<List<Move>> solution = PuzzleSolver.solve(level.getTubes(), HINT_SOLVER_STATE_BUDGET);
        if (solution.isEmpty() || solution.get().isEmpty()) {
            return Optional.empty();
        }
        hintsRemaining--;
        return Optional.of(solution.get().get(0));
    }

    private List<Tube> snapshot() {
        List<Tube> copy = new ArrayList<>(level.tubeCount());
        for (Tube tube : level.getTubes()) {
            copy.add(tube.copy());
        }
        return copy;
    }

    private void restoreFrom(List<Tube> snapshot) {
        level.restoreFrom(snapshot);
    }
}
