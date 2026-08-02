package com.keplersharvest.quests;

import java.util.Arrays;
import java.util.Objects;

/** Live progress for one quest. */
public final class QuestState {

    private final QuestDefinition definition;
    private QuestStatus status = QuestStatus.NOT_STARTED;
    private final int[] counters;

    QuestState(QuestDefinition definition) {
        this.definition = Objects.requireNonNull(definition, "definition");
        this.counters = new int[definition.objectiveCount()];
    }

    public QuestDefinition definition() {
        return definition;
    }

    public String questId() {
        return definition.id();
    }

    public QuestStatus status() {
        return status;
    }

    public boolean active() {
        return status == QuestStatus.ACTIVE;
    }

    public boolean completed() {
        return status == QuestStatus.COMPLETED;
    }

    void setStatus(QuestStatus status) {
        this.status = Objects.requireNonNull(status, "status");
    }

    int counter(int objectiveIndex) {
        return counters[objectiveIndex];
    }

    void addToCounter(int objectiveIndex, int amount) {
        counters[objectiveIndex] += amount;
    }

    /** Progress of one objective, combining polled world state with accumulated events. */
    public int progress(int objectiveIndex, QuestWorldView view) {
        return definition.objectives().get(objectiveIndex).progress(view, counters[objectiveIndex]);
    }

    public boolean objectiveComplete(int objectiveIndex, QuestWorldView view) {
        return definition.objectives().get(objectiveIndex).complete(view, counters[objectiveIndex]);
    }

    public boolean allObjectivesComplete(QuestWorldView view) {
        for (int i = 0; i < definition.objectiveCount(); i++) {
            if (!objectiveComplete(i, view)) {
                return false;
            }
        }
        return true;
    }

    public int[] counterSnapshot() {
        return counters.clone();
    }

    /** Restores counters from a save, tolerating a changed objective count. */
    public void restore(QuestStatus status, int[] saved) {
        this.status = Objects.requireNonNull(status, "status");
        Arrays.fill(counters, 0);
        int shared = Math.min(counters.length, saved.length);
        System.arraycopy(saved, 0, counters, 0, shared);
    }
}
