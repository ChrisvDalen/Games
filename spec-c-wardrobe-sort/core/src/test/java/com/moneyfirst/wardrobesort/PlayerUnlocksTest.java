package com.moneyfirst.wardrobesort;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerUnlocksTest {

    @Test
    void ownsNothingByDefault() {
        PlayerUnlocks unlocks = new PlayerUnlocks();
        for (WardrobeIapCatalog offer : WardrobeIapCatalog.values()) {
            assertFalse(unlocks.owns(offer));
        }
        assertFalse(unlocks.isRemoveAdsActive());
        assertFalse(unlocks.isAvatarPersonalizationActive());
        assertTrue(unlocks.unlockedCosmeticGarmentIds().isEmpty());
    }

    @Test
    void grantingRemoveAdsOnlyFlipsThatFlag() {
        PlayerUnlocks unlocks = new PlayerUnlocks();
        unlocks.grant(WardrobeIapCatalog.WS_REMOVE_ADS);

        assertTrue(unlocks.isRemoveAdsActive());
        assertFalse(unlocks.isAvatarPersonalizationActive());
        assertTrue(unlocks.unlockedCosmeticGarmentIds().isEmpty());
    }

    @Test
    void grantingAvatarPersonalizationOnlyFlipsThatFlag() {
        PlayerUnlocks unlocks = new PlayerUnlocks();
        unlocks.grant(WardrobeIapCatalog.WS_AVATAR_PERSONALIZATION);

        assertTrue(unlocks.isAvatarPersonalizationActive());
        assertFalse(unlocks.isRemoveAdsActive());
    }

    @Test
    void unlockedCosmeticGarmentIdsAggregatesAcrossOwnedPacksOnly() {
        PlayerUnlocks unlocks = new PlayerUnlocks();
        unlocks.grant(WardrobeIapCatalog.WS_PACK_STREETWEAR);

        Set<String> unlocked = unlocks.unlockedCosmeticGarmentIds();
        assertEquals(Set.copyOf(WardrobeIapCatalog.WS_PACK_STREETWEAR.unlockedGarmentIds()), unlocked);

        unlocks.grant(WardrobeIapCatalog.WS_PACK_FORMAL);
        Set<String> expectedAfterTwo = new java.util.HashSet<>(WardrobeIapCatalog.WS_PACK_STREETWEAR.unlockedGarmentIds());
        expectedAfterTwo.addAll(WardrobeIapCatalog.WS_PACK_FORMAL.unlockedGarmentIds());
        assertEquals(expectedAfterTwo, unlocks.unlockedCosmeticGarmentIds());
    }

    @Test
    void revokeRemovesOwnership() {
        PlayerUnlocks unlocks = new PlayerUnlocks();
        unlocks.grant(WardrobeIapCatalog.WS_REMOVE_ADS);
        assertTrue(unlocks.isRemoveAdsActive());

        unlocks.revoke(WardrobeIapCatalog.WS_REMOVE_ADS);
        assertFalse(unlocks.isRemoveAdsActive());
    }
}
