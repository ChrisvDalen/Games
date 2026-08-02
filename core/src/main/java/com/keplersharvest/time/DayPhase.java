package com.keplersharvest.time;

/** Coarse time-of-day buckets. NPC schedules, hazards and dialogue all key off these. */
public enum DayPhase {
    MORNING("Morning", 6 * 60),
    AFTERNOON("Afternoon", 12 * 60),
    EVENING("Evening", 18 * 60),
    NIGHT("Night", 22 * 60);

    private final String label;
    private final int startMinute;

    DayPhase(String label, int startMinute) {
        this.label = label;
        this.startMinute = startMinute;
    }

    public String label() {
        return label;
    }

    public int startMinute() {
        return startMinute;
    }

    /** The phase containing {@code minuteOfDay} (0..1439). Hours before 06:00 are still night. */
    public static DayPhase at(int minuteOfDay) {
        if (minuteOfDay >= NIGHT.startMinute || minuteOfDay < MORNING.startMinute) {
            return NIGHT;
        }
        if (minuteOfDay >= EVENING.startMinute) {
            return EVENING;
        }
        if (minuteOfDay >= AFTERNOON.startMinute) {
            return AFTERNOON;
        }
        return MORNING;
    }
}
