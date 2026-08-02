package com.keplersharvest.dialogue;

import com.keplersharvest.game.StoryState;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Walks one conversation: which line is on screen, which replies are offered, and what the world
 * looks like afterwards.
 *
 * <p>Node effects fire once, on entry. Choice effects fire when the choice is taken. A node whose
 * choices are all condition-failed falls through to its {@code next} rather than dead-ending.
 */
public final class DialogueRunner {

    private final DialogueTree tree;
    private final StoryState state;

    private DialogueNode node;
    private int lineIndex;
    private boolean finished;

    private DialogueRunner(DialogueTree tree, StoryState state, DialogueNode opening) {
        this.tree = Objects.requireNonNull(tree, "tree");
        this.state = Objects.requireNonNull(state, "state");
        enter(opening);
    }

    /** Starts a conversation, or returns empty when no entry node's condition holds. */
    public static Optional<DialogueRunner> start(DialogueTree tree, StoryState state) {
        return tree.openingNode(state).map(node -> new DialogueRunner(tree, state, node));
    }

    public String speakerId() {
        return tree.speakerId();
    }

    public DialogueNode currentNode() {
        return node;
    }

    public boolean finished() {
        return finished;
    }

    public String currentLine() {
        if (finished) {
            throw new IllegalStateException("Conversation has ended");
        }
        return node.lines().get(lineIndex);
    }

    public int lineIndex() {
        return lineIndex;
    }

    public int lineCount() {
        return node.lines().size();
    }

    public boolean atLastLine() {
        return lineIndex >= node.lines().size() - 1;
    }

    /** Replies whose conditions currently pass. Empty until the last line of the node. */
    public List<DialogueChoice> choices() {
        if (finished || !atLastLine()) {
            return List.of();
        }
        return node.choices().stream()
                .filter(choice -> choice.condition().test(state, tree.speakerId()))
                .toList();
    }

    public boolean awaitingChoice() {
        return !choices().isEmpty();
    }

    /**
     * Moves to the next line, or follows the node's {@code next} link.
     *
     * <p>Does nothing while the player still has to pick a reply.
     */
    public void advance() {
        if (finished || awaitingChoice()) {
            return;
        }
        if (!atLastLine()) {
            lineIndex++;
            return;
        }
        followTo(node.next());
    }

    /** Takes the {@code index}-th currently visible reply. */
    public void choose(int index) {
        List<DialogueChoice> visible = choices();
        if (index < 0 || index >= visible.size()) {
            throw new IndexOutOfBoundsException("No reply at index " + index);
        }
        DialogueChoice choice = visible.get(index);
        choice.effects().forEach(effect -> effect.apply(state, tree.speakerId()));
        followTo(choice.next());
    }

    private void followTo(Optional<String> nextId) {
        if (nextId.isEmpty()) {
            finished = true;
            return;
        }
        Optional<DialogueNode> target = tree.node(nextId.get());
        if (target.isEmpty()) {
            finished = true;
            return;
        }
        enter(target.get());
    }

    private void enter(DialogueNode next) {
        this.node = next;
        this.lineIndex = 0;
        next.effects().forEach(effect -> effect.apply(state, tree.speakerId()));
    }
}
