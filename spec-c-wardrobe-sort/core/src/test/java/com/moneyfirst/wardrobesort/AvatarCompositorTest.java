package com.moneyfirst.wardrobesort;

import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AvatarCompositorTest {

    private static OutfitTarget fullTarget() {
        Map<GarmentSlot, Garment> req = new EnumMap<>(GarmentSlot.class);
        req.put(GarmentSlot.HEAD, byId("head_cap"));
        req.put(GarmentSlot.TOP, byId("top_tshirt"));
        req.put(GarmentSlot.BOTTOM, byId("bottom_jeans"));
        req.put(GarmentSlot.SHOES, byId("shoes_sneakers"));
        req.put(GarmentSlot.ACCESSORY, byId("acc_scarf"));
        return new OutfitTarget(req);
    }

    private static Garment byId(String id) {
        Garment g = GarmentCatalog.findById(id);
        if (g == null) {
            throw new IllegalStateException("test fixture references unknown garment id " + id);
        }
        return g;
    }

    @Test
    void correctGarmentOnCorrectSlotIsAcceptedAndEquipped() {
        AvatarState state = new AvatarState();
        OutfitTarget target = fullTarget();
        Garment cap = byId("head_cap");

        AvatarCompositor.DropOutcome outcome = AvatarCompositor.attemptDrop(state, target, cap, GarmentSlot.HEAD);

        assertEquals(AvatarCompositor.DropOutcome.ACCEPTED_CORRECT_MATCH, outcome);
        assertEquals("head_cap", state.equippedGarmentId(GarmentSlot.HEAD));
    }

    @Test
    void garmentDroppedOnAStructurallyWrongSlotIsRejectedAndStateUnchanged() {
        AvatarState state = new AvatarState();
        OutfitTarget target = fullTarget();
        Garment cap = byId("head_cap"); // a HEAD garment...

        // ...dropped on the TOP slot: structurally impossible, must be rejected.
        AvatarCompositor.DropOutcome outcome = AvatarCompositor.attemptDrop(state, target, cap, GarmentSlot.TOP);

        assertEquals(AvatarCompositor.DropOutcome.REJECTED_WRONG_SLOT_TYPE, outcome);
        assertNull(state.equippedGarmentId(GarmentSlot.TOP));
        assertNull(state.equippedGarmentId(GarmentSlot.HEAD));
    }

    @Test
    void decoyGarmentOnItsOwnSlotTypeButWrongForTargetIsRejectedAndStateUnchanged() {
        AvatarState state = new AvatarState();
        OutfitTarget target = fullTarget(); // wants head_cap
        Garment decoyHead = byId("head_beanie"); // a HEAD item, but not the target's HEAD requirement

        AvatarCompositor.DropOutcome outcome = AvatarCompositor.attemptDrop(state, target, decoyHead, GarmentSlot.HEAD);

        assertEquals(AvatarCompositor.DropOutcome.REJECTED_WRONG_GARMENT, outcome);
        assertNull(state.equippedGarmentId(GarmentSlot.HEAD));
    }

    @Test
    void roundIsNotWonUntilEverySlotHasItsRequiredGarment() {
        AvatarState state = new AvatarState();
        OutfitTarget target = fullTarget();

        assertFalse(AvatarCompositor.isRoundWon(state, target));

        AvatarCompositor.attemptDrop(state, target, byId("head_cap"), GarmentSlot.HEAD);
        AvatarCompositor.attemptDrop(state, target, byId("top_tshirt"), GarmentSlot.TOP);
        AvatarCompositor.attemptDrop(state, target, byId("bottom_jeans"), GarmentSlot.BOTTOM);
        AvatarCompositor.attemptDrop(state, target, byId("shoes_sneakers"), GarmentSlot.SHOES);
        assertFalse(AvatarCompositor.isRoundWon(state, target), "missing accessory - round should not be won yet");

        AvatarCompositor.attemptDrop(state, target, byId("acc_scarf"), GarmentSlot.ACCESSORY);
        assertTrue(AvatarCompositor.isRoundWon(state, target), "all five slots correctly filled - round should be won");
    }

    @Test
    void rejectedDropsNeverFlipRoundToWon() {
        AvatarState state = new AvatarState();
        OutfitTarget target = fullTarget();

        AvatarCompositor.attemptDrop(state, target, byId("head_cap"), GarmentSlot.HEAD);
        AvatarCompositor.attemptDrop(state, target, byId("top_tshirt"), GarmentSlot.TOP);
        AvatarCompositor.attemptDrop(state, target, byId("bottom_jeans"), GarmentSlot.BOTTOM);
        AvatarCompositor.attemptDrop(state, target, byId("shoes_sneakers"), GarmentSlot.SHOES);
        // Wrong accessory decoy instead of the correct one.
        AvatarCompositor.attemptDrop(state, target, byId("acc_watch"), GarmentSlot.ACCESSORY);

        assertFalse(AvatarCompositor.isRoundWon(state, target));
    }
}
