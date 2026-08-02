package com.keplersharvest.quests;

import com.keplersharvest.game.GameEvent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Tracks which quests are offered, active and finished.
 *
 * <p>Bookkeeping only: handing out rewards belongs to the session, so the log can be tested with no
 * inventory or world behind it.
 */
public final class QuestLog {

    private final Map<String, QuestState> states = new LinkedHashMap<>();

    public QuestLog(Collection<QuestDefinition> definitions) {
        Objects.requireNonNull(definitions, "definitions")
                .forEach(def -> states.put(def.id(), new QuestState(def)));
    }

    public Collection<QuestState> all() {
        return states.values();
    }

    public Optional<QuestState> state(String questId) {
        return Optional.ofNullable(states.get(questId));
    }

    public QuestStatus status(String questId) {
        return state(questId).map(QuestState::status).orElse(QuestStatus.NOT_STARTED);
    }

    public boolean isActive(String questId) {
        return status(questId) == QuestStatus.ACTIVE;
    }

    public boolean isCompleted(String questId) {
        return status(questId) == QuestStatus.COMPLETED;
    }

    public List<QuestState> active() {
        return states.values().stream().filter(QuestState::active).toList();
    }

    public List<QuestState> completed() {
        return states.values().stream().filter(QuestState::completed).toList();
    }

    /** A quest is offerable when it has not started and every prerequisite is complete. */
    public boolean available(String questId) {
        QuestState state = states.get(questId);
        return state != null
                && state.status() == QuestStatus.NOT_STARTED
                && prerequisitesMet(state.definition());
    }

    public boolean prerequisitesMet(QuestDefinition definition) {
        return definition.prerequisites().stream().allMatch(this::isCompleted);
    }

    /**
     * Starts a quest.
     *
     * @return false when it is already running, finished, or still blocked
     */
    public boolean start(String questId) {
        if (!available(questId)) {
            return false;
        }
        states.get(questId).setStatus(QuestStatus.ACTIVE);
        return true;
    }

    /** Starts every auto-start quest whose prerequisites are now met. */
    public List<QuestDefinition> startAvailableAutoQuests() {
        List<QuestDefinition> started = new ArrayList<>();
        for (QuestState state : states.values()) {
            if (state.definition().autoStart() && available(state.questId())) {
                state.setStatus(QuestStatus.ACTIVE);
                started.add(state.definition());
            }
        }
        return started;
    }

    /** Feeds an event to the counters of every active quest. */
    public void onEvent(GameEvent event) {
        for (QuestState state : states.values()) {
            if (!state.active()) {
                continue;
            }
            List<Objective> objectives = state.definition().objectives();
            for (int i = 0; i < objectives.size(); i++) {
                int delta = objectives.get(i).advanceFor(event);
                if (delta > 0) {
                    state.addToCounter(i, delta);
                }
            }
        }
    }

    /**
     * Completes any active quest whose objectives are all satisfied.
     *
     * @return the quests that just finished, so the caller can pay their rewards
     */
    public List<QuestDefinition> completeFinishedQuests(QuestWorldView view) {
        List<QuestDefinition> finished = new ArrayList<>();
        for (QuestState state : states.values()) {
            if (state.active() && state.allObjectivesComplete(view)) {
                state.setStatus(QuestStatus.COMPLETED);
                finished.add(state.definition());
            }
        }
        return finished;
    }
}
