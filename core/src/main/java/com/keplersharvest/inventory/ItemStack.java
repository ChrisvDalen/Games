package com.keplersharvest.inventory;

import java.util.Objects;

/**
 * A non-empty pile of one item type. Immutable: mutating an inventory slot means replacing the
 * stack, which keeps illegal intermediate states unrepresentable.
 */
public record ItemStack(ItemDefinition item, int count) {

    public ItemStack {
        Objects.requireNonNull(item, "item");
        if (count < 1) {
            throw new IllegalArgumentException("Stack of " + item.id() + " must hold at least 1, got " + count);
        }
        if (count > item.maxStack()) {
            throw new IllegalArgumentException(
                    "Stack of " + item.id() + " exceeds max " + item.maxStack() + ", got " + count);
        }
    }

    public static ItemStack of(ItemDefinition item, int count) {
        return new ItemStack(item, count);
    }

    public static ItemStack one(ItemDefinition item) {
        return new ItemStack(item, 1);
    }

    /** Room left before this stack is full. */
    public int spaceLeft() {
        return item.maxStack() - count;
    }

    public boolean isFull() {
        return spaceLeft() == 0;
    }

    public boolean holds(String itemId) {
        return item.id().equals(itemId);
    }

    public ItemStack withCount(int newCount) {
        return new ItemStack(item, newCount);
    }

    public ItemStack plus(int amount) {
        return withCount(count + amount);
    }
}
