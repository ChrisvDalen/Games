package com.keplersharvest.crafting;

import java.util.Map;

/** Outcome of running a recipe. Every failure says exactly what went wrong. */
public sealed interface CraftResult {

    String recipeId();

    record Crafted(String recipeId, String outputItemId, int amount) implements CraftResult {
    }

    /** Nothing was consumed; these amounts are still needed. */
    record MissingIngredients(String recipeId, Map<String, Integer> missing) implements CraftResult {
    }

    /** The output would not fit, so the ingredients were left alone. */
    record NoRoom(String recipeId, String outputItemId) implements CraftResult {
    }

    /** The recipe needs a module that is still offline. */
    record Locked(String recipeId, String requiredModuleId) implements CraftResult {
    }

    record UnknownRecipe(String recipeId) implements CraftResult {
    }

    /** The recipe belongs to a different bench. */
    record WrongStation(String recipeId, String stationId) implements CraftResult {
    }

    default boolean succeeded() {
        return this instanceof Crafted;
    }
}
