package com.keplersharvest.dialogue;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * One beat of a conversation: a few lines, optional replies, and where to go next.
 *
 * @param entry  eligible as the conversation's opening node
 * @param next   followed after the last line when there are no choices; empty ends the conversation
 */
public record DialogueNode(
        String id,
        boolean entry,
        DialogueCondition condition,
        List<String> lines,
        List<DialogueEffect> effects,
        List<DialogueChoice> choices,
        Optional<String> next) {

    public DialogueNode {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(condition, "condition");
        Objects.requireNonNull(next, "next");
        lines = List.copyOf(Objects.requireNonNull(lines, "lines"));
        effects = List.copyOf(Objects.requireNonNull(effects, "effects"));
        choices = List.copyOf(Objects.requireNonNull(choices, "choices"));
        if (lines.isEmpty()) {
            throw new IllegalArgumentException("Dialogue node " + id + " has no lines");
        }
    }
}
