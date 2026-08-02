package com.keplersharvest.game;

/**
 * Something the player did that other systems may care about.
 *
 * <p>Quests, relationships and the journal all subscribe to these rather than being called directly
 * by the farming or repair code, which keeps those systems unaware of each other.
 */
public sealed interface GameEvent {

    record ItemGathered(String itemId, int amount) implements GameEvent {
    }

    record CropHarvested(String cropId, String produceItemId, int amount) implements GameEvent {
    }

    record ItemCrafted(String itemId, int amount) implements GameEvent {
    }

    record CropPlanted(String cropId) implements GameEvent {
    }

    record SoilTilled(int count) implements GameEvent {
    }

    record ModuleStageRepaired(String moduleId, int stageIndex, boolean online) implements GameEvent {
    }

    record ColonistGreeted(String colonistId) implements GameEvent {
    }

    record RelationshipChanged(String colonistId, int points) implements GameEvent {
    }

    record LandmarkDiscovered(String landmarkId, String displayName) implements GameEvent {
    }

    record CrewLogFound(String logId) implements GameEvent {
    }

    record EvidenceRecorded(String evidenceId) implements GameEvent {
    }

    record QuestStarted(String questId) implements GameEvent {
    }

    record QuestCompleted(String questId) implements GameEvent {
    }

    record DayStarted(int day) implements GameEvent {
    }

    record RevealUnlocked(String revealId) implements GameEvent {
    }
}
