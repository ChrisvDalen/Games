package com.moneyfirst.pourperfect.state;

import com.moneyfirst.pourperfect.fakes.InMemoryPreferences;
import com.moneyfirst.pourperfect.model.Level;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class LevelRepositoryTest {

    @Test
    void currentLevelMatchesProgressionStatesLevelIndex() {
        LevelProgressionState state = LevelProgressionState.load(new InMemoryPreferences());
        LevelRepository repository = new LevelRepository(state);

        Level level = repository.currentLevel();

        assertEquals(state.getCurrentLevelIndex(), level.getLevelIndex());
    }

    @Test
    void currentLevelAdvancesAfterASolve() {
        LevelProgressionState state = LevelProgressionState.load(new InMemoryPreferences());
        LevelRepository repository = new LevelRepository(state);
        int firstLevelIndex = repository.currentLevel().getLevelIndex();

        state.onLevelSolved(5);

        int secondLevelIndex = repository.currentLevel().getLevelIndex();
        assertEquals(firstLevelIndex + 1, secondLevelIndex);
    }

    @Test
    void sameLevelIndexAlwaysRegeneratesTheIdenticalLevel() {
        LevelProgressionState state = LevelProgressionState.load(new InMemoryPreferences());
        LevelRepository repository = new LevelRepository(state);

        Level first = repository.levelFor(9);
        Level second = repository.levelFor(9);

        assertEquals(first.tubeCount(), second.tubeCount());
        for (int i = 0; i < first.tubeCount(); i++) {
            // Tube has no equals() override (each Tube instance is a distinct mutable object by
            // design - see GameSession's undo snapshots), so compare contents, not references.
            assertEquals(first.getTube(i).getContents(), second.getTube(i).getContents());
        }
        assertFalse(first.isWon());
    }

    @Test
    void differentLevelIndexesUseDifferentSeeds() {
        LevelProgressionState state = LevelProgressionState.load(new InMemoryPreferences());
        LevelRepository repository = new LevelRepository(state);

        Level level9 = repository.levelFor(9);
        Level level10 = repository.levelFor(10);

        assertNotEquals(level9.getSeed(), level10.getSeed());
    }
}
