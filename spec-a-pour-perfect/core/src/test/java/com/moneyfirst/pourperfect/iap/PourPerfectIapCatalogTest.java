package com.moneyfirst.pourperfect.iap;

import com.moneyfirst.pourperfect.fakes.InMemoryPreferences;
import com.moneyfirst.pourperfect.state.LevelProgressionState;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PourPerfectIapCatalogTest {

    @Test
    void removeAdsSetsAdsRemovedFlag() {
        LevelProgressionState state = LevelProgressionState.load(new InMemoryPreferences());
        assertFalse(state.isAdsRemoved());

        PourPerfectIapCatalog.REMOVE_ADS.apply(state);

        assertTrue(state.isAdsRemoved());
    }

    @Test
    void starterPackGrantsFiftyHintsAndTenUndos() {
        LevelProgressionState state = LevelProgressionState.load(new InMemoryPreferences());
        int hintsBefore = state.getHintsRemaining();
        int undosBefore = state.getUndosRemaining();

        PourPerfectIapCatalog.STARTER_PACK.apply(state);

        assertEquals(hintsBefore + 50, state.getHintsRemaining());
        assertEquals(undosBefore + 10, state.getUndosRemaining());
    }

    @Test
    void cafeExpansionUnlocksTheCafe() {
        LevelProgressionState state = LevelProgressionState.load(new InMemoryPreferences());
        assertFalse(state.isCafeUnlocked());

        PourPerfectIapCatalog.CAFE_EXPANSION.apply(state);

        assertTrue(state.isCafeUnlocked());
    }

    @Test
    void productIdsMatchTheStoreListingConvention() {
        assertEquals("pp_remove_ads", PourPerfectIapCatalog.REMOVE_ADS.getProductId());
        assertEquals("pp_starter_pack", PourPerfectIapCatalog.STARTER_PACK.getProductId());
        assertEquals("pp_cafe_expansion", PourPerfectIapCatalog.CAFE_EXPANSION.getProductId());
    }

    @Test
    void byProductIdResolvesKnownIdsAndIsEmptyForUnknownOnes() {
        Optional<PourPerfectIapCatalog> found = PourPerfectIapCatalog.byProductId("pp_remove_ads");
        assertTrue(found.isPresent());
        assertEquals(PourPerfectIapCatalog.REMOVE_ADS, found.get());

        assertTrue(PourPerfectIapCatalog.byProductId("does_not_exist").isEmpty());
    }

    @Test
    void everyProductHasANonBlankPriceHint() {
        for (PourPerfectIapCatalog product : PourPerfectIapCatalog.values()) {
            assertFalse(product.getPriceHint().isBlank(), product + " must have a display price hint");
        }
    }

    @Test
    void applyingRemoveAdsTwiceStaysIdempotent() {
        LevelProgressionState state = LevelProgressionState.load(new InMemoryPreferences());

        PourPerfectIapCatalog.REMOVE_ADS.apply(state);
        PourPerfectIapCatalog.REMOVE_ADS.apply(state);

        assertTrue(state.isAdsRemoved());
    }
}
