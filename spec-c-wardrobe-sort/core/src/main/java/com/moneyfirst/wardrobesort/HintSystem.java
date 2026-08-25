package com.moneyfirst.wardrobesort;

import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Tracks how many outfit hints the player has (granted via rewarded video or
 * IAP - platform code decides how they're earned, this class only tracks the
 * balance and spends them). Spending a hint reveals one correct garment's
 * current position in the tray for a slot that hasn't been hinted yet this
 * round. The balance never goes negative.
 */
public final class HintSystem {

    private int hintsRemaining;
    private final Set<GarmentSlot> revealedThisRound = EnumSet.noneOf(GarmentSlot.class);

    public HintSystem(int startingHints) {
        this.hintsRemaining = Math.max(0, startingHints);
    }

    public int hintsRemaining() {
        return hintsRemaining;
    }

    /** Adds hints (e.g. after a rewarded-video watch or an IAP-hint purchase). Ignores non-positive amounts. */
    public void grantHints(int count) {
        if (count > 0) {
            hintsRemaining += count;
        }
    }

    /** Clears which slots have been hinted, for the start of a new round. Does not touch the hint balance. */
    public void resetForRound() {
        revealedThisRound.clear();
    }

    /**
     * Spends one hint, if available, to reveal a correct garment's tray
     * position for a slot not yet revealed this round. Returns
     * {@link Optional#empty()} - and leaves the hint balance untouched - when
     * there are no hints left, or when every slot has already been revealed
     * this round.
     */
    public Optional<HintReveal> useHint(OutfitTarget target, List<Garment> tray) {
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(tray, "tray");

        if (hintsRemaining <= 0) {
            return Optional.empty();
        }

        for (GarmentSlot slot : target.slots()) {
            if (revealedThisRound.contains(slot)) {
                continue;
            }
            Garment required = target.required(slot);
            int trayIndex = indexOfGarmentId(tray, required.id());
            if (trayIndex < 0) {
                // Shouldn't happen given RoundGenerator's guarantee, but never reveal a
                // position that doesn't exist.
                continue;
            }
            revealedThisRound.add(slot);
            hintsRemaining--;
            return Optional.of(new HintReveal(slot, required.id(), trayIndex));
        }

        return Optional.empty();
    }

    private static int indexOfGarmentId(List<Garment> tray, String garmentId) {
        for (int i = 0; i < tray.size(); i++) {
            if (tray.get(i).id().equals(garmentId)) {
                return i;
            }
        }
        return -1;
    }
}
