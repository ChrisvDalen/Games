package com.keplersharvest.quests;

import com.keplersharvest.game.GameEvent;

/**
 * One tracked step of a quest.
 *
 * <p>Each variant knows two things: how to read its own progress, and which events advance it. That
 * keeps the quest engine free of a giant switch and makes adding an objective type a local change.
 */
public sealed interface Objective {

    /** Player-facing text shown in the quest log. */
    String description();

    /** Progress needed to count as done. */
    int target();

    /**
     * Current progress.
     *
     * @param counter events accumulated so far for this objective
     */
    int progress(QuestWorldView view, int counter);

    /** How much {@code event} adds to this objective's counter; 0 for polled objectives. */
    default int advanceFor(GameEvent event) {
        return 0;
    }

    default boolean complete(QuestWorldView view, int counter) {
        return progress(view, counter) >= target();
    }

    /** Hold a quantity of an item. Re-checks the inventory, so spending it un-completes the step. */
    record CollectItem(String itemId, int count, String description) implements Objective {
        @Override
        public int target() {
            return count;
        }

        @Override
        public int progress(QuestWorldView view, int counter) {
            return Math.min(count, view.itemCount(itemId));
        }
    }

    record RepairModule(String moduleId, String description) implements Objective {
        @Override
        public int target() {
            return 1;
        }

        @Override
        public int progress(QuestWorldView view, int counter) {
            return view.moduleOnline(moduleId) ? 1 : 0;
        }
    }

    record TalkTo(String colonistId, String description) implements Objective {
        @Override
        public int target() {
            return 1;
        }

        @Override
        public int progress(QuestWorldView view, int counter) {
            return Math.min(1, counter);
        }

        @Override
        public int advanceFor(GameEvent event) {
            return event instanceof GameEvent.ColonistGreeted greeted
                    && greeted.colonistId().equals(colonistId) ? 1 : 0;
        }
    }

    record DiscoverLandmark(String landmarkId, String description) implements Objective {
        @Override
        public int target() {
            return 1;
        }

        @Override
        public int progress(QuestWorldView view, int counter) {
            return view.landmarkDiscovered(landmarkId) ? 1 : 0;
        }
    }

    record FindCrewLog(String logId, String description) implements Objective {
        @Override
        public int target() {
            return 1;
        }

        @Override
        public int progress(QuestWorldView view, int counter) {
            return view.crewLogFound(logId) ? 1 : 0;
        }
    }

    record FindAnyCrewLogs(int count, String description) implements Objective {
        @Override
        public int target() {
            return count;
        }

        @Override
        public int progress(QuestWorldView view, int counter) {
            return Math.min(count, view.crewLogsFound());
        }
    }

    record CraftItem(String itemId, int count, String description) implements Objective {
        @Override
        public int target() {
            return count;
        }

        @Override
        public int progress(QuestWorldView view, int counter) {
            return Math.min(count, counter);
        }

        @Override
        public int advanceFor(GameEvent event) {
            return event instanceof GameEvent.ItemCrafted crafted && crafted.itemId().equals(itemId)
                    ? crafted.amount() : 0;
        }
    }

    record HarvestCrop(String cropId, int count, String description) implements Objective {
        @Override
        public int target() {
            return count;
        }

        @Override
        public int progress(QuestWorldView view, int counter) {
            return Math.min(count, counter);
        }

        @Override
        public int advanceFor(GameEvent event) {
            return event instanceof GameEvent.CropHarvested harvested && harvested.cropId().equals(cropId)
                    ? harvested.amount() : 0;
        }
    }

    record ReachRelationship(String colonistId, int points, String description) implements Objective {
        @Override
        public int target() {
            return points;
        }

        @Override
        public int progress(QuestWorldView view, int counter) {
            return Math.min(points, view.relationshipPoints(colonistId));
        }
    }
}
