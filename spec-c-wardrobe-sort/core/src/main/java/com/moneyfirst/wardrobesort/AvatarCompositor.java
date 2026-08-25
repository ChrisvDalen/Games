package com.moneyfirst.wardrobesort;

import java.util.Objects;

/**
 * Pure logic for applying a dragged {@link Garment} to a dropped-on
 * {@link GarmentSlot} on an {@link AvatarState}, and for deciding when the
 * avatar fully matches an {@link OutfitTarget} (round won). No rendering, no
 * input handling - just the rules.
 */
public final class AvatarCompositor {

    private AvatarCompositor() {
    }

    /** The three things that can happen when a garment is dropped on a slot. */
    public enum DropOutcome {
        /** The garment's own slot matches the required garment for the dropped-on slot: equipped. */
        ACCEPTED_CORRECT_MATCH,
        /** The garment's own slot doesn't even match the slot it was dropped on (e.g. a HEAD item on TOP). Rejected, no state change. */
        REJECTED_WRONG_SLOT_TYPE,
        /** The garment fits the slot type but isn't what this outfit target requires there (a decoy). Rejected, no state change. */
        REJECTED_WRONG_GARMENT
    }

    /**
     * Attempts to drop {@code garment} onto {@code dropSlot} of the avatar.
     * Only mutates {@code state} on {@link DropOutcome#ACCEPTED_CORRECT_MATCH}.
     */
    public static DropOutcome attemptDrop(AvatarState state, OutfitTarget target, Garment garment, GarmentSlot dropSlot) {
        Objects.requireNonNull(state, "state");
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(garment, "garment");
        Objects.requireNonNull(dropSlot, "dropSlot");

        if (garment.slot() != dropSlot) {
            return DropOutcome.REJECTED_WRONG_SLOT_TYPE;
        }

        Garment required = target.required(dropSlot);
        if (required != null && required.id().equals(garment.id())) {
            state.equip(dropSlot, garment.id());
            return DropOutcome.ACCEPTED_CORRECT_MATCH;
        }

        return DropOutcome.REJECTED_WRONG_GARMENT;
    }

    /** True once every slot the target requires is equipped with exactly the required garment. */
    public static boolean isRoundWon(AvatarState state, OutfitTarget target) {
        Objects.requireNonNull(state, "state");
        Objects.requireNonNull(target, "target");
        for (GarmentSlot slot : target.slots()) {
            Garment required = target.required(slot);
            String equipped = state.equippedGarmentId(slot);
            if (equipped == null || !equipped.equals(required.id())) {
                return false;
            }
        }
        return true;
    }
}
