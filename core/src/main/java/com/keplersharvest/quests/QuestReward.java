package com.keplersharvest.quests;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * What finishing a quest grants.
 *
 * @param items         item id to quantity
 * @param relationship  colonist id to relationship points
 * @param evidence      journal entries unlocked
 */
public record QuestReward(Map<String, Integer> items, Map<String, Integer> relationship, List<String> evidence) {

    public static final QuestReward NONE = new QuestReward(Map.of(), Map.of(), List.of());

    public QuestReward {
        items = Map.copyOf(Objects.requireNonNull(items, "items"));
        relationship = Map.copyOf(Objects.requireNonNull(relationship, "relationship"));
        evidence = List.copyOf(Objects.requireNonNull(evidence, "evidence"));
    }

    public boolean isEmpty() {
        return items.isEmpty() && relationship.isEmpty() && evidence.isEmpty();
    }
}
