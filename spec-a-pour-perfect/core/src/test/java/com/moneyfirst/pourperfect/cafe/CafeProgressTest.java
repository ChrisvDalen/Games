package com.moneyfirst.pourperfect.cafe;

import com.badlogic.gdx.Preferences;
import com.moneyfirst.pourperfect.fakes.InMemoryPreferences;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CafeProgressTest {

    @Test
    void freshProgressIsClosedWithNoCupsOrUpgrades() {
        CafeProgress progress = CafeProgress.load(new InMemoryPreferences());

        assertFalse(progress.isOpen());
        assertEquals(0, progress.getCups());
        assertTrue(progress.getUnlockedUpgradeIds().isEmpty());
    }

    @Test
    void earningCupsAccumulatesByFixedAmountPerSolve() {
        CafeProgress progress = CafeProgress.load(new InMemoryPreferences());

        progress.earnCupsForSolve();
        progress.earnCupsForSolve();

        assertEquals(2 * CafeProgress.CUPS_PER_SOLVE, progress.getCups());
    }

    @Test
    void cafeOpensWhenExpansionIsOwnedRegardlessOfLevel() {
        CafeProgress progress = CafeProgress.load(new InMemoryPreferences());

        progress.refreshEligibility(true, 1);

        assertTrue(progress.isOpen());
    }

    @Test
    void cafeOpensAtLevelTwentyWithoutTheIap() {
        CafeProgress progress = CafeProgress.load(new InMemoryPreferences());

        progress.refreshEligibility(false, 19);
        assertFalse(progress.isOpen());

        progress.refreshEligibility(false, 20);
        assertTrue(progress.isOpen());
    }

    @Test
    void onceOpenStaysOpenEvenIfEligibilityIsRecheckedAndNoLongerMet() {
        CafeProgress progress = CafeProgress.load(new InMemoryPreferences());
        progress.refreshEligibility(true, 1);
        assertTrue(progress.isOpen());

        progress.refreshEligibility(false, 1); // e.g. a refunded IAP re-check
        assertTrue(progress.isOpen(), "cafe should not re-lock once opened");
    }

    @Test
    void purchaseUpgradeFailsWhenCafeIsClosed() {
        CafeProgress progress = CafeProgress.load(new InMemoryPreferences());
        for (int i = 0; i < 10; i++) {
            progress.earnCupsForSolve();
        }

        boolean purchased = progress.purchaseUpgrade(CafeUpgrade.WARM_LIGHTING);

        assertFalse(purchased);
        assertEquals(10 * CafeProgress.CUPS_PER_SOLVE, progress.getCups(), "failed purchase must not spend cups");
    }

    @Test
    void purchaseUpgradeFailsWithInsufficientCups() {
        CafeProgress progress = CafeProgress.load(new InMemoryPreferences());
        progress.refreshEligibility(true, 1);
        progress.earnCupsForSolve(); // far short of WARM_LIGHTING's cost

        assertFalse(progress.purchaseUpgrade(CafeUpgrade.WARM_LIGHTING));
        assertFalse(progress.isUnlocked(CafeUpgrade.WARM_LIGHTING));
    }

    @Test
    void purchaseUpgradeSucceedsAndDeductsCups() {
        CafeProgress progress = CafeProgress.load(new InMemoryPreferences());
        progress.refreshEligibility(true, 1);
        for (int i = 0; i < CafeUpgrade.WARM_LIGHTING.getCostInCups(); i++) {
            progress.earnCupsForSolve();
        }
        int cupsBefore = progress.getCups();

        boolean purchased = progress.purchaseUpgrade(CafeUpgrade.WARM_LIGHTING);

        assertTrue(purchased);
        assertTrue(progress.isUnlocked(CafeUpgrade.WARM_LIGHTING));
        assertEquals(cupsBefore - CafeUpgrade.WARM_LIGHTING.getCostInCups(), progress.getCups());
    }

    @Test
    void cannotPurchaseTheSameUpgradeTwice() {
        CafeProgress progress = CafeProgress.load(new InMemoryPreferences());
        progress.refreshEligibility(true, 1);
        for (int i = 0; i < 50; i++) {
            progress.earnCupsForSolve();
        }
        assertTrue(progress.purchaseUpgrade(CafeUpgrade.WARM_LIGHTING));
        int cupsAfterFirst = progress.getCups();

        boolean secondAttempt = progress.purchaseUpgrade(CafeUpgrade.WARM_LIGHTING);

        assertFalse(secondAttempt);
        assertEquals(cupsAfterFirst, progress.getCups());
    }

    @Test
    void saveThenLoadRoundTripsCupsOpenFlagAndUnlockedUpgrades() {
        Preferences prefs = new InMemoryPreferences();
        CafeProgress progress = CafeProgress.load(prefs);
        progress.refreshEligibility(true, 1);
        for (int i = 0; i < 50; i++) {
            progress.earnCupsForSolve();
        }
        progress.purchaseUpgrade(CafeUpgrade.WARM_LIGHTING);
        progress.save();

        CafeProgress reloaded = CafeProgress.load(prefs);

        assertEquals(progress.getCups(), reloaded.getCups());
        assertTrue(reloaded.isOpen());
        assertTrue(reloaded.isUnlocked(CafeUpgrade.WARM_LIGHTING));
    }
}
