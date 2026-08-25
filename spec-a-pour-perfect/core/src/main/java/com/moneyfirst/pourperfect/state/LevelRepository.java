package com.moneyfirst.pourperfect.state;

import com.moneyfirst.pourperfect.model.Level;
import com.moneyfirst.pourperfect.model.LevelGenerator;

/**
 * Hands out the {@link Level} the player should currently be playing, derived deterministically
 * from {@link LevelProgressionState#getCurrentLevelIndex()}. Regenerating a level from its index
 * (rather than persisting the full tube layout) keeps save data tiny and makes "current level"
 * trivially reproducible across app reinstalls/devices as long as progression state is restored.
 */
public final class LevelRepository {

    /** Large odd multiplier decorrelates consecutive level seeds; arbitrary but fixed forever. */
    private static final long SEED_MULTIPLIER = 1_000_003L;
    private static final long SEED_OFFSET = 17L;

    private final LevelProgressionState progressionState;

    public LevelRepository(LevelProgressionState progressionState) {
        this.progressionState = progressionState;
    }

    public LevelProgressionState getProgressionState() {
        return progressionState;
    }

    /** The level the player is currently on, generated fresh (deterministically) each call. */
    public Level currentLevel() {
        return levelFor(progressionState.getCurrentLevelIndex());
    }

    public Level levelFor(int levelIndex) {
        return LevelGenerator.generate(levelIndex, seedFor(levelIndex));
    }

    private static long seedFor(int levelIndex) {
        return SEED_MULTIPLIER * levelIndex + SEED_OFFSET;
    }
}
