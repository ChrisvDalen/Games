package com.moneyfirst.towerperil.rescue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PinPullLevelGeneratorTest {

    @ParameterizedTest
    @ValueSource(longs = {0L, 1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L,
            10L, 11L, 12L, 13L, 14L, 15L, 16L, 17L, 18L, 19L})
    void generatedLevelIsAlwaysSolvableAcrossTwentySeeds(long seed) {
        PinPullLevel level = PinPullLevelGenerator.generate(seed);

        assertTrue(PinPullLevelGenerator.isSolvable(level.getGrid()),
                "Level generated from seed " + seed + " must be solvable");
        assertTrue(level.getTotalCharacters() > 0, "A generated level must contain at least one character");

        Optional<List<Pin>> solution = PinPullLevelGenerator.findSolution(level.getGrid());
        assertTrue(solution.isPresent(), "A concrete winning pull order must exist for seed " + seed);

        // Actually replay the discovered solution end-to-end and confirm zero losses / full rescue.
        Grid replay = level.getGrid().copy();
        int totalRescued = 0;
        for (Pin pin : solution.get()) {
            Pin onReplay = replay.findPin(pin.getOrientation(), pin.getLineIndex());
            PullOutcome outcome = PhysicsResolver.applyPull(replay, onReplay);
            assertTrue(outcome.getLost().isEmpty(), "Solution must never lose a character (seed " + seed + ")");
            totalRescued += outcome.getRescued().size();
        }
        assertEquals(level.getTotalCharacters(), totalRescued,
                "Every character must be rescued by the discovered solution (seed " + seed + ")");
    }

    @Test
    void generationIsDeterministicForAGivenSeed() {
        PinPullLevel a = PinPullLevelGenerator.generate(42L);
        PinPullLevel b = PinPullLevelGenerator.generate(42L);

        assertEquals(describe(a.getGrid()), describe(b.getGrid()));
    }

    @Test
    void differentSeedsTypicallyProduceDifferentLevels() {
        PinPullLevel a = PinPullLevelGenerator.generate(1L);
        PinPullLevel b = PinPullLevelGenerator.generate(2L);

        assertFalse(describe(a.getGrid()).equals(describe(b.getGrid())),
                "Different seeds should (overwhelmingly likely) produce different layouts");
    }

    @Test
    void levelWithNoCharactersIsNeverConsideredSolvable() {
        Grid grid = new Grid(4, 4);
        assertFalse(PinPullLevelGenerator.isSolvable(grid));
        assertFalse(PinPullLevelGenerator.findSolution(grid).isPresent());
    }

    /** Deterministic, order-stable textual snapshot of a grid's contents for equality checks. */
    private static String describe(Grid grid) {
        StringBuilder sb = new StringBuilder();
        for (Pin pin : grid.getPins()) {
            sb.append("P:").append(pin.getOrientation()).append(pin.getLineIndex())
                    .append(pin.getPullDirection()).append('@').append(pin.getCol()).append(',').append(pin.getRow()).append(';');
        }
        for (Character c : grid.getCharacters()) {
            sb.append("C:").append(c.getType()).append('@').append(c.getCol()).append(',').append(c.getRow()).append(';');
        }
        for (Hazard h : grid.getHazards()) {
            sb.append("H:@").append(h.getCol()).append(',').append(h.getRow()).append(';');
        }
        return sb.toString();
    }
}
