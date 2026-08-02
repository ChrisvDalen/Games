package com.keplersharvest.world;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Runtime state of placed map objects: which resource nodes are depleted and when they come back,
 * and which one-shot objects have been used up.
 *
 * <p>Keyed by {@code mapId#objectId} so state survives leaving and re-entering a map.
 */
public final class WorldObjectState {

    private final Map<String, Integer> depletedOnDay = new LinkedHashMap<>();
    private final Set<String> consumed = new LinkedHashSet<>();

    public static String key(String mapId, String objectId) {
        return mapId + "#" + objectId;
    }

    /** Marks a gatherable node as harvested on {@code day}. */
    public void markDepleted(String key, int day) {
        depletedOnDay.put(key, day);
    }

    /** Marks a one-shot object (a crew log, a scripted find) as permanently used. */
    public void markConsumed(String key) {
        consumed.add(key);
    }

    public boolean isConsumed(String key) {
        return consumed.contains(key);
    }

    /**
     * Whether a node can be gathered again.
     *
     * @param respawnDays 0 or less means the node never returns
     */
    public boolean isAvailable(String key, int currentDay, int respawnDays) {
        if (consumed.contains(key)) {
            return false;
        }
        Integer day = depletedOnDay.get(key);
        if (day == null) {
            return true;
        }
        if (respawnDays <= 0) {
            return false;
        }
        return currentDay - day >= respawnDays;
    }

    /** Clears respawn bookkeeping for nodes that are available again, keeping saves small. */
    public void pruneRespawned(int currentDay, int maxRespawnDays) {
        depletedOnDay.entrySet().removeIf(e -> currentDay - e.getValue() > maxRespawnDays);
    }

    public Map<String, Integer> depletedSnapshot() {
        return Map.copyOf(depletedOnDay);
    }

    public Set<String> consumedSnapshot() {
        return Set.copyOf(consumed);
    }

    public void restore(Map<String, Integer> depleted, Set<String> consumedKeys) {
        depletedOnDay.clear();
        depletedOnDay.putAll(depleted);
        consumed.clear();
        consumed.addAll(consumedKeys);
    }
}
