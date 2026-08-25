package com.moneyfirst.wardrobesort;

import java.util.EnumMap;
import java.util.Map;

/**
 * Mutable "what's currently equipped" state for the avatar being dressed
 * this round. Populated exclusively through {@link AvatarCompositor}, which
 * enforces the drop rules; this class itself is a plain state holder.
 */
public final class AvatarState {

    private final Map<GarmentSlot, String> equippedGarmentIdBySlot = new EnumMap<>(GarmentSlot.class);

    void equip(GarmentSlot slot, String garmentId) {
        equippedGarmentIdBySlot.put(slot, garmentId);
    }

    public String equippedGarmentId(GarmentSlot slot) {
        return equippedGarmentIdBySlot.get(slot);
    }

    public boolean isFilled(GarmentSlot slot) {
        return equippedGarmentIdBySlot.containsKey(slot);
    }

    public void clear() {
        equippedGarmentIdBySlot.clear();
    }

    public void unequip(GarmentSlot slot) {
        equippedGarmentIdBySlot.remove(slot);
    }
}
