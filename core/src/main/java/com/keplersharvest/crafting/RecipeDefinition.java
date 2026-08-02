package com.keplersharvest.crafting;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * A craftable output, loaded from {@code config/recipes.json}.
 *
 * @param stationId      which bench can run it
 * @param requiresModule module that must be online before the recipe appears
 */
public record RecipeDefinition(
        String id,
        String name,
        String description,
        String stationId,
        Map<String, Integer> inputs,
        String outputItemId,
        int outputCount,
        Optional<String> requiresModule) {

    public RecipeDefinition {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(description, "description");
        Objects.requireNonNull(stationId, "stationId");
        Objects.requireNonNull(outputItemId, "outputItemId");
        Objects.requireNonNull(requiresModule, "requiresModule");
        inputs = Map.copyOf(new LinkedHashMap<>(Objects.requireNonNull(inputs, "inputs")));
        if (inputs.isEmpty()) {
            throw new IllegalArgumentException("Recipe " + id + " has no ingredients");
        }
        if (outputCount < 1) {
            throw new IllegalArgumentException("Recipe " + id + " produces nothing");
        }
    }
}
