package com.moneyfirst.pourperfect.state;

import com.badlogic.gdx.Preferences;

import java.time.LocalDate;
import java.time.ZoneOffset;

/**
 * Daily-puzzle streak tracking, keyed entirely on epoch-day integers rather than wall-clock time
 * deltas - comparing {@code today - lastPlayed} in days is immune to clock skew, DST changes and
 * the exact time of day the player opens the app, which a duration-based "played within 24h"
 * check would not be. Callers pass in {@code todayEpochDay} explicitly (see {@link #todayUtc()}
 * for the production helper) so tests can drive day boundaries deterministically without any
 * dependency on the real clock.
 */
public final class StreakTracker {

    static final String KEY_LAST_PLAYED_EPOCH_DAY = "pp_streak_last_played_epoch_day";
    static final String KEY_CURRENT_STREAK = "pp_streak_current";
    static final String KEY_BEST_STREAK = "pp_streak_best";

    private static final long NEVER_PLAYED = Long.MIN_VALUE;

    private StreakTracker() {
    }

    /** Today's date as an epoch-day, in UTC, so the streak boundary doesn't depend on device timezone. */
    public static long todayUtc() {
        return LocalDate.now(ZoneOffset.UTC).toEpochDay();
    }

    /**
     * Records that the player played on {@code todayEpochDay}, updating (and persisting) the
     * streak:
     * <ul>
     *   <li>same day as last recorded play -&gt; streak unchanged (already counted today);</li>
     *   <li>exactly one day after the last recorded play -&gt; streak extends by one;</li>
     *   <li>any other gap (including the very first play ever) -&gt; streak resets to one.</li>
     * </ul>
     */
    public static StreakResult recordPlay(Preferences preferences, long todayEpochDay) {
        long lastPlayed = preferences.getLong(KEY_LAST_PLAYED_EPOCH_DAY, NEVER_PLAYED);
        int currentStreak = preferences.getInteger(KEY_CURRENT_STREAK, 0);
        int bestStreak = preferences.getInteger(KEY_BEST_STREAK, 0);

        if (lastPlayed == todayEpochDay) {
            // Already recorded today; leave the streak as-is.
        } else if (lastPlayed == todayEpochDay - 1) {
            currentStreak++;
        } else {
            currentStreak = 1;
        }
        bestStreak = Math.max(bestStreak, currentStreak);

        preferences.putLong(KEY_LAST_PLAYED_EPOCH_DAY, todayEpochDay);
        preferences.putInteger(KEY_CURRENT_STREAK, currentStreak);
        preferences.putInteger(KEY_BEST_STREAK, bestStreak);
        preferences.flush();

        return new StreakResult(currentStreak, bestStreak);
    }

    public static int getCurrentStreak(Preferences preferences) {
        return preferences.getInteger(KEY_CURRENT_STREAK, 0);
    }

    public static int getBestStreak(Preferences preferences) {
        return preferences.getInteger(KEY_BEST_STREAK, 0);
    }

    public static long getLastPlayedEpochDay(Preferences preferences) {
        return preferences.getLong(KEY_LAST_PLAYED_EPOCH_DAY, NEVER_PLAYED);
    }
}
