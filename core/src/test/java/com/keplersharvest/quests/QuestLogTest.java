package com.keplersharvest.quests;

import com.keplersharvest.game.GameEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuestLogTest {

    /** A hand-controlled world so objective rules can be checked in isolation. */
    private static final class FakeWorld implements QuestWorldView {
        private final Map<String, Integer> items = new HashMap<>();
        private final Map<String, Boolean> modules = new HashMap<>();
        private final Map<String, Integer> relationships = new HashMap<>();
        private int logs;

        @Override
        public int itemCount(String itemId) {
            return items.getOrDefault(itemId, 0);
        }

        @Override
        public boolean moduleOnline(String moduleId) {
            return modules.getOrDefault(moduleId, false);
        }

        @Override
        public boolean landmarkDiscovered(String landmarkId) {
            return false;
        }

        @Override
        public boolean crewLogFound(String logId) {
            return false;
        }

        @Override
        public int crewLogsFound() {
            return logs;
        }

        @Override
        public int relationshipPoints(String colonistId) {
            return relationships.getOrDefault(colonistId, 0);
        }
    }

    private static QuestDefinition quest(String id, List<Objective> objectives, List<String> prerequisites,
                                         boolean autoStart) {
        return new QuestDefinition(id, id, "", objectives, prerequisites, QuestReward.NONE, autoStart,
                Optional.empty(), Optional.empty());
    }

    @Test
    @DisplayName("a quest completes when every objective is satisfied")
    void completesWhenObjectivesAreMet() {
        QuestDefinition definition = quest("first_breath", List.of(
                new Objective.CollectItem("scrap_alloy", 4, "Salvage 4 alloy"),
                new Objective.RepairModule("oxygen_recycler", "Bring the recycler online")), List.of(), true);
        QuestLog log = new QuestLog(List.of(definition));
        FakeWorld world = new FakeWorld();

        log.startAvailableAutoQuests();
        assertTrue(log.isActive("first_breath"));

        world.items.put("scrap_alloy", 4);
        assertTrue(log.completeFinishedQuests(world).isEmpty(), "the module is still down");

        world.modules.put("oxygen_recycler", true);
        List<QuestDefinition> finished = log.completeFinishedQuests(world);

        assertEquals(List.of(definition), finished);
        assertTrue(log.isCompleted("first_breath"));
        assertTrue(log.completeFinishedQuests(world).isEmpty(), "a quest only completes once");
    }

    @Test
    @DisplayName("objective progress is reported for partially met steps")
    void reportsPartialProgress() {
        QuestDefinition definition = quest("gather", List.of(
                new Objective.CollectItem("scrap_alloy", 8, "Salvage 8 alloy")), List.of(), true);
        QuestLog log = new QuestLog(List.of(definition));
        FakeWorld world = new FakeWorld();
        log.startAvailableAutoQuests();
        world.items.put("scrap_alloy", 3);

        QuestState state = log.state("gather").orElseThrow();

        assertEquals(3, state.progress(0, world));
        assertFalse(state.objectiveComplete(0, world));
    }

    @Test
    @DisplayName("prerequisites block a quest until they are finished")
    void prerequisitesGateQuests() {
        QuestDefinition first = quest("arrival", List.of(new Objective.TalkTo("sefa", "Find Sefa")),
                List.of(), true);
        QuestDefinition second = quest("follow_up", List.of(new Objective.CollectItem("glassroot", 1, "Bring one")),
                List.of("arrival"), true);
        QuestLog log = new QuestLog(List.of(first, second));
        FakeWorld world = new FakeWorld();

        assertFalse(log.available("follow_up"));
        assertEquals(List.of(first), log.startAvailableAutoQuests(), "only the unblocked quest starts");

        log.onEvent(new GameEvent.ColonistGreeted("sefa"));
        log.completeFinishedQuests(world);

        assertTrue(log.available("follow_up"));
        assertEquals(List.of(second), log.startAvailableAutoQuests());
    }

    @Test
    @DisplayName("act-based objectives accumulate from events")
    void eventsAdvanceActObjectives() {
        QuestDefinition definition = quest("breaking_ground", List.of(
                new Objective.HarvestCrop("lumen_pod", 2, "Harvest two pods")), List.of(), true);
        QuestLog log = new QuestLog(List.of(definition));
        FakeWorld world = new FakeWorld();
        log.startAvailableAutoQuests();

        log.onEvent(new GameEvent.CropHarvested("lumen_pod", "lumen_pod", 1));
        log.onEvent(new GameEvent.CropHarvested("glassroot", "glassroot", 5));
        assertTrue(log.completeFinishedQuests(world).isEmpty(), "the wrong crop does not count");

        log.onEvent(new GameEvent.CropHarvested("lumen_pod", "lumen_pod", 1));

        assertEquals(1, log.completeFinishedQuests(world).size());
    }

    @Test
    @DisplayName("condition objectives re-check the world, act objectives do not")
    void conditionAndActObjectivesDiffer() {
        Objective collect = new Objective.CollectItem("glassroot", 2, "Hold two");
        Objective craft = new Objective.CraftItem("filter_mesh", 1, "Make one");
        FakeWorld world = new FakeWorld();

        world.items.put("glassroot", 2);
        assertTrue(collect.complete(world, 0));
        world.items.put("glassroot", 0);
        assertFalse(collect.complete(world, 0), "spending the item un-completes a hold objective");

        assertEquals(1, craft.advanceFor(new GameEvent.ItemCrafted("filter_mesh", 1)));
        assertEquals(0, craft.advanceFor(new GameEvent.ItemCrafted("sealing_gel", 1)));
        assertTrue(craft.complete(world, 1), "having made it once counts even after it is spent");
    }

    @Test
    @DisplayName("events for inactive quests are ignored")
    void inactiveQuestsIgnoreEvents() {
        QuestDefinition definition = quest("later", List.of(
                new Objective.TalkTo("rike", "Find Rike")), List.of("never_done"), false);
        QuestDefinition blocker = quest("never_done", List.of(
                new Objective.CollectItem("nothing", 1, "impossible")), List.of(), false);
        QuestLog log = new QuestLog(List.of(definition, blocker));

        log.onEvent(new GameEvent.ColonistGreeted("rike"));

        assertEquals(QuestStatus.NOT_STARTED, log.status("later"));
        assertFalse(log.start("later"), "prerequisites are still unmet");
    }

    @Test
    @DisplayName("counters survive a save round trip, tolerating a changed objective count")
    void restoreToleratesContentChanges() {
        QuestDefinition definition = quest("gather", List.of(
                new Objective.HarvestCrop("lumen_pod", 3, "Harvest three")), List.of(), true);
        QuestLog log = new QuestLog(List.of(definition));
        QuestState state = log.state("gather").orElseThrow();

        state.restore(QuestStatus.ACTIVE, new int[]{2, 9, 9});

        assertEquals(QuestStatus.ACTIVE, state.status());
        assertEquals(2, state.progress(0, new FakeWorld()), "extra saved counters are discarded");
    }
}
