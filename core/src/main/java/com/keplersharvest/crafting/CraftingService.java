package com.keplersharvest.crafting;

import com.keplersharvest.inventory.Inventory;
import com.keplersharvest.inventory.ItemDefinition;
import com.keplersharvest.colony.ColonyState;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

/**
 * Runs recipes against an inventory.
 *
 * <p>Checks ingredients, the bench, module unlocks and output space *before* consuming anything, so
 * a refused craft always leaves the inventory exactly as it was.
 */
public final class CraftingService {

    private final Map<String, RecipeDefinition> recipes;
    private final Function<String, ItemDefinition> items;
    private final ColonyState colony;

    public CraftingService(Map<String, RecipeDefinition> recipes,
                           Function<String, ItemDefinition> items,
                           ColonyState colony) {
        this.recipes = Map.copyOf(Objects.requireNonNull(recipes, "recipes"));
        this.items = Objects.requireNonNull(items, "items");
        this.colony = Objects.requireNonNull(colony, "colony");
    }

    /** Recipes a given bench can run right now, including still-locked ones for display. */
    public List<RecipeDefinition> recipesFor(String stationId) {
        return recipes.values().stream()
                .filter(recipe -> recipe.stationId().equals(stationId))
                .toList();
    }

    public boolean unlocked(RecipeDefinition recipe) {
        return recipe.requiresModule().map(colony::isOnline).orElse(true);
    }

    public CraftResult craft(String recipeId, String stationId, Inventory inventory) {
        RecipeDefinition recipe = recipes.get(recipeId);
        if (recipe == null) {
            return new CraftResult.UnknownRecipe(recipeId);
        }
        if (!recipe.stationId().equals(stationId)) {
            return new CraftResult.WrongStation(recipeId, recipe.stationId());
        }
        if (!unlocked(recipe)) {
            return new CraftResult.Locked(recipeId, recipe.requiresModule().orElseThrow());
        }
        Map<String, Integer> missing = inventory.missingFrom(recipe.inputs());
        if (!missing.isEmpty()) {
            return new CraftResult.MissingIngredients(recipeId, missing);
        }
        ItemDefinition output = items.apply(recipe.outputItemId());
        if (output == null) {
            return new CraftResult.UnknownRecipe(recipeId);
        }
        // Ingredients leave the inventory first, which is what frees the space the output needs.
        inventory.removeAll(recipe.inputs());
        int leftover = inventory.add(output, recipe.outputCount());
        if (leftover > 0) {
            // Put everything back rather than destroying the surplus.
            inventory.remove(output.id(), recipe.outputCount() - leftover);
            recipe.inputs().forEach((itemId, count) -> {
                ItemDefinition ingredient = items.apply(itemId);
                if (ingredient != null) {
                    inventory.add(ingredient, count);
                }
            });
            return new CraftResult.NoRoom(recipeId, output.id());
        }
        return new CraftResult.Crafted(recipeId, output.id(), recipe.outputCount());
    }
}
