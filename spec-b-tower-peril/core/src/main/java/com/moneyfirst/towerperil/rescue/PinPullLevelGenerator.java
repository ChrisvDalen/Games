package com.moneyfirst.towerperil.rescue;

import com.moneyfirst.towerperil.battle.UnitType;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;

/**
 * Deterministic, seeded generator for pin-pull rooms.
 *
 * <p>A candidate room is built by scattering pins, characters and hazards
 * using a {@link Random} seeded from the caller's seed, then verified with a
 * bounded brute-force search ({@link #isSolvable(Grid)}) over pull orders
 * (feasible because rooms carry only a handful of pins). If a candidate
 * isn't solvable, generation retries - deterministically, since the same
 * {@link Random} instance simply keeps consuming its seeded sequence - until
 * a solvable room is found or an attempt cap is hit.
 */
public final class PinPullLevelGenerator {

    public static final int DEFAULT_WIDTH = 8;
    public static final int DEFAULT_HEIGHT = 10;
    private static final int MAX_ATTEMPTS = 1000;

    private PinPullLevelGenerator() {
    }

    public static PinPullLevel generate(long seed) {
        return generate(seed, DEFAULT_WIDTH, DEFAULT_HEIGHT);
    }

    public static PinPullLevel generate(long seed, int width, int height) {
        Random rng = new Random(seed);
        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            Grid candidate = buildCandidate(rng, width, height);
            if (isSolvable(candidate)) {
                return new PinPullLevel(candidate, seed);
            }
        }
        throw new IllegalStateException(
                "Could not generate a solvable Tower Peril level for seed " + seed + " within " + MAX_ATTEMPTS + " attempts");
    }

    private static Grid buildCandidate(Random rng, int width, int height) {
        Grid grid = new Grid(width, height);

        int pinCount = 3 + rng.nextInt(3); // 3..5 pins keeps brute-force search tiny
        Set<Integer> usedRows = new HashSet<>();
        Set<Integer> usedCols = new HashSet<>();
        int pinSeq = 0;
        int placedPins = 0;
        int guard = 0;
        while (placedPins < pinCount && guard++ < 500) {
            if (rng.nextBoolean()) {
                int r = rng.nextInt(height);
                if (usedRows.contains(r)) {
                    continue;
                }
                Direction dir = rng.nextBoolean() ? Direction.LEFT : Direction.RIGHT;
                int col = dir == Direction.RIGHT ? width - 1 : 0;
                if (grid.occupantAt(col, r) != null) {
                    continue;
                }
                grid.place(new Pin("pin-" + pinSeq++, Orientation.ROW, r, dir, col, r));
                usedRows.add(r);
                placedPins++;
            } else {
                int c = rng.nextInt(width);
                if (usedCols.contains(c)) {
                    continue;
                }
                Direction dir = rng.nextBoolean() ? Direction.UP : Direction.DOWN;
                int row = dir == Direction.DOWN ? height - 1 : 0;
                if (grid.occupantAt(c, row) != null) {
                    continue;
                }
                grid.place(new Pin("pin-" + pinSeq++, Orientation.COLUMN, c, dir, c, row));
                usedCols.add(c);
                placedPins++;
            }
        }

        UnitType[] types = UnitType.values();
        int characterCount = 2 + rng.nextInt(3); // 2..4
        int charSeq = 0;
        int placedChars = 0;
        int guardC = 0;
        while (placedChars < characterCount && guardC++ < 1000) {
            int col = rng.nextInt(width);
            int row = rng.nextInt(height);
            if (grid.occupantAt(col, row) != null) {
                continue;
            }
            grid.place(new Character("char-" + charSeq++, types[rng.nextInt(types.length)], col, row));
            placedChars++;
        }

        int hazardCount = rng.nextInt(3); // 0..2
        int hazSeq = 0;
        int placedHaz = 0;
        int guardH = 0;
        while (placedHaz < hazardCount && guardH++ < 1000) {
            int col = rng.nextInt(width);
            int row = rng.nextInt(height);
            if (grid.occupantAt(col, row) != null) {
                continue;
            }
            grid.place(new Hazard("haz-" + hazSeq++, col, row));
            placedHaz++;
        }

        return grid;
    }

    /** True if some order of pulling pins rescues every character with zero losses. */
    public static boolean isSolvable(Grid grid) {
        if (grid.getCharacters().isEmpty()) {
            return false;
        }
        return findSolution(grid).isPresent();
    }

    /**
     * Brute-force search returning a concrete winning pull order, if one
     * exists. A grid with no characters has nothing worth rescuing, so it is
     * never considered to have a solution (consistent with {@link #isSolvable}).
     */
    public static Optional<List<Pin>> findSolution(Grid grid) {
        if (grid.getCharacters().isEmpty()) {
            return Optional.empty();
        }
        List<Pin> path = new ArrayList<>();
        if (search(grid.copy(), path)) {
            return Optional.of(path);
        }
        return Optional.empty();
    }

    private static boolean search(Grid grid, List<Pin> path) {
        if (grid.getCharacters().isEmpty()) {
            return true;
        }
        if (grid.getPins().isEmpty()) {
            return false;
        }
        for (Pin pin : grid.getPins()) {
            Grid branch = grid.copy();
            Pin branchPin = branch.findPin(pin.getOrientation(), pin.getLineIndex());
            PullOutcome outcome = PhysicsResolver.applyPull(branch, branchPin);
            if (!outcome.getLost().isEmpty()) {
                continue;
            }
            path.add(pin);
            if (search(branch, path)) {
                return true;
            }
            path.remove(path.size() - 1);
        }
        return false;
    }
}
