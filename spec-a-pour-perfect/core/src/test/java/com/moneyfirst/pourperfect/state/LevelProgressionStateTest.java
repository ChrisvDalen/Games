package com.moneyfirst.pourperfect.state;

import com.badlogic.gdx.Preferences;
import com.moneyfirst.pourperfect.fakes.InMemoryPreferences;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LevelProgressionStateTest {

    @Test
    void freshStateHasSensibleDefaults() {
        LevelProgressionState state = LevelProgressionState.load(new InMemoryPreferences());

        assertEquals(1, state.getCurrentLevelIndex());
        assertEquals(0, state.getSolvedCount());
        assertTrue(state.getHintsRemaining() > 0);
        assertTrue(state.getUndosRemaining() > 0);
        assertEquals(0, state.getCups());
        assertFalse(state.isAdsRemoved());
        assertFalse(state.isCafeUnlocked());
    }

    @Test
    void onLevelSolvedAdvancesLevelBumpsSolvedCountAndAwardsCups() {
        LevelProgressionState state = LevelProgressionState.load(new InMemoryPreferences());
        int startingLevel = state.getCurrentLevelIndex();

        state.onLevelSolved(5);

        assertEquals(startingLevel + 1, state.getCurrentLevelIndex());
        assertEquals(1, state.getSolvedCount());
        assertEquals(5, state.getCups());
    }

    @Test
    void saveThenLoadRoundTripsEveryField() {
        Preferences prefs = new InMemoryPreferences();
        LevelProgressionState state = LevelProgressionState.load(prefs);
        state.onLevelSolved(10);
        state.onLevelSolved(10);
        state.grantHints(4);
        state.grantUndos(2);
        state.setAdsRemoved(true);
        state.unlockCafe();
        state.save();

        LevelProgressionState reloaded = LevelProgressionState.load(prefs);

        assertEquals(state.getCurrentLevelIndex(), reloaded.getCurrentLevelIndex());
        assertEquals(state.getSolvedCount(), reloaded.getSolvedCount());
        assertEquals(state.getHintsRemaining(), reloaded.getHintsRemaining());
        assertEquals(state.getUndosRemaining(), reloaded.getUndosRemaining());
        assertEquals(state.getCups(), reloaded.getCups());
        assertTrue(reloaded.isAdsRemoved());
        assertTrue(reloaded.isCafeUnlocked());
    }

    @Test
    void consumeHintNeverGoesNegative() {
        LevelProgressionState state = LevelProgressionState.load(new InMemoryPreferences());
        int startingHints = state.getHintsRemaining();

        for (int i = 0; i < startingHints; i++) {
            assertTrue(state.consumeHint());
        }
        assertEquals(0, state.getHintsRemaining());

        assertFalse(state.consumeHint(), "consuming beyond zero must fail, not go negative");
        assertEquals(0, state.getHintsRemaining());
    }

    @Test
    void consumeUndoNeverGoesNegative() {
        LevelProgressionState state = LevelProgressionState.load(new InMemoryPreferences());
        int startingUndos = state.getUndosRemaining();

        for (int i = 0; i < startingUndos; i++) {
            assertTrue(state.consumeUndo());
        }
        assertFalse(state.consumeUndo());
        assertEquals(0, state.getUndosRemaining());
    }

    @Test
    void grantingNegativeAmountsNeverReducesCredits() {
        LevelProgressionState state = LevelProgressionState.load(new InMemoryPreferences());
        int hintsBefore = state.getHintsRemaining();
        int undosBefore = state.getUndosRemaining();

        state.grantHints(-50);
        state.grantUndos(-50);

        assertEquals(hintsBefore, state.getHintsRemaining());
        assertEquals(undosBefore, state.getUndosRemaining());
    }

    @Test
    void spendCupsFailsWithoutSufficientBalanceAndLeavesBalanceUnchanged() {
        LevelProgressionState state = LevelProgressionState.load(new InMemoryPreferences());
        state.onLevelSolved(10); // cups = 10

        assertFalse(state.spendCups(11));
        assertEquals(10, state.getCups());

        assertTrue(state.spendCups(10));
        assertEquals(0, state.getCups());
    }

    @Test
    void spendCupsRejectsNegativeAmounts() {
        LevelProgressionState state = LevelProgressionState.load(new InMemoryPreferences());
        state.onLevelSolved(10);

        assertFalse(state.spendCups(-1));
        assertEquals(10, state.getCups());
    }
}
