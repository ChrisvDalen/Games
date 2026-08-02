package com.keplersharvest.inventory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Fixed-size slot container.
 *
 * <p>Removal is all-or-nothing: {@link #remove} and {@link #removeAll} either take every requested
 * item or change nothing, so a failed craft or repair can never eat half the ingredients.
 */
public final class Inventory {

    private final ItemStack[] slots;

    public Inventory(int size) {
        if (size < 1) {
            throw new IllegalArgumentException("Inventory size must be positive, got " + size);
        }
        this.slots = new ItemStack[size];
    }

    public int size() {
        return slots.length;
    }

    public Optional<ItemStack> slot(int index) {
        checkIndex(index);
        return Optional.ofNullable(slots[index]);
    }

    /** Replaces a slot outright. Used by save loading and by the UI when moving stacks. */
    public void setSlot(int index, ItemStack stack) {
        checkIndex(index);
        slots[index] = stack;
    }

    public void clearSlot(int index) {
        setSlot(index, null);
    }

    public void clear() {
        java.util.Arrays.fill(slots, null);
    }

    /**
     * Adds up to {@code count} items, topping up matching stacks before using empty slots.
     *
     * @return the number that did not fit
     */
    public int add(ItemDefinition item, int count) {
        Objects.requireNonNull(item, "item");
        if (count < 0) {
            throw new IllegalArgumentException("Cannot add a negative amount: " + count);
        }
        int remaining = count;
        if (item.stackable()) {
            for (int i = 0; i < slots.length && remaining > 0; i++) {
                ItemStack stack = slots[i];
                if (stack != null && stack.holds(item.id()) && !stack.isFull()) {
                    int moved = Math.min(remaining, stack.spaceLeft());
                    slots[i] = stack.plus(moved);
                    remaining -= moved;
                }
            }
        }
        for (int i = 0; i < slots.length && remaining > 0; i++) {
            if (slots[i] == null) {
                int moved = Math.min(remaining, item.maxStack());
                slots[i] = ItemStack.of(item, moved);
                remaining -= moved;
            }
        }
        return remaining;
    }

    /** True when {@link #add} would place every item. */
    public boolean canFit(ItemDefinition item, int count) {
        int capacity = 0;
        for (ItemStack stack : slots) {
            if (stack == null) {
                capacity += item.maxStack();
            } else if (item.stackable() && stack.holds(item.id())) {
                capacity += stack.spaceLeft();
            }
            if (capacity >= count) {
                return true;
            }
        }
        return capacity >= count;
    }

    public int count(String itemId) {
        int total = 0;
        for (ItemStack stack : slots) {
            if (stack != null && stack.holds(itemId)) {
                total += stack.count();
            }
        }
        return total;
    }

    public boolean has(String itemId, int count) {
        return count <= 0 || count(itemId) >= count;
    }

    public boolean hasAll(Map<String, Integer> required) {
        return required.entrySet().stream().allMatch(e -> has(e.getKey(), e.getValue()));
    }

    /** Removes {@code count} of {@code itemId}, or nothing at all if there are too few. */
    public boolean remove(String itemId, int count) {
        if (count < 0) {
            throw new IllegalArgumentException("Cannot remove a negative amount: " + count);
        }
        if (!has(itemId, count)) {
            return false;
        }
        int remaining = count;
        for (int i = 0; i < slots.length && remaining > 0; i++) {
            ItemStack stack = slots[i];
            if (stack == null || !stack.holds(itemId)) {
                continue;
            }
            int taken = Math.min(remaining, stack.count());
            remaining -= taken;
            slots[i] = taken == stack.count() ? null : stack.withCount(stack.count() - taken);
        }
        return true;
    }

    /** Atomically removes a whole ingredient list. */
    public boolean removeAll(Map<String, Integer> required) {
        if (!hasAll(required)) {
            return false;
        }
        required.forEach(this::remove);
        return true;
    }

    /** Amounts still needed to satisfy {@code required}; empty when the inventory can pay. */
    public Map<String, Integer> missingFrom(Map<String, Integer> required) {
        Map<String, Integer> missing = new LinkedHashMap<>();
        required.forEach((itemId, needed) -> {
            int shortfall = needed - count(itemId);
            if (shortfall > 0) {
                missing.put(itemId, shortfall);
            }
        });
        return missing;
    }

    public List<ItemStack> stacks() {
        List<ItemStack> result = new ArrayList<>();
        for (ItemStack stack : slots) {
            if (stack != null) {
                result.add(stack);
            }
        }
        return List.copyOf(result);
    }

    public boolean isEmpty() {
        return stacks().isEmpty();
    }

    /** Index of the first slot holding {@code itemId}, or -1. */
    public int findSlot(String itemId) {
        for (int i = 0; i < slots.length; i++) {
            if (slots[i] != null && slots[i].holds(itemId)) {
                return i;
            }
        }
        return -1;
    }

    private void checkIndex(int index) {
        if (index < 0 || index >= slots.length) {
            throw new IndexOutOfBoundsException("Slot " + index + " outside 0.." + (slots.length - 1));
        }
    }
}
