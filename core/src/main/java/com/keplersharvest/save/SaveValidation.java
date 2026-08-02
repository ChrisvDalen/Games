package com.keplersharvest.save;

import java.util.ArrayList;
import java.util.List;

/**
 * Structural checks run on a save before it is allowed anywhere near a live session.
 *
 * <p>Content mismatches (an item id that no longer exists) are *not* errors: those are dropped
 * during mapping so an old save still loads after content changes. What is checked here is the
 * things that would produce a nonsensical session.
 */
public final class SaveValidation {

    private SaveValidation() {
    }

    /**
     * @return a list of problems; empty means the save is usable
     */
    public static List<String> problems(SaveData data) {
        List<String> problems = new ArrayList<>();
        if (data == null) {
            return List.of("Save file is empty.");
        }
        if (data.version <= 0) {
            problems.add("Missing save format version.");
        }
        if (data.version > SaveData.CURRENT_VERSION) {
            problems.add("Save was written by a newer version of the game (format "
                    + data.version + ", this build reads " + SaveData.CURRENT_VERSION + ").");
        }
        if (data.day < 1) {
            problems.add("Day counter is " + data.day + ".");
        }
        if (data.minuteOfDay < 0 || data.minuteOfDay >= 24 * 60) {
            problems.add("Time of day is out of range: " + data.minuteOfDay + ".");
        }
        if (data.mapId == null || data.mapId.isBlank()) {
            problems.add("Save does not say which map the player is on.");
        }
        if (data.energy < 0) {
            problems.add("Energy is negative.");
        }
        if (data.inventory != null) {
            for (SaveData.SlotEntry slot : data.inventory) {
                if (slot.slot < 0) {
                    problems.add("Inventory slot index " + slot.slot + " is negative.");
                }
                if (slot.count < 1) {
                    problems.add("Inventory slot " + slot.slot + " holds " + slot.count + " items.");
                }
                if (slot.item == null || slot.item.isBlank()) {
                    problems.add("Inventory slot " + slot.slot + " has no item id.");
                }
            }
        }
        if (data.plots != null) {
            for (SaveData.PlotEntry plot : data.plots) {
                if (plot.map == null || plot.map.isBlank()) {
                    problems.add("A farm plot has no map id.");
                }
                if (plot.grownDays < 0 || plot.dryDays < 0) {
                    problems.add("A farm plot has negative growth counters.");
                }
            }
        }
        return List.copyOf(problems);
    }

    public static boolean isValid(SaveData data) {
        return problems(data).isEmpty();
    }
}
