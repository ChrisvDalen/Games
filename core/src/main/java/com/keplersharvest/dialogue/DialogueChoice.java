package com.keplersharvest.dialogue;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * A reply the player can pick.
 *
 * @param next the node to jump to; empty ends the conversation
 */
public record DialogueChoice(
        String text,
        DialogueCondition condition,
        Optional<String> next,
        List<DialogueEffect> effects) {

    public DialogueChoice {
        Objects.requireNonNull(text, "text");
        Objects.requireNonNull(condition, "condition");
        Objects.requireNonNull(next, "next");
        effects = List.copyOf(Objects.requireNonNull(effects, "effects"));
    }
}
