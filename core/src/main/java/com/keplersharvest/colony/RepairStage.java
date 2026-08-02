package com.keplersharvest.colony;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * One step of a module repair.
 *
 * @param materials item id to quantity consumed by this stage
 */
public record RepairStage(int index, String name, String description, Map<String, Integer> materials) {

    public RepairStage {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(description, "description");
        materials = Map.copyOf(new LinkedHashMap<>(Objects.requireNonNull(materials, "materials")));
        if (index < 0) {
            throw new IllegalArgumentException("Stage index must not be negative");
        }
        if (materials.values().stream().anyMatch(v -> v < 1)) {
            throw new IllegalArgumentException("Repair stage " + name + " asks for a non-positive quantity");
        }
    }
}
