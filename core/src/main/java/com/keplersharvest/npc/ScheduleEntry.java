package com.keplersharvest.npc;

import com.keplersharvest.time.DayPhase;
import com.keplersharvest.world.GridPoint;

import java.util.Objects;
import java.util.Optional;

/**
 * Where a colonist stands during one phase of the day.
 *
 * @param requiresModule when set, this entry only applies once that module is back online, which is
 *                       how colonists visibly react to repairs
 */
public record ScheduleEntry(DayPhase phase, String mapId, GridPoint position, Optional<String> requiresModule) {

    public ScheduleEntry {
        Objects.requireNonNull(phase, "phase");
        Objects.requireNonNull(mapId, "mapId");
        Objects.requireNonNull(position, "position");
        Objects.requireNonNull(requiresModule, "requiresModule");
    }
}
