package com.keplersharvest.npc;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Relationship points per colonist, clamped to a sane range.
 *
 * <p>Crossing a {@link RelationshipLevel} threshold is reported back to the caller so the session
 * can announce it and unlock the dialogue that goes with the new tier.
 */
public final class RelationshipBook {

    private final Map<String, Integer> points = new LinkedHashMap<>();

    public int points(String colonistId) {
        return points.getOrDefault(colonistId, 0);
    }

    public RelationshipLevel level(String colonistId) {
        return RelationshipLevel.forPoints(points(colonistId));
    }

    /**
     * Adds (or removes) points.
     *
     * @return the level crossed into, or empty when the tier did not change
     */
    public java.util.Optional<RelationshipLevel> add(String colonistId, int delta) {
        int before = points(colonistId);
        int after = Math.clamp((long) before + delta, 0, RelationshipLevel.MAX_POINTS);
        points.put(colonistId, after);
        RelationshipLevel levelBefore = RelationshipLevel.forPoints(before);
        RelationshipLevel levelAfter = RelationshipLevel.forPoints(after);
        return levelAfter != levelBefore && after > before
                ? java.util.Optional.of(levelAfter)
                : java.util.Optional.empty();
    }

    public void set(String colonistId, int value) {
        points.put(colonistId, Math.clamp(value, 0, RelationshipLevel.MAX_POINTS));
    }

    public Map<String, Integer> snapshot() {
        return Map.copyOf(points);
    }

    public void restore(Map<String, Integer> saved) {
        points.clear();
        saved.forEach(this::set);
    }
}
