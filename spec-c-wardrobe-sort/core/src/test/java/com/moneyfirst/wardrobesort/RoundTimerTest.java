package com.moneyfirst.wardrobesort;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RoundTimerTest {

    @Test
    void countsDownAndClampsAtZero() {
        RoundTimer timer = new RoundTimer(10f);
        timer.update(4f);
        assertEquals(6f, timer.remainingSeconds(), 0.0001f);
        assertFalse(timer.isExpired());

        timer.update(100f);
        assertEquals(0f, timer.remainingSeconds(), 0.0001f);
        assertTrue(timer.isExpired());
    }

    @Test
    void firstExtensionSucceedsAndAddsTenSeconds() {
        RoundTimer timer = new RoundTimer(10f);
        timer.update(8f); // remaining = 2

        boolean applied = timer.extend();

        assertTrue(applied);
        assertEquals(12f, timer.remainingSeconds(), 0.0001f);
        assertTrue(timer.isExtensionUsed());
        assertFalse(timer.isExtensionAvailable());
    }

    @Test
    void secondExtensionAttemptIsRefusedAndChangesNothing() {
        RoundTimer timer = new RoundTimer(10f);
        assertTrue(timer.extend());
        float afterFirst = timer.remainingSeconds();

        boolean secondApplied = timer.extend();

        assertFalse(secondApplied, "a second extension in the same round must be refused");
        assertEquals(afterFirst, timer.remainingSeconds(), 0.0001f, "remaining time must not change on a refused extension");
    }

    @Test
    void extensionCanRescueAnAlreadyExpiredTimer() {
        RoundTimer timer = new RoundTimer(5f);
        timer.update(5f);
        assertTrue(timer.isExpired());

        assertTrue(timer.extend());

        assertFalse(timer.isExpired(), "the one-time extension should be able to bring an expired timer back to life");
        assertEquals(10f, timer.remainingSeconds(), 0.0001f);
    }

    @Test
    void rejectsNonPositiveTotalAndNegativeDelta() {
        assertThrows(IllegalArgumentException.class, () -> new RoundTimer(0f));
        assertThrows(IllegalArgumentException.class, () -> new RoundTimer(-1f));
        RoundTimer timer = new RoundTimer(5f);
        assertThrows(IllegalArgumentException.class, () -> timer.update(-1f));
    }
}
