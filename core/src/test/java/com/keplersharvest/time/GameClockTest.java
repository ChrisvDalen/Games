package com.keplersharvest.time;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameClockTest {

    private static final float SECONDS_PER_MINUTE = 0.5f;
    private static final int WAKE = 6 * 60;

    @Test
    @DisplayName("real seconds convert to in-game minutes at the configured rate")
    void realTimeDrivesTheClock() {
        GameClock clock = new GameClock(SECONDS_PER_MINUTE, WAKE);

        clock.advance(5f);

        assertEquals(WAKE + 10, clock.now().minuteOfDay(), "5 seconds at 0.5s per minute is 10 minutes");
        assertEquals(1, clock.day());
    }

    @Test
    @DisplayName("leftover fractions of a minute carry into the next update")
    void partialMinutesCarryOver() {
        GameClock clock = new GameClock(SECONDS_PER_MINUTE, WAKE);

        for (int i = 0; i < 4; i++) {
            clock.advance(0.3f);
        }

        assertEquals(WAKE + 2, clock.now().minuteOfDay(), "1.2 seconds of accumulation is 2 whole minutes");
    }

    @Test
    @DisplayName("phases follow the time of day")
    void phasesFollowTheClock() {
        assertEquals(DayPhase.NIGHT, DayPhase.at(3 * 60));
        assertEquals(DayPhase.MORNING, DayPhase.at(6 * 60));
        assertEquals(DayPhase.AFTERNOON, DayPhase.at(13 * 60));
        assertEquals(DayPhase.EVENING, DayPhase.at(19 * 60));
        assertEquals(DayPhase.NIGHT, DayPhase.at(23 * 60));
    }

    @Test
    @DisplayName("crossing midnight increments the day exactly once")
    void midnightRollsTheDay() {
        GameClock clock = new GameClock(SECONDS_PER_MINUTE, WAKE);
        List<Integer> daysStarted = new ArrayList<>();
        clock.addListener(new ClockListener() {
            @Override
            public void onDayStarted(int day) {
                daysStarted.add(day);
            }
        });

        clock.advanceMinutes(18 * 60);

        assertEquals(List.of(2), daysStarted);
        assertEquals(2, clock.day());
        assertEquals(0, clock.now().minuteOfDay());
    }

    @Test
    @DisplayName("sleeping jumps to the next morning")
    void sleepingAdvancesToWakeTime() {
        GameClock clock = new GameClock(SECONDS_PER_MINUTE, WAKE);
        clock.advanceMinutes(14 * 60);

        TimeOfDay morning = clock.sleepUntilMorning();

        assertEquals(2, morning.day());
        assertEquals(WAKE, morning.minuteOfDay());
        assertEquals(DayPhase.MORNING, clock.phase());
    }

    @Test
    @DisplayName("sleeping after midnight still lands on the same calendar day's morning")
    void sleepingAfterMidnight() {
        GameClock clock = new GameClock(SECONDS_PER_MINUTE, WAKE);
        clock.advanceMinutes(19 * 60);
        assertEquals(2, clock.day());
        assertEquals(60, clock.now().minuteOfDay(), "01:00 on day 2");

        clock.sleepUntilMorning();

        assertEquals(3, clock.day(), "going to bed at 01:00 still costs you the night");
        assertEquals(WAKE, clock.now().minuteOfDay());
    }

    @Test
    @DisplayName("a paused clock ignores elapsed time")
    void pausingStopsTime() {
        GameClock clock = new GameClock(SECONDS_PER_MINUTE, WAKE);
        clock.setPaused(true);

        clock.advance(30f);

        assertEquals(WAKE, clock.now().minuteOfDay());

        clock.setPaused(false);
        clock.advance(1f);
        assertEquals(WAKE + 2, clock.now().minuteOfDay());
    }

    @Test
    @DisplayName("listeners see one minute event per elapsed minute")
    void minuteEventsAreNotDropped() {
        GameClock clock = new GameClock(SECONDS_PER_MINUTE, WAKE);
        int[] ticks = {0};
        clock.addListener(new ClockListener() {
            @Override
            public void onMinute(TimeOfDay now) {
                ticks[0]++;
            }
        });

        clock.advance(10f);

        assertEquals(20, ticks[0]);
    }

    @Test
    @DisplayName("time only moves forwards")
    void timeIsMonotonic() {
        GameClock clock = new GameClock(SECONDS_PER_MINUTE, WAKE);
        TimeOfDay before = clock.now();

        clock.advance(-5f);

        assertEquals(before, clock.now());
        assertTrue(clock.minutesUntilMidnight() > 0);
    }
}
