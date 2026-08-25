package com.moneyfirst.wardrobesort;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HintSystemTest {

    private final RoundGenerator generator = RoundGenerator.withBaseCatalog();

    @Test
    void neverGoesNegativeWhenSpentWithoutBalance() {
        HintSystem hints = new HintSystem(0);

        Round round = generator.generateRound(1, 1L);
        Optional<HintReveal> reveal = hints.useHint(round.target(), round.tray());

        assertTrue(reveal.isEmpty());
        assertEquals(0, hints.hintsRemaining());
    }

    @Test
    void constructorClampsNegativeStartingHintsToZero() {
        HintSystem hints = new HintSystem(-5);
        assertEquals(0, hints.hintsRemaining());
    }

    @Test
    void spendingAHintDecrementsBalanceAndNeverUndershootsZero() {
        HintSystem hints = new HintSystem(2);
        Round round = generator.generateRound(3, 5L);

        assertTrue(hints.useHint(round.target(), round.tray()).isPresent());
        assertEquals(1, hints.hintsRemaining());
        assertTrue(hints.useHint(round.target(), round.tray()).isPresent());
        assertEquals(0, hints.hintsRemaining());

        // A third spend attempt with zero balance must not go negative.
        Optional<HintReveal> third = hints.useHint(round.target(), round.tray());
        assertTrue(third.isEmpty());
        assertEquals(0, hints.hintsRemaining());
    }

    @Test
    void revealedGarmentIsActuallyAtTheReportedTrayIndex() {
        HintSystem hints = new HintSystem(5);
        Round round = generator.generateRound(4, 77L);

        Optional<HintReveal> reveal = hints.useHint(round.target(), round.tray());

        assertTrue(reveal.isPresent());
        HintReveal r = reveal.get();
        Garment required = round.target().required(r.slot());
        assertEquals(required.id(), r.garmentId(), "hint must reveal the garment the target actually requires for that slot");
        List<Garment> tray = round.tray();
        assertTrue(r.trayIndex() >= 0 && r.trayIndex() < tray.size());
        assertEquals(r.garmentId(), tray.get(r.trayIndex()).id(),
            "the tray index the hint reports must actually hold the revealed garment");
    }

    @Test
    void neverRevealsTheSameSlotTwiceInOneRound() {
        HintSystem hints = new HintSystem(10);
        Round round = generator.generateRound(2, 9L);

        Set<GarmentSlot> revealedSlots = new HashSet<>();
        for (int i = 0; i < GarmentSlot.values().length; i++) {
            Optional<HintReveal> reveal = hints.useHint(round.target(), round.tray());
            assertTrue(reveal.isPresent());
            assertTrue(revealedSlots.add(reveal.get().slot()), "slot " + reveal.get().slot() + " was revealed twice in one round");
        }

        // Every slot has now been revealed; a further hint should report nothing more to reveal,
        // and critically must not consume a hint it can't fulfil.
        int balanceBeforeExtra = hints.hintsRemaining();
        Optional<HintReveal> extra = hints.useHint(round.target(), round.tray());
        assertTrue(extra.isEmpty());
        assertEquals(balanceBeforeExtra, hints.hintsRemaining());
    }

    @Test
    void resetForRoundAllowsSlotsToBeRevealedAgainNextRound() {
        HintSystem hints = new HintSystem(10);
        Round round1 = generator.generateRound(1, 3L);
        for (int i = 0; i < GarmentSlot.values().length; i++) {
            hints.useHint(round1.target(), round1.tray());
        }
        int balanceBeforeReset = hints.hintsRemaining();

        hints.resetForRound();

        Round round2 = generator.generateRound(2, 3L);
        Optional<HintReveal> reveal = hints.useHint(round2.target(), round2.tray());
        assertTrue(reveal.isPresent(), "after resetForRound, hints should be spendable again");
        assertEquals(balanceBeforeReset - 1, hints.hintsRemaining());
    }

    @Test
    void grantHintsIncreasesBalanceAndIgnoresNonPositiveAmounts() {
        HintSystem hints = new HintSystem(1);
        hints.grantHints(3);
        assertEquals(4, hints.hintsRemaining());

        hints.grantHints(0);
        hints.grantHints(-2);
        assertEquals(4, hints.hintsRemaining(), "granting zero/negative hints must not change the balance");
    }
}
