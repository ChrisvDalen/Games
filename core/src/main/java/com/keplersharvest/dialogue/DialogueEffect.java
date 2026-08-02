package com.keplersharvest.dialogue;

import com.keplersharvest.game.StoryState;

/** Something a dialogue node or choice does to the world when it is reached. */
public sealed interface DialogueEffect {

    void apply(StoryState state, String speakerId);

    /** {@code colonistId} may be blank to mean "the colonist being spoken to". */
    record ChangeRelationship(String colonistId, int delta) implements DialogueEffect {
        @Override
        public void apply(StoryState state, String speakerId) {
            String target = colonistId == null || colonistId.isBlank() ? speakerId : colonistId;
            state.changeRelationship(target, delta);
        }
    }

    record StartQuest(String questId) implements DialogueEffect {
        @Override
        public void apply(StoryState state, String speakerId) {
            state.startQuest(questId);
        }
    }

    record GiveItem(String itemId, int count) implements DialogueEffect {
        @Override
        public void apply(StoryState state, String speakerId) {
            state.giveItem(itemId, count);
        }
    }

    record SetFlag(String flag) implements DialogueEffect {
        @Override
        public void apply(StoryState state, String speakerId) {
            state.setFlag(flag);
        }
    }

    record ClearFlag(String flag) implements DialogueEffect {
        @Override
        public void apply(StoryState state, String speakerId) {
            state.clearFlag(flag);
        }
    }

    record GrantEvidence(String evidenceId) implements DialogueEffect {
        @Override
        public void apply(StoryState state, String speakerId) {
            state.grantEvidence(evidenceId);
        }
    }
}
