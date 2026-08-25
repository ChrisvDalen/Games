package com.moneyfirst.towerperil.rescue;

import java.util.ArrayList;
import java.util.List;

/**
 * Deterministic pin-pull resolution.
 *
 * <p>Pulling a pin removes it and opens a straight path to the grid's exit
 * along its entire row or column. Every character and hazard in that line
 * slides toward the exit, nearest-to-exit first, and compacts against
 * whatever stops it:
 * <ul>
 *   <li>a still-present pin from the other orientation permanently ends that
 *       sub-run - anything behind it just settles in place, unrescued but
 *       not lost, available for a future pull;</li>
 *   <li>a hazard that is nearer the exit than a character "reaches" that
 *       character (and everyone behind it in the same run) before it can
 *       escape - those characters are lost;</li>
 *   <li>characters nearer the exit than any hazard in their run slide
 *       straight off the grid and are rescued.</li>
 * </ul>
 * The algorithm is a pure function of the grid + pin geometry, so identical
 * inputs always produce identical outcomes.
 */
public final class PhysicsResolver {

    private PhysicsResolver() {
    }

    public static PullOutcome applyPull(Grid grid, Pin pin) {
        Pin onGrid = grid.findPin(pin.getOrientation(), pin.getLineIndex());
        if (onGrid == null || !onGrid.getId().equals(pin.getId())) {
            throw new IllegalArgumentException("Pin " + pin.getId() + " is not present on this grid");
        }
        grid.removePin(onGrid);

        List<int[]> order = lineOrder(grid, onGrid);
        List<GridEntity> snapshot = new ArrayList<>(order.size());
        for (int[] cell : order) {
            snapshot.add(grid.occupantAt(cell[0], cell[1]));
        }
        // Lift every movable entity off the line; we re-place survivors deliberately below.
        for (int i = 0; i < order.size(); i++) {
            GridEntity occupant = snapshot.get(i);
            if (occupant instanceof Character || occupant instanceof Hazard) {
                grid.clearCell(order.get(i)[0], order.get(i)[1]);
            }
        }

        List<Character> rescued = new ArrayList<>();
        List<Character> lost = new ArrayList<>();

        int runStart = 0;
        boolean isExitRun = true;
        for (int i = 0; i <= order.size(); i++) {
            boolean runBoundary = (i == order.size()) || (snapshot.get(i) instanceof Pin);
            if (runBoundary) {
                resolveRun(grid, order, snapshot, runStart, i, isExitRun, rescued, lost);
                runStart = i + 1;
                isExitRun = false;
            }
        }
        return new PullOutcome(onGrid, rescued, lost);
    }

    private static void resolveRun(Grid grid, List<int[]> order, List<GridEntity> snapshot,
                                    int fromInclusive, int toExclusive, boolean isExitRun,
                                    List<Character> rescued, List<Character> lost) {
        boolean blockedByHazard = false;
        int cursor = fromInclusive;
        for (int i = fromInclusive; i < toExclusive; i++) {
            GridEntity occupant = snapshot.get(i);
            if (occupant == null) {
                continue;
            }
            if (occupant instanceof Hazard) {
                blockedByHazard = true;
                int[] slot = order.get(cursor++);
                grid.setCell(occupant, slot[0], slot[1]);
            } else if (occupant instanceof Character) {
                Character character = (Character) occupant;
                if (isExitRun && !blockedByHazard) {
                    rescued.add(character);
                    grid.removeCharacter(character);
                } else if (blockedByHazard) {
                    lost.add(character);
                    grid.removeCharacter(character);
                } else {
                    int[] slot = order.get(cursor++);
                    grid.setCell(character, slot[0], slot[1]);
                }
            }
        }
    }

    /** Cell coordinates along a pin's line, ordered nearest-to-exit first. */
    static List<int[]> lineOrder(Grid grid, Pin pin) {
        List<int[]> cells = new ArrayList<>();
        if (pin.getOrientation() == Orientation.ROW) {
            int row = pin.getLineIndex();
            if (pin.getPullDirection() == Direction.RIGHT) {
                for (int col = grid.getWidth() - 1; col >= 0; col--) {
                    cells.add(new int[]{col, row});
                }
            } else {
                for (int col = 0; col < grid.getWidth(); col++) {
                    cells.add(new int[]{col, row});
                }
            }
        } else {
            int col = pin.getLineIndex();
            if (pin.getPullDirection() == Direction.DOWN) {
                for (int row = grid.getHeight() - 1; row >= 0; row--) {
                    cells.add(new int[]{col, row});
                }
            } else {
                for (int row = 0; row < grid.getHeight(); row++) {
                    cells.add(new int[]{col, row});
                }
            }
        }
        return cells;
    }
}
