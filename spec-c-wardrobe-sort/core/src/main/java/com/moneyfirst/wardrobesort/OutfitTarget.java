package com.moneyfirst.wardrobesort;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * The "look" the player must assemble on the avatar this round: exactly one
 * required {@link Garment} per {@link GarmentSlot}.
 */
public final class OutfitTarget {

    private final Map<GarmentSlot, Garment> requiredBySlot;

    public OutfitTarget(Map<GarmentSlot, Garment> requiredBySlot) {
        Objects.requireNonNull(requiredBySlot, "requiredBySlot");
        EnumMap<GarmentSlot, Garment> copy = new EnumMap<>(GarmentSlot.class);
        for (Map.Entry<GarmentSlot, Garment> entry : requiredBySlot.entrySet()) {
            GarmentSlot slot = entry.getKey();
            Garment garment = entry.getValue();
            if (garment.slot() != slot) {
                throw new IllegalArgumentException(
                    "Garment " + garment.id() + " belongs to slot " + garment.slot()
                        + " but was mapped to " + slot);
            }
            copy.put(slot, garment);
        }
        this.requiredBySlot = Collections.unmodifiableMap(copy);
    }

    /** The garment required in the given slot, or {@code null} if this target doesn't use that slot. */
    public Garment required(GarmentSlot slot) {
        return requiredBySlot.get(slot);
    }

    public boolean requiresSlot(GarmentSlot slot) {
        return requiredBySlot.containsKey(slot);
    }

    public Set<GarmentSlot> slots() {
        return requiredBySlot.keySet();
    }

    public boolean isComplete() {
        return requiredBySlot.size() == GarmentSlot.values().length;
    }
}
