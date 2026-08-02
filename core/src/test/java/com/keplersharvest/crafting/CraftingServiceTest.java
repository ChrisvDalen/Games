package com.keplersharvest.crafting;

import com.keplersharvest.colony.ColonyState;
import com.keplersharvest.configuration.GameContent;
import com.keplersharvest.inventory.Inventory;
import com.keplersharvest.testing.TestContent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CraftingServiceTest {

    private GameContent content;
    private ColonyState colony;
    private CraftingService crafting;
    private Inventory inventory;

    @BeforeEach
    void setUp() {
        content = TestContent.load();
        colony = new ColonyState(content.modules().values());
        crafting = new CraftingService(content.recipes(), id -> content.item(id).orElse(null), colony);
        inventory = new Inventory(24);
    }

    private void stock(Map<String, Integer> items) {
        items.forEach((id, count) -> inventory.add(content.requireItem(id), count));
    }

    private void bringOnline(String moduleId) {
        var definition = content.module(moduleId).orElseThrow();
        Inventory parts = new Inventory(40);
        for (int i = 0; i < definition.stageCount(); i++) {
            definition.stage(i).materials().forEach((id, count) -> parts.add(content.requireItem(id), count));
            colony.repairNextStage(moduleId, parts);
        }
    }

    @Test
    @DisplayName("a recipe with everything in the pack produces its output")
    void craftsWhenStocked() {
        RecipeDefinition recipe = content.recipes().get("filter_mesh");
        stock(recipe.inputs());

        CraftResult result = crafting.craft("filter_mesh", "fabricator", inventory);

        CraftResult.Crafted crafted = assertInstanceOf(CraftResult.Crafted.class, result);
        assertEquals("filter_mesh", crafted.outputItemId());
        assertEquals(1, inventory.count("filter_mesh"));
        assertEquals(0, inventory.count("resin_fibre"), "ingredients are consumed");
    }

    @Test
    @DisplayName("missing ingredients are named and nothing is consumed")
    void reportsMissingIngredients() {
        inventory.add(content.requireItem("resin_fibre"), 3);

        CraftResult result = crafting.craft("filter_mesh", "fabricator", inventory);

        CraftResult.MissingIngredients missing =
                assertInstanceOf(CraftResult.MissingIngredients.class, result);
        assertEquals(Map.of("silica_shard", 2), missing.missing());
        assertEquals(3, inventory.count("resin_fibre"), "a refused craft leaves the pack untouched");
        assertEquals(0, inventory.count("filter_mesh"));
    }

    @Test
    @DisplayName("a module-gated recipe stays locked until the module is online")
    void modulesGateRecipes() {
        RecipeDefinition gated = content.recipes().get("orbit_berry_seed");
        assertTrue(gated.requiresModule().isPresent(), "this test needs a gated recipe");
        stock(gated.inputs());

        CraftResult locked = crafting.craft(gated.id(), "fabricator", inventory);
        assertInstanceOf(CraftResult.Locked.class, locked);
        assertFalse(crafting.unlocked(gated));

        bringOnline(gated.requiresModule().orElseThrow());

        assertTrue(crafting.unlocked(gated));
        assertInstanceOf(CraftResult.Crafted.class, crafting.craft(gated.id(), "fabricator", inventory));
    }

    @Test
    @DisplayName("running a recipe at the wrong bench is refused")
    void wrongStationIsRefused() {
        stock(content.recipes().get("filter_mesh").inputs());

        CraftResult result = crafting.craft("filter_mesh", "seed_press", inventory);

        assertInstanceOf(CraftResult.WrongStation.class, result);
        assertEquals(3, inventory.count("resin_fibre"));
    }

    @Test
    @DisplayName("an unknown recipe is reported")
    void unknownRecipeIsReported() {
        assertInstanceOf(CraftResult.UnknownRecipe.class,
                crafting.craft("antimatter_pie", "fabricator", inventory));
    }

    @Test
    @DisplayName("a craft with nowhere to put the output puts the ingredients back")
    void refusesWhenThereIsNoRoom() {
        Inventory tiny = new Inventory(2);
        RecipeDefinition recipe = content.recipes().get("filter_mesh");
        // Both slots hold ingredients; consuming them frees space, so fill up with a third item type.
        tiny.add(content.requireItem("resin_fibre"), 3);
        tiny.add(content.requireItem("silica_shard"), 2);

        CraftResult result = crafting.craft("filter_mesh", "fabricator", tiny);

        // Two slots free up and one is needed, so this particular craft does fit.
        assertInstanceOf(CraftResult.Crafted.class, result);
        assertEquals(1, tiny.count("filter_mesh"));
        assertEquals(recipe.outputCount(), tiny.count(recipe.outputItemId()));
    }

    @Test
    @DisplayName("the bench lists only its own recipes")
    void listsRecipesPerStation() {
        assertFalse(crafting.recipesFor("fabricator").isEmpty());
        assertTrue(crafting.recipesFor("nonexistent_bench").isEmpty());
        crafting.recipesFor("fabricator")
                .forEach(recipe -> assertEquals("fabricator", recipe.stationId()));
    }
}
