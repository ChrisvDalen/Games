package com.moneyfirst.pourperfect.state;

import com.badlogic.gdx.Preferences;

/**
 * The player's persistent progress: which level they're on, how many they've solved, remaining
 * hint/undo credits, cafe-currency ("cups") balance, and the two boolean unlocks (ads removed,
 * cafe unlocked). Backed by a libGDX {@link Preferences} instance, which on Android is a
 * SharedPreferences-backed implementation and on iOS is RoboVM's NSUserDefaults-backed
 * implementation - {@code core} depends only on the {@code Preferences} interface, never a
 * platform class, so this is fully unit-testable with any in-memory {@link Preferences} fake.
 *
 * <p>Every counter here is guarded to never go negative, mirroring {@code GameSession}'s
 * in-session guards - the two layers use the same discipline independently.
 */
public final class LevelProgressionState {

    static final String KEY_CURRENT_LEVEL_INDEX = "pp_current_level_index";
    static final String KEY_SOLVED_COUNT = "pp_solved_count";
    static final String KEY_HINTS_REMAINING = "pp_hints_remaining";
    static final String KEY_UNDOS_REMAINING = "pp_undos_remaining";
    static final String KEY_CUPS = "pp_cups";
    static final String KEY_ADS_REMOVED = "pp_ads_removed";
    static final String KEY_CAFE_UNLOCKED = "pp_cafe_unlocked";

    private static final int STARTING_HINTS = 3;
    private static final int STARTING_UNDOS = 3;
    private static final int FIRST_LEVEL_INDEX = 1;

    private final Preferences preferences;

    private int currentLevelIndex;
    private int solvedCount;
    private int hintsRemaining;
    private int undosRemaining;
    private int cups;
    private boolean adsRemoved;
    private boolean cafeUnlocked;

    private LevelProgressionState(Preferences preferences) {
        this.preferences = preferences;
    }

    /** Loads progression from {@code preferences}, defaulting to a brand-new player's state. */
    public static LevelProgressionState load(Preferences preferences) {
        LevelProgressionState state = new LevelProgressionState(preferences);
        state.currentLevelIndex = preferences.getInteger(KEY_CURRENT_LEVEL_INDEX, FIRST_LEVEL_INDEX);
        state.solvedCount = preferences.getInteger(KEY_SOLVED_COUNT, 0);
        state.hintsRemaining = preferences.getInteger(KEY_HINTS_REMAINING, STARTING_HINTS);
        state.undosRemaining = preferences.getInteger(KEY_UNDOS_REMAINING, STARTING_UNDOS);
        state.cups = preferences.getInteger(KEY_CUPS, 0);
        state.adsRemoved = preferences.getBoolean(KEY_ADS_REMOVED, false);
        state.cafeUnlocked = preferences.getBoolean(KEY_CAFE_UNLOCKED, false);
        return state;
    }

    /** Persists all fields and flushes to disk. */
    public void save() {
        preferences.putInteger(KEY_CURRENT_LEVEL_INDEX, currentLevelIndex);
        preferences.putInteger(KEY_SOLVED_COUNT, solvedCount);
        preferences.putInteger(KEY_HINTS_REMAINING, hintsRemaining);
        preferences.putInteger(KEY_UNDOS_REMAINING, undosRemaining);
        preferences.putInteger(KEY_CUPS, cups);
        preferences.putBoolean(KEY_ADS_REMOVED, adsRemoved);
        preferences.putBoolean(KEY_CAFE_UNLOCKED, cafeUnlocked);
        preferences.flush();
    }

    public int getCurrentLevelIndex() {
        return currentLevelIndex;
    }

    public int getSolvedCount() {
        return solvedCount;
    }

    public int getHintsRemaining() {
        return hintsRemaining;
    }

    public int getUndosRemaining() {
        return undosRemaining;
    }

    public int getCups() {
        return cups;
    }

    public boolean isAdsRemoved() {
        return adsRemoved;
    }

    public boolean isCafeUnlocked() {
        return cafeUnlocked;
    }

    /** Records a level solve: advances to the next level, bumps solved count, awards cups. */
    public void onLevelSolved(int cupsEarned) {
        solvedCount++;
        currentLevelIndex++;
        cups += Math.max(0, cupsEarned);
    }

    public void grantHints(int amount) {
        hintsRemaining += Math.max(0, amount);
    }

    public void grantUndos(int amount) {
        undosRemaining += Math.max(0, amount);
    }

    /** @return true if a hint credit was consumed; false if none were available (count untouched). */
    public boolean consumeHint() {
        if (hintsRemaining <= 0) {
            return false;
        }
        hintsRemaining--;
        return true;
    }

    /** @return true if an undo credit was consumed; false if none were available (count untouched). */
    public boolean consumeUndo() {
        if (undosRemaining <= 0) {
            return false;
        }
        undosRemaining--;
        return true;
    }

    /** @return true if the spend succeeded (sufficient balance); false and no change otherwise. */
    public boolean spendCups(int amount) {
        if (amount < 0 || cups < amount) {
            return false;
        }
        cups -= amount;
        return true;
    }

    public void setAdsRemoved(boolean adsRemoved) {
        this.adsRemoved = adsRemoved;
    }

    public void unlockCafe() {
        this.cafeUnlocked = true;
    }
}
