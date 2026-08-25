package com.moneyfirst.pourperfect.state;

import com.badlogic.gdx.Preferences;
import com.moneyfirst.pourperfect.fakes.InMemoryPreferences;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * All day boundaries here are driven by explicit epoch-day longs rather than the real clock, so
 * these tests are immune to wall-clock flakiness (see {@link StreakTracker} javadoc).
 */
class StreakTrackerTest {

    private static final long DAY_1 = 20_000L;

    @Test
    void firstEverPlayStartsAStreakOfOne() {
        Preferences prefs = new InMemoryPreferences();

        StreakResult result = StreakTracker.recordPlay(prefs, DAY_1);

        assertEquals(1, result.currentStreak());
        assertEquals(1, result.bestStreak());
    }

    @Test
    void consecutiveDaysExtendTheStreak() {
        Preferences prefs = new InMemoryPreferences();

        StreakTracker.recordPlay(prefs, DAY_1);
        StreakTracker.recordPlay(prefs, DAY_1 + 1);
        StreakResult third = StreakTracker.recordPlay(prefs, DAY_1 + 2);

        assertEquals(3, third.currentStreak());
        assertEquals(3, third.bestStreak());
    }

    @Test
    void playingTwiceOnTheSameDayDoesNotDoubleCountTheStreak() {
        Preferences prefs = new InMemoryPreferences();

        StreakTracker.recordPlay(prefs, DAY_1);
        StreakResult sameDayAgain = StreakTracker.recordPlay(prefs, DAY_1);

        assertEquals(1, sameDayAgain.currentStreak());
    }

    @Test
    void skippingADayResetsTheStreakToOne() {
        Preferences prefs = new InMemoryPreferences();

        StreakTracker.recordPlay(prefs, DAY_1);
        StreakTracker.recordPlay(prefs, DAY_1 + 1);
        StreakResult afterGap = StreakTracker.recordPlay(prefs, DAY_1 + 3); // skipped DAY_1 + 2

        assertEquals(1, afterGap.currentStreak());
    }

    @Test
    void bestStreakPersistsAfterAResetToACurrentlyLowerStreak() {
        Preferences prefs = new InMemoryPreferences();

        StreakTracker.recordPlay(prefs, DAY_1);
        StreakTracker.recordPlay(prefs, DAY_1 + 1);
        StreakTracker.recordPlay(prefs, DAY_1 + 2); // streak of 3, best = 3

        StreakResult afterBreak = StreakTracker.recordPlay(prefs, DAY_1 + 10); // big gap -> resets to 1

        assertEquals(1, afterBreak.currentStreak());
        assertEquals(3, afterBreak.bestStreak(), "best streak must not regress");
    }

    @Test
    void playingBeforeTheLastRecordedDayStillResetsToOne() {
        // Defensive case: a clock rollback (or, more realistically, a device timezone change
        // that shifts the UTC epoch-day backwards by one) must not be treated as "consecutive".
        Preferences prefs = new InMemoryPreferences();

        StreakTracker.recordPlay(prefs, DAY_1 + 5);
        StreakResult earlierDay = StreakTracker.recordPlay(prefs, DAY_1);

        assertEquals(1, earlierDay.currentStreak());
    }

    @Test
    void queryHelpersReflectPersistedState() {
        Preferences prefs = new InMemoryPreferences();
        StreakTracker.recordPlay(prefs, DAY_1);
        StreakTracker.recordPlay(prefs, DAY_1 + 1);

        assertEquals(2, StreakTracker.getCurrentStreak(prefs));
        assertEquals(2, StreakTracker.getBestStreak(prefs));
        assertEquals(DAY_1 + 1, StreakTracker.getLastPlayedEpochDay(prefs));
    }

    @Test
    void todayUtcReturnsAPlausibleEpochDay() {
        // Sanity check only - not a determinism test. Epoch day for any date from 2024 onward is
        // comfortably above 19700 (1970-01-01 = day 0); this just guards against an obviously
        // broken implementation (e.g. returning 0 or a negative number).
        long today = StreakTracker.todayUtc();
        assertTrue(today > 19_000L);
    }
}
