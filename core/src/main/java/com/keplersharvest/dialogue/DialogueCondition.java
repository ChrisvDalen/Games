package com.keplersharvest.dialogue;

import com.keplersharvest.game.StoryState;

import java.util.List;

/**
 * A gate on a dialogue node or choice.
 *
 * <p>Authored in JSON as an object whose keys are combined with AND, e.g.
 * {@code {"questCompleted": "arrival", "minRelationship": 45}}.
 */
public sealed interface DialogueCondition {

    /**
     * @param speakerId the colonist being talked to, so conditions can omit an explicit target
     */
    boolean test(StoryState state, String speakerId);

    DialogueCondition ALWAYS = new Always();

    record Always() implements DialogueCondition {
        @Override
        public boolean test(StoryState state, String speakerId) {
            return true;
        }
    }

    /** {@code colonistId} may be blank to mean "the colonist being spoken to". */
    record MinRelationship(String colonistId, int points) implements DialogueCondition {
        @Override
        public boolean test(StoryState state, String speakerId) {
            String target = colonistId == null || colonistId.isBlank() ? speakerId : colonistId;
            return state.relationshipPoints(target) >= points;
        }
    }

    record QuestActive(String questId) implements DialogueCondition {
        @Override
        public boolean test(StoryState state, String speakerId) {
            return state.questActive(questId);
        }
    }

    record QuestCompleted(String questId) implements DialogueCondition {
        @Override
        public boolean test(StoryState state, String speakerId) {
            return state.questCompleted(questId);
        }
    }

    record QuestAvailable(String questId) implements DialogueCondition {
        @Override
        public boolean test(StoryState state, String speakerId) {
            return state.questAvailable(questId);
        }
    }

    record QuestNotStarted(String questId) implements DialogueCondition {
        @Override
        public boolean test(StoryState state, String speakerId) {
            return state.questNotStarted(questId);
        }
    }

    record ModuleOnline(String moduleId) implements DialogueCondition {
        @Override
        public boolean test(StoryState state, String speakerId) {
            return state.moduleOnline(moduleId);
        }
    }

    record ModuleOffline(String moduleId) implements DialogueCondition {
        @Override
        public boolean test(StoryState state, String speakerId) {
            return !state.moduleOnline(moduleId);
        }
    }

    record LogFound(String logId) implements DialogueCondition {
        @Override
        public boolean test(StoryState state, String speakerId) {
            return state.crewLogFound(logId);
        }
    }

    record MinLogs(int count) implements DialogueCondition {
        @Override
        public boolean test(StoryState state, String speakerId) {
            return state.crewLogCount() >= count;
        }
    }

    record EvidenceKnown(String evidenceId) implements DialogueCondition {
        @Override
        public boolean test(StoryState state, String speakerId) {
            return state.evidenceKnown(evidenceId);
        }
    }

    record FlagSet(String flag) implements DialogueCondition {
        @Override
        public boolean test(StoryState state, String speakerId) {
            return state.flagSet(flag);
        }
    }

    record FlagClear(String flag) implements DialogueCondition {
        @Override
        public boolean test(StoryState state, String speakerId) {
            return !state.flagSet(flag);
        }
    }

    record MinDay(int day) implements DialogueCondition {
        @Override
        public boolean test(StoryState state, String speakerId) {
            return state.day() >= day;
        }
    }

    record All(List<DialogueCondition> conditions) implements DialogueCondition {
        public All {
            conditions = List.copyOf(conditions);
        }

        @Override
        public boolean test(StoryState state, String speakerId) {
            return conditions.stream().allMatch(c -> c.test(state, speakerId));
        }
    }

    record Any(List<DialogueCondition> conditions) implements DialogueCondition {
        public Any {
            conditions = List.copyOf(conditions);
        }

        @Override
        public boolean test(StoryState state, String speakerId) {
            return conditions.isEmpty() || conditions.stream().anyMatch(c -> c.test(state, speakerId));
        }
    }
}
