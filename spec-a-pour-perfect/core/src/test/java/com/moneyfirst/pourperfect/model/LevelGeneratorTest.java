package com.moneyfirst.pourperfect.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LevelGeneratorTest {

    private static final int SOLVER_BUDGET = 200_000;

    /**
     * The core solvability guarantee: for 20 different random seeds, the generated level must be
     * solvable from its scrambled starting position, verified with an independent BFS search over
     * the exact same {@link Tube}/pour rules a player uses. This exercises {@link LevelGenerator}
     * end to end (scramble + internal solver-gated retry), then re-verifies with a fresh solver
     * call so the assertion doesn't just trust the generator's own internal check.
     */
    @ParameterizedTest
    @ValueSource(longs = {
            1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L,
            11L, 12L, 13L, 14L, 15L, 16L, 17L, 18L, 19L, 20L
    })
    void generatedLevelIsAlwaysSolvable(long seed) {
        Level level = LevelGenerator.generate(5, seed);

        assertFalse(level.isWon(), "a freshly generated puzzle should not already be solved");

        List<Tube> tubesCopy = level.getTubes().stream().map(Tube::copy).toList();
        boolean solvable = PuzzleSolver.isSolvable(tubesCopy, SOLVER_BUDGET);

        assertTrue(solvable, "seed " + seed + " produced an unsolvable level");
    }

    @Test
    void sameLevelIndexAndSeedProduceIdenticalLevels() {
        Level a = LevelGenerator.generate(7, 42L);
        Level b = LevelGenerator.generate(7, 42L);

        assertEquals(a.tubeCount(), b.tubeCount());
        for (int i = 0; i < a.tubeCount(); i++) {
            assertEquals(a.getTube(i).getContents(), b.getTube(i).getContents());
        }
    }

    @Test
    void differentSeedsProduceDifferentLevelsUsually() {
        Level a = LevelGenerator.generate(7, 42L);
        Level b = LevelGenerator.generate(7, 4242L);

        boolean anyTubeDiffers = false;
        for (int i = 0; i < a.tubeCount(); i++) {
            if (!a.getTube(i).getContents().equals(b.getTube(i).getContents())) {
                anyTubeDiffers = true;
                break;
            }
        }
        assertTrue(anyTubeDiffers, "two very different seeds produced byte-identical levels");
    }

    @Test
    void generatedLevelUsesExactlyCapacityUnitsOfEachColor() {
        LevelGenerator.GenerationConfig config = new LevelGenerator.GenerationConfig(5, 4, 2);
        Level level = LevelGenerator.generate(3, 99L, config);

        int[] countPerColor = new int[LiquidColor.values().length];
        int totalUnits = 0;
        for (Tube tube : level.getTubes()) {
            for (LiquidColor color : tube.getContents()) {
                countPerColor[color.ordinal()]++;
                totalUnits++;
            }
        }
        assertEquals(config.numColors() * config.tubeCapacity(), totalUnits);
        for (int c = 0; c < config.numColors(); c++) {
            assertEquals(config.tubeCapacity(), countPerColor[c],
                    "color " + LiquidColor.values()[c] + " should appear exactly tubeCapacity times");
        }
    }

    @Test
    void difficultyCurveIncreasesColorsAsLevelIndexGrows() {
        LevelGenerator.GenerationConfig early = LevelGenerator.configFor(1);
        LevelGenerator.GenerationConfig later = LevelGenerator.configFor(40);

        assertTrue(later.numColors() >= early.numColors());
        assertTrue(later.numColors() <= LiquidColor.values().length);
    }

    @Test
    void generatedTubeCountMatchesColorsPlusEmptyTubes() {
        LevelGenerator.GenerationConfig config = LevelGenerator.configFor(1);
        Level level = LevelGenerator.generate(1, 5L, config);

        assertEquals(config.numColors() + config.numEmptyTubes(), level.tubeCount());
    }

    @Test
    void solverFindsAWorkingSolutionThatActuallyWinsWhenReplayed() {
        Level level = LevelGenerator.generate(2, 123L);
        List<Tube> tubes = level.getTubes().stream().map(Tube::copy).toList();

        var solution = PuzzleSolver.solve(tubes, SOLVER_BUDGET);
        assertTrue(solution.isPresent());

        for (Move move : solution.get()) {
            Tube from = tubes.get(move.from());
            Tube to = tubes.get(move.to());
            assertTrue(from.canPourInto(to), "solver returned an illegal move: " + move);
            from.pourInto(to);
        }

        for (Tube tube : tubes) {
            assertTrue(tube.isSolvedOrEmpty(), "replaying the solver's own solution did not win");
        }
    }

    @Test
    void generationConfigRejectsInvalidParameters() {
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> new LevelGenerator.GenerationConfig(1, 4, 2));
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> new LevelGenerator.GenerationConfig(5, 1, 2));
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> new LevelGenerator.GenerationConfig(5, 4, 0));
    }

    @Test
    void allGeneratedColorsAreDistinctAcrossFullTubes() {
        LevelGenerator.GenerationConfig config = LevelGenerator.configFor(20);
        Level level = LevelGenerator.generate(20, 777L, config);

        Set<LiquidColor> seen = new java.util.HashSet<>();
        for (Tube tube : level.getTubes()) {
            seen.addAll(tube.getContents());
        }
        assertEquals(config.numColors(), seen.size());
    }
}
