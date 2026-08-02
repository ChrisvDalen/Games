package com.keplersharvest.inventory;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InventoryTest {

    private static ItemDefinition stackable(String id, int maxStack) {
        return new ItemDefinition(id, id, "", ItemCategory.RESOURCE, maxStack, "ffffff",
                Optional.empty(), Optional.empty());
    }

    private static ItemDefinition tool(String id) {
        return new ItemDefinition(id, id, "", ItemCategory.TOOL, 1, "ffffff",
                Optional.of(ToolKind.TILLER), Optional.empty());
    }

    @Test
    @DisplayName("adding fills existing stacks before opening a new slot")
    void topsUpExistingStacks() {
        Inventory inventory = new Inventory(4);
        ItemDefinition alloy = stackable("scrap_alloy", 10);

        assertEquals(0, inventory.add(alloy, 6));
        assertEquals(0, inventory.add(alloy, 3));

        assertEquals(9, inventory.count("scrap_alloy"));
        assertEquals(1, inventory.stacks().size(), "nine of a ten-stack item is still one slot");
    }

    @Test
    @DisplayName("a full stack spills into the next slot")
    void spillsIntoNextSlot() {
        Inventory inventory = new Inventory(4);
        ItemDefinition alloy = stackable("scrap_alloy", 10);

        assertEquals(0, inventory.add(alloy, 25));

        assertEquals(25, inventory.count("scrap_alloy"));
        assertEquals(3, inventory.stacks().size());
    }

    @Test
    @DisplayName("adding beyond capacity reports the leftover instead of losing it")
    void reportsLeftover() {
        Inventory inventory = new Inventory(2);
        ItemDefinition alloy = stackable("scrap_alloy", 10);

        int leftover = inventory.add(alloy, 25);

        assertEquals(5, leftover);
        assertEquals(20, inventory.count("scrap_alloy"));
    }

    @Test
    @DisplayName("tools never stack")
    void toolsDoNotStack() {
        Inventory inventory = new Inventory(4);
        ItemDefinition blade = tool("soil_blade");

        inventory.add(blade, 3);

        assertEquals(3, inventory.count("soil_blade"));
        assertEquals(3, inventory.stacks().size(), "each tool takes its own slot");
    }

    @Test
    @DisplayName("a stack cannot hold more than its maximum, or fewer than one")
    void stackBoundsAreEnforced() {
        ItemDefinition alloy = stackable("scrap_alloy", 10);

        assertThrows(IllegalArgumentException.class, () -> ItemStack.of(alloy, 11));
        assertThrows(IllegalArgumentException.class, () -> ItemStack.of(alloy, 0));
        assertThrows(IllegalArgumentException.class, () -> ItemStack.of(alloy, -3));
    }

    @Test
    @DisplayName("removing more than is held changes nothing")
    void removalIsAllOrNothing() {
        Inventory inventory = new Inventory(4);
        ItemDefinition alloy = stackable("scrap_alloy", 10);
        inventory.add(alloy, 5);

        assertFalse(inventory.remove("scrap_alloy", 6));
        assertEquals(5, inventory.count("scrap_alloy"), "a refused removal must not take a partial amount");

        assertTrue(inventory.remove("scrap_alloy", 5));
        assertEquals(0, inventory.count("scrap_alloy"));
        assertTrue(inventory.isEmpty());
    }

    @Test
    @DisplayName("removing an ingredient list is atomic")
    void removeAllIsAtomic() {
        Inventory inventory = new Inventory(6);
        inventory.add(stackable("scrap_alloy", 20), 4);
        inventory.add(stackable("resin_fibre", 20), 1);

        Map<String, Integer> recipe = new LinkedHashMap<>();
        recipe.put("scrap_alloy", 4);
        recipe.put("resin_fibre", 2);

        assertFalse(inventory.removeAll(recipe));
        assertEquals(4, inventory.count("scrap_alloy"), "the alloy must survive a failed removal");
        assertEquals(1, inventory.count("resin_fibre"));

        assertEquals(Map.of("resin_fibre", 1), inventory.missingFrom(recipe));

        inventory.add(stackable("resin_fibre", 20), 1);
        assertTrue(inventory.removeAll(recipe));
        assertTrue(inventory.isEmpty());
    }

    @Test
    @DisplayName("canFit accounts for partial stacks and empty slots")
    void canFitIsAccurate() {
        Inventory inventory = new Inventory(2);
        ItemDefinition alloy = stackable("scrap_alloy", 10);
        inventory.add(alloy, 8);

        assertTrue(inventory.canFit(alloy, 12), "2 left in the open stack plus a whole free slot");
        assertFalse(inventory.canFit(alloy, 13));
    }

    @Test
    @DisplayName("slot indices outside the pack are rejected")
    void slotBoundsAreChecked() {
        Inventory inventory = new Inventory(3);
        assertThrows(IndexOutOfBoundsException.class, () -> inventory.slot(3));
        assertThrows(IndexOutOfBoundsException.class, () -> inventory.slot(-1));
    }

    @Test
    @DisplayName("the toolbar reads through to the inventory and reports the equipped tool")
    void toolbarTracksInventory() {
        Inventory inventory = new Inventory(10);
        inventory.setSlot(0, ItemStack.one(tool("soil_blade")));
        inventory.setSlot(1, ItemStack.of(stackable("scrap_alloy", 10), 4));
        Toolbar toolbar = new Toolbar(inventory, 8);

        assertEquals(Optional.of(ToolKind.TILLER), toolbar.equippedTool());

        toolbar.select(1);
        assertEquals(Optional.empty(), toolbar.equippedTool(), "alloy is not a tool");
        assertEquals("scrap_alloy", toolbar.selectedItem().orElseThrow().id());

        toolbar.scroll(-1);
        assertEquals(0, toolbar.selectedIndex());
        toolbar.scroll(-1);
        assertEquals(7, toolbar.selectedIndex(), "scrolling wraps around");

        assertThrows(IndexOutOfBoundsException.class, () -> toolbar.select(8));
    }
}
