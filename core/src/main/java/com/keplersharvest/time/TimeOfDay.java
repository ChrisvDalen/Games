package com.keplersharvest.time;

/**
 * An instant in colony time.
 *
 * @param day          1-based day counter since landfall
 * @param minuteOfDay  0..1439
 */
public record TimeOfDay(int day, int minuteOfDay) {

    public static final int MINUTES_PER_DAY = 24 * 60;

    public TimeOfDay {
        if (day < 1) {
            throw new IllegalArgumentException("Day must be at least 1, got " + day);
        }
        if (minuteOfDay < 0 || minuteOfDay >= MINUTES_PER_DAY) {
            throw new IllegalArgumentException("minuteOfDay outside 0..1439: " + minuteOfDay);
        }
    }

    public static TimeOfDay startOf(int day, int minuteOfDay) {
        return new TimeOfDay(day, minuteOfDay);
    }

    public DayPhase phase() {
        return DayPhase.at(minuteOfDay);
    }

    public int hour() {
        return minuteOfDay / 60;
    }

    public int minute() {
        return minuteOfDay % 60;
    }

    /** Advances by {@code minutes}, rolling the day counter over midnight. */
    public TimeOfDay plusMinutes(int minutes) {
        if (minutes < 0) {
            throw new IllegalArgumentException("Time only moves forwards, got " + minutes);
        }
        int total = minuteOfDay + minutes;
        return new TimeOfDay(day + total / MINUTES_PER_DAY, total % MINUTES_PER_DAY);
    }

    /** 24-hour clock reading, e.g. {@code 06:30}. */
    public String clockText() {
        return String.format("%02d:%02d", hour(), minute());
    }

    @Override
    public String toString() {
        return "Day " + day + " " + clockText();
    }
}
