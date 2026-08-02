package com.keplersharvest.inventory;

import java.util.Objects;
import java.util.Optional;

/**
 * Immutable description of an item type, loaded from {@code config/items.json}.
 *
 * @param id           stable identifier used by saves, recipes and quests
 * @param maxStack     how many fit in one inventory slot; tools are always 1
 * @param colour       placeholder render colour, {@code RRGGBB}
 * @param toolKind     present only for {@link ItemCategory#TOOL}
 * @param plantsCropId present only for {@link ItemCategory#SEED}
 */
public record ItemDefinition(
        String id,
        String name,
        String description,
        ItemCategory category,
        int maxStack,
        String colour,
        Optional<ToolKind> toolKind,
        Optional<String> plantsCropId) {

    public ItemDefinition {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(description, "description");
        Objects.requireNonNull(category, "category");
        Objects.requireNonNull(colour, "colour");
        Objects.requireNonNull(toolKind, "toolKind");
        Objects.requireNonNull(plantsCropId, "plantsCropId");
        if (id.isBlank()) {
            throw new IllegalArgumentException("Item id must not be blank");
        }
        if (maxStack < 1) {
            throw new IllegalArgumentException("Item " + id + " has maxStack " + maxStack);
        }
        if (category == ItemCategory.TOOL && maxStack != 1) {
            throw new IllegalArgumentException("Tool " + id + " must not stack");
        }
        if (category == ItemCategory.TOOL && toolKind.isEmpty()) {
            throw new IllegalArgumentException("Tool " + id + " has no toolKind");
        }
        if (category == ItemCategory.SEED && plantsCropId.isEmpty()) {
            throw new IllegalArgumentException("Seed " + id + " does not name a crop");
        }
    }

    public boolean stackable() {
        return maxStack > 1;
    }

    public boolean isTool() {
        return category == ItemCategory.TOOL;
    }
}
