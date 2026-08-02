package com.keplersharvest.time;

/** Reacts to the passage of colony time. All callbacks are optional. */
public interface ClockListener {

    /** Fired once per elapsed in-game minute. */
    default void onMinute(TimeOfDay now) {
    }

    /** Fired when the clock crosses into a new {@link DayPhase}. */
    default void onPhaseChanged(DayPhase phase, TimeOfDay now) {
    }

    /**
     * Fired once per new day, whether reached by sleeping or by staying up past midnight.
     *
     * @param day the day just started
     */
    default void onDayStarted(int day) {
    }
}
