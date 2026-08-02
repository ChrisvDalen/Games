package com.keplersharvest.configuration;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Tuning values from {@code config/game.json}.
 *
 * <p>Day length, energy costs and the starting kit are all data, so balance changes never need a
 * recompile.
 *
 * @param realSecondsPerGameMinute wall-clock seconds per in-game minute; smaller means faster days
 * @param wakeMinute               minute of day the player wakes at
 * @param collapseMinute           minute of day the player passes out if still awake
 */
public record GameSettings(
        float realSecondsPerGameMinute,
        int wakeMinute,
        int collapseMinute,
        int baseMaxEnergy,
        int collapseEnergyPenalty,
        int inventorySize,
        int toolbarSize,
        float playerTilesPerSecond,
        int nightEnergyDrainPerHour,
        int hazardEnergyDrainPerHour,
        int dailyGreetingPoints,
        Map<String, Integer> energyCosts,
        List<StartingItem> startingItems) {

    /** An item handed out when a new game begins. */
    public record StartingItem(String itemId, int count) {
        public StartingItem {
            Objects.requireNonNull(itemId, "itemId");
            if (count < 1) {
                throw new IllegalArgumentException("Starting item " + itemId + " has count " + count);
            }
        }
    }

    public GameSettings {
        energyCosts = Map.copyOf(Objects.requireNonNull(energyCosts, "energyCosts"));
        startingItems = List.copyOf(Objects.requireNonNull(startingItems, "startingItems"));
        if (realSecondsPerGameMinute <= 0f) {
            throw new IllegalArgumentException("realSecondsPerGameMinute must be positive");
        }
        if (baseMaxEnergy < 1) {
            throw new IllegalArgumentException("baseMaxEnergy must be positive");
        }
        if (toolbarSize < 1 || toolbarSize > inventorySize) {
            throw new IllegalArgumentException("toolbarSize must fit inside inventorySize");
        }
    }

    /** Energy cost of a named action, defaulting to free when unlisted. */
    public int energyCost(String action) {
        return energyCosts.getOrDefault(action, 0);
    }

    /** How long a full in-game day takes in real seconds, for the README and the options screen. */
    public float realSecondsPerDay() {
        return realSecondsPerGameMinute * 24 * 60;
    }
}
