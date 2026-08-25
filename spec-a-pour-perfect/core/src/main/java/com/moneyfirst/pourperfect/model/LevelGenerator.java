package com.moneyfirst.pourperfect.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Builds solvable {@link Level}s. Deterministic given a seed: the same {@code (levelIndex, seed)}
 * pair always produces the exact same puzzle.
 *
 * <p><strong>Algorithm.</strong> The "solved" state - one tube fully filled with each color, plus
 * some number of empty tubes - defines the multiset of liquid units the puzzle must contain
 * (exactly {@code tubeCapacity} units of each of the {@code numColors} colors, and nothing else).
 * A candidate puzzle is built by shuffling that whole multiset with the seeded RNG and dealing it
 * back out into {@code numColors} tubes of {@code tubeCapacity} units each (plus the empty
 * tubes) - i.e. every candidate is, by construction, some random rearrangement of exactly the
 * solved state's contents, so it always uses the right amount of every color.
 *
 * <p>A naive alternative - starting from the solved state and applying a walk of randomly chosen
 * <em>legal pours</em> to scramble it - does not work under this game's all-or-nothing pour rule
 * (a pour always moves a source tube's <em>entire</em> top run): starting from the solved state,
 * every non-empty tube is a distinct pure color, so every legal pour target is necessarily either
 * empty or a matching pure-color tube, meaning such a walk can only ever relocate whole pure
 * colors between tube slots - it can never interleave two colors into the same tube, so it can
 * never produce a genuinely mixed, playable puzzle. Dealing from the shuffled multiset instead
 * sidesteps that limitation entirely by constructing the arrangement directly.
 *
 * <p>Not every shuffle is solvable (or even non-trivial - a shuffle can land back on an
 * already-won arrangement by chance), so every candidate is verified with {@link PuzzleSolver}
 * before being accepted; a losing candidate is silently discarded and regenerated from the same
 * deterministic random stream. This makes solvability a guarantee of the output, not a property
 * assumed of the shuffling step.
 */
public final class LevelGenerator {

    private static final int MAX_GENERATION_ATTEMPTS = 500;
    private static final int SOLVER_STATE_BUDGET = 60_000;

    private LevelGenerator() {
    }

    /** Difficulty curve: more colors and fewer spare empty tubes as the player advances. */
    public static GenerationConfig configFor(int levelIndex) {
        int index = Math.max(1, levelIndex);
        int numColors = Math.min(4 + (index - 1) / 4, LiquidColor.values().length);
        int numEmptyTubes = index < 15 ? 2 : 1;
        int tubeCapacity = 4;
        return new GenerationConfig(numColors, tubeCapacity, numEmptyTubes);
    }

    /** Generates the level for {@code levelIndex} using the difficulty curve from {@link #configFor}. */
    public static Level generate(int levelIndex, long seed) {
        return generate(levelIndex, seed, configFor(levelIndex));
    }

    public static Level generate(int levelIndex, long seed, GenerationConfig config) {
        Random rng = new Random(seed);

        for (int attempt = 0; attempt < MAX_GENERATION_ATTEMPTS; attempt++) {
            List<Tube> candidate = dealShuffledCandidate(config, rng);

            if (isAlreadyWon(candidate)) {
                continue;
            }
            if (PuzzleSolver.isSolvable(candidate, SOLVER_STATE_BUDGET)) {
                return new Level(seed, levelIndex, candidate);
            }
            // Unlucky shuffle produced an unsolvable board; loop and try again. The rng instance
            // is shared across attempts so the whole process stays deterministic for a given
            // (levelIndex, seed).
        }

        // Practically unreachable for sane configs (small color counts, capacity 4, >=1 empty
        // tube) - random deals verified by BFS converge within a handful of attempts in practice.
        // Fail loudly rather than silently returning an unfair/degenerate puzzle.
        throw new IllegalStateException(
                "LevelGenerator could not produce a solvable, non-trivial level for levelIndex="
                        + levelIndex + " seed=" + seed + " after " + MAX_GENERATION_ATTEMPTS + " attempts");
    }

    /** Shuffles exactly {@code numColors * tubeCapacity} liquid units and deals them into fresh tubes. */
    private static List<Tube> dealShuffledCandidate(GenerationConfig config, Random rng) {
        LiquidColor[] palette = LiquidColor.values();
        int totalUnits = config.numColors() * config.tubeCapacity();

        List<LiquidColor> units = new ArrayList<>(totalUnits);
        for (int c = 0; c < config.numColors(); c++) {
            for (int unit = 0; unit < config.tubeCapacity(); unit++) {
                units.add(palette[c]);
            }
        }
        shuffle(units, rng);

        List<Tube> tubes = new ArrayList<>(config.numColors() + config.numEmptyTubes());
        int cursor = 0;
        for (int t = 0; t < config.numColors(); t++) {
            LiquidColor[] fill = new LiquidColor[config.tubeCapacity()];
            for (int slot = 0; slot < config.tubeCapacity(); slot++) {
                fill[slot] = units.get(cursor++);
            }
            tubes.add(Tube.of(config.tubeCapacity(), fill));
        }
        for (int e = 0; e < config.numEmptyTubes(); e++) {
            tubes.add(new Tube(config.tubeCapacity()));
        }
        return tubes;
    }

    /** Fisher-Yates shuffle, driven entirely by the passed-in seeded {@link Random}. */
    private static void shuffle(List<LiquidColor> units, Random rng) {
        for (int i = units.size() - 1; i > 0; i--) {
            int j = rng.nextInt(i + 1);
            LiquidColor tmp = units.get(i);
            units.set(i, units.get(j));
            units.set(j, tmp);
        }
    }

    private static boolean isAlreadyWon(List<Tube> tubes) {
        for (Tube tube : tubes) {
            if (!tube.isSolvedOrEmpty()) {
                return false;
            }
        }
        return true;
    }

    /** Immutable generation parameters for one difficulty tier. */
    public record GenerationConfig(int numColors, int tubeCapacity, int numEmptyTubes) {
        public GenerationConfig {
            if (numColors < 2 || numColors > LiquidColor.values().length) {
                throw new IllegalArgumentException("numColors must be between 2 and " + LiquidColor.values().length);
            }
            if (tubeCapacity < 2) {
                throw new IllegalArgumentException("tubeCapacity must be >= 2");
            }
            if (numEmptyTubes < 1) {
                throw new IllegalArgumentException("numEmptyTubes must be >= 1 to keep the puzzle solvable");
            }
        }
    }
}
