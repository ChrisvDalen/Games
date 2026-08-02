package com.keplersharvest.npc;

import com.keplersharvest.time.DayPhase;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * A colonist, loaded from {@code config/colonists.json}.
 *
 * @param dialogueId     the dialogue tree file this colonist speaks from
 * @param personalQuestId their one personal quest
 * @param schedule       daily locations, checked in order so later entries can override earlier ones
 */
public record ColonistDefinition(
        String id,
        String name,
        String role,
        String personality,
        String dialogueId,
        Optional<String> personalQuestId,
        List<ScheduleEntry> schedule,
        String colour) {

    public ColonistDefinition {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(role, "role");
        Objects.requireNonNull(personality, "personality");
        Objects.requireNonNull(dialogueId, "dialogueId");
        Objects.requireNonNull(personalQuestId, "personalQuestId");
        Objects.requireNonNull(colour, "colour");
        schedule = List.copyOf(Objects.requireNonNull(schedule, "schedule"));
        if (schedule.isEmpty()) {
            throw new IllegalArgumentException("Colonist " + id + " has no schedule");
        }
    }

    /**
     * Where this colonist is during {@code phase}.
     *
     * <p>The last matching entry wins, so a module-gated entry listed after the default one takes
     * over when that module comes online.
     */
    public ScheduleEntry locationDuring(DayPhase phase, Predicate<String> moduleOnline) {
        ScheduleEntry chosen = null;
        for (ScheduleEntry entry : schedule) {
            if (entry.phase() != phase) {
                continue;
            }
            if (entry.requiresModule().isPresent() && !moduleOnline.test(entry.requiresModule().get())) {
                continue;
            }
            chosen = entry;
        }
        if (chosen != null) {
            return chosen;
        }
        // Fall back to any entry so a colonist is never missing from the world.
        return schedule.get(0);
    }
}
