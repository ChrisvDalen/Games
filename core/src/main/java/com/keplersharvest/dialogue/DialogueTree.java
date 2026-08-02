package com.keplersharvest.dialogue;

import com.keplersharvest.game.StoryState;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * One colonist's conversation, loaded from {@code config/dialogue/<id>.json}.
 *
 * <p>The opening node is chosen by walking the entry nodes in authored order and taking the first
 * whose condition passes, so writers control priority by ordering rather than by nesting.
 */
public record DialogueTree(String id, String speakerId, List<DialogueNode> nodes, Map<String, DialogueNode> byId) {

    public DialogueTree(String id, String speakerId, List<DialogueNode> nodes) {
        this(id, speakerId, List.copyOf(nodes), index(nodes));
    }

    public DialogueTree {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(speakerId, "speakerId");
        nodes = List.copyOf(Objects.requireNonNull(nodes, "nodes"));
        byId = Map.copyOf(Objects.requireNonNull(byId, "byId"));
        if (nodes.isEmpty()) {
            throw new IllegalArgumentException("Dialogue tree " + id + " has no nodes");
        }
        if (nodes.stream().noneMatch(DialogueNode::entry)) {
            throw new IllegalArgumentException("Dialogue tree " + id + " has no entry node");
        }
    }

    private static Map<String, DialogueNode> index(List<DialogueNode> nodes) {
        Map<String, DialogueNode> map = new LinkedHashMap<>();
        for (DialogueNode node : nodes) {
            if (map.put(node.id(), node) != null) {
                throw new IllegalArgumentException("Duplicate dialogue node id: " + node.id());
            }
        }
        return map;
    }

    public Optional<DialogueNode> node(String nodeId) {
        return Optional.ofNullable(byId.get(nodeId));
    }

    /** The first entry node whose condition holds. */
    public Optional<DialogueNode> openingNode(StoryState state) {
        return nodes.stream()
                .filter(DialogueNode::entry)
                .filter(node -> node.condition().test(state, speakerId))
                .findFirst();
    }
}
