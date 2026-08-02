package com.keplersharvest.time;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Drives colony time from real elapsed seconds.
 *
 * <p>Day length is configuration, not a constant: {@code realSecondsPerGameMinute} scales the whole
 * clock. The clock is pure logic - it never reads input or renders - so time-dependent rules can be
 * tested by calling {@link #advance} with fixed deltas.
 */
public final class GameClock {

    private final float realSecondsPerGameMinute;
    private final int wakeMinute;
    private final List<ClockListener> listeners = new ArrayList<>();

    private TimeOfDay now;
    private DayPhase phase;
    private float carriedSeconds;
    private boolean paused;

    public GameClock(float realSecondsPerGameMinute, int wakeMinute) {
        this(realSecondsPerGameMinute, wakeMinute, new TimeOfDay(1, wakeMinute));
    }

    public GameClock(float realSecondsPerGameMinute, int wakeMinute, TimeOfDay start) {
        if (realSecondsPerGameMinute <= 0f) {
            throw new IllegalArgumentException("realSecondsPerGameMinute must be positive");
        }
        this.realSecondsPerGameMinute = realSecondsPerGameMinute;
        this.wakeMinute = Math.floorMod(wakeMinute, TimeOfDay.MINUTES_PER_DAY);
        this.now = Objects.requireNonNull(start, "start");
        this.phase = start.phase();
    }

    public TimeOfDay now() {
        return now;
    }

    public int day() {
        return now.day();
    }

    public DayPhase phase() {
        return phase;
    }

    public boolean paused() {
        return paused;
    }

    /** Menus pause time by flipping this; {@link #advance} then does nothing. */
    public void setPaused(boolean paused) {
        this.paused = paused;
        if (paused) {
            carriedSeconds = 0f;
        }
    }

    public void addListener(ClockListener listener) {
        listeners.add(Objects.requireNonNull(listener, "listener"));
    }

    public void removeListener(ClockListener listener) {
        listeners.remove(listener);
    }

    /** Feeds real elapsed seconds into the clock, emitting one event per in-game minute. */
    public void advance(float deltaSeconds) {
        if (paused || deltaSeconds <= 0f) {
            return;
        }
        carriedSeconds += deltaSeconds;
        int minutes = (int) (carriedSeconds / realSecondsPerGameMinute);
        if (minutes <= 0) {
            return;
        }
        carriedSeconds -= minutes * realSecondsPerGameMinute;
        advanceMinutes(minutes);
    }

    /** Moves the clock forward directly; used by sleeping, cutscenes and tests. */
    public void advanceMinutes(int minutes) {
        for (int i = 0; i < minutes; i++) {
            TimeOfDay next = now.plusMinutes(1);
            boolean newDay = next.day() != now.day();
            now = next;
            listeners.forEach(l -> l.onMinute(now));
            if (newDay) {
                notifyDayStarted(now.day());
            }
            DayPhase nextPhase = now.phase();
            if (nextPhase != phase) {
                phase = nextPhase;
                listeners.forEach(l -> l.onPhaseChanged(phase, now));
            }
        }
    }

    /**
     * Jumps to the wake-up time of the following day.
     *
     * @return the new time
     */
    public TimeOfDay sleepUntilMorning() {
        int minutesUntilMidnight = TimeOfDay.MINUTES_PER_DAY - now.minuteOfDay();
        advanceMinutes(minutesUntilMidnight + wakeMinute);
        return now;
    }

    /** How many in-game minutes remain before the next day begins. */
    public int minutesUntilMidnight() {
        return TimeOfDay.MINUTES_PER_DAY - now.minuteOfDay();
    }

    private void notifyDayStarted(int day) {
        List.copyOf(listeners).forEach(l -> l.onDayStarted(day));
    }
}
