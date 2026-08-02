package com.keplersharvest.quests;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * A quest, loaded from {@code config/quests.json}.
 *
 * @param prerequisites quest ids that must be completed first
 * @param autoStart     starts by itself as soon as prerequisites are met
 * @param giverId       colonist who offers it, for the quest log's "speak to" hint
 */
public record QuestDefinition(
        String id,
        String title,
        String summary,
        List<Objective> objectives,
        List<String> prerequisites,
        QuestReward reward,
        boolean autoStart,
        Optional<String> giverId,
        Optional<String> completionText) {

    public QuestDefinition {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(summary, "summary");
        objectives = List.copyOf(Objects.requireNonNull(objectives, "objectives"));
        prerequisites = List.copyOf(Objects.requireNonNull(prerequisites, "prerequisites"));
        Objects.requireNonNull(reward, "reward");
        Objects.requireNonNull(giverId, "giverId");
        Objects.requireNonNull(completionText, "completionText");
        if (objectives.isEmpty()) {
            throw new IllegalArgumentException("Quest " + id + " has no objectives");
        }
    }

    public int objectiveCount() {
        return objectives.size();
    }
}
