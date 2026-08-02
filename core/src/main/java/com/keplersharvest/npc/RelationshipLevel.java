package com.keplersharvest.npc;

/** Friendship tiers. Dialogue and clues gate on these rather than on raw point totals. */
public enum RelationshipLevel {
    STRANGER("Stranger", 0),
    ACQUAINTANCE("Acquaintance", 20),
    COLLEAGUE("Colleague", 45),
    FRIEND("Friend", 75),
    CONFIDANT("Confidant", 110);

    /** Points beyond which the meter stops rising. */
    public static final int MAX_POINTS = 140;

    private final String label;
    private final int threshold;

    RelationshipLevel(String label, int threshold) {
        this.label = label;
        this.threshold = threshold;
    }

    public String label() {
        return label;
    }

    public int threshold() {
        return threshold;
    }

    public static RelationshipLevel forPoints(int points) {
        RelationshipLevel best = STRANGER;
        for (RelationshipLevel level : values()) {
            if (points >= level.threshold) {
                best = level;
            }
        }
        return best;
    }

    /** Points still needed to reach the next tier, or 0 at the top. */
    public static int pointsToNext(int points) {
        for (RelationshipLevel level : values()) {
            if (points < level.threshold) {
                return level.threshold - points;
            }
        }
        return 0;
    }
}
