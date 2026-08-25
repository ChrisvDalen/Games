package com.moneyfirst.wardrobesort;

import java.util.Objects;

/**
 * The result of spending a hint: which slot it revealed the answer for, the
 * id of the garment that slot needs, and that garment's current index in the
 * tray so the UI can highlight it.
 */
public final class HintReveal {

    private final GarmentSlot slot;
    private final String garmentId;
    private final int trayIndex;

    public HintReveal(GarmentSlot slot, String garmentId, int trayIndex) {
        this.slot = Objects.requireNonNull(slot, "slot");
        this.garmentId = Objects.requireNonNull(garmentId, "garmentId");
        this.trayIndex = trayIndex;
    }

    public GarmentSlot slot() {
        return slot;
    }

    public String garmentId() {
        return garmentId;
    }

    public int trayIndex() {
        return trayIndex;
    }
}
