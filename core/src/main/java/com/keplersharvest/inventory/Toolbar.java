package com.keplersharvest.inventory;

import java.util.Objects;
import java.util.Optional;

/** A view over the first {@code size} inventory slots, with one of them selected. */
public final class Toolbar {

    private final Inventory inventory;
    private final int size;
    private int selectedIndex;

    public Toolbar(Inventory inventory, int size) {
        this.inventory = Objects.requireNonNull(inventory, "inventory");
        if (size < 1 || size > inventory.size()) {
            throw new IllegalArgumentException("Toolbar size " + size + " does not fit inventory " + inventory.size());
        }
        this.size = size;
    }

    public int size() {
        return size;
    }

    public int selectedIndex() {
        return selectedIndex;
    }

    public void select(int index) {
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException("Toolbar slot " + index + " outside 0.." + (size - 1));
        }
        this.selectedIndex = index;
    }

    public void scroll(int delta) {
        selectedIndex = Math.floorMod(selectedIndex + delta, size);
    }

    public Optional<ItemStack> selected() {
        return inventory.slot(selectedIndex);
    }

    public Optional<ItemDefinition> selectedItem() {
        return selected().map(ItemStack::item);
    }

    /** The tool the player is holding, if the selected slot holds one. */
    public Optional<ToolKind> equippedTool() {
        return selectedItem().flatMap(ItemDefinition::toolKind);
    }

    public Optional<ItemStack> slot(int index) {
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException("Toolbar slot " + index + " outside 0.." + (size - 1));
        }
        return inventory.slot(index);
    }
}
