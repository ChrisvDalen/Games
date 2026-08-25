package com.moneyfirst.wardrobesort;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WardrobeIapCatalogTest {

    @Test
    void catalogHasThreeCosmeticPacksAndTwoFeatureUnlocksWithSpecPrices() {
        assertEquals(new BigDecimal("0.99"), WardrobeIapCatalog.WS_PACK_STREETWEAR.referencePriceEur());
        assertEquals(new BigDecimal("2.99"), WardrobeIapCatalog.WS_PACK_FORMAL.referencePriceEur());
        assertEquals(new BigDecimal("4.99"), WardrobeIapCatalog.WS_PACK_SEASONAL_BUNDLE.referencePriceEur());
        assertEquals(new BigDecimal("2.99"), WardrobeIapCatalog.WS_REMOVE_ADS.referencePriceEur());
        assertEquals(new BigDecimal("4.99"), WardrobeIapCatalog.WS_AVATAR_PERSONALIZATION.referencePriceEur());

        assertTrue(WardrobeIapCatalog.WS_PACK_STREETWEAR.isCosmeticPack());
        assertTrue(WardrobeIapCatalog.WS_PACK_FORMAL.isCosmeticPack());
        assertTrue(WardrobeIapCatalog.WS_PACK_SEASONAL_BUNDLE.isCosmeticPack());
        assertFalse(WardrobeIapCatalog.WS_REMOVE_ADS.isCosmeticPack());
        assertFalse(WardrobeIapCatalog.WS_AVATAR_PERSONALIZATION.isCosmeticPack());
    }

    @Test
    void everyCosmeticPackUnlocksANonEmptyGarmentList() {
        for (WardrobeIapCatalog offer : WardrobeIapCatalog.values()) {
            if (offer.isCosmeticPack()) {
                assertFalse(offer.unlockedGarmentIds().isEmpty(), offer + " should unlock at least one garment");
                for (String garmentId : offer.unlockedGarmentIds()) {
                    assertTrue(GarmentCatalog.findById(garmentId) != null, "unlocked garment id " + garmentId + " must exist in the catalog");
                }
            } else {
                assertTrue(offer.unlockedGarmentIds().isEmpty(), offer + " is a feature unlock, not a cosmetic pack, so it should not list garments");
            }
        }
    }

    @Test
    void cosmeticGarmentsAreNeverPartOfTheBaseCatalogRoundGeneratorDrawsFrom() {
        Set<String> baseIds = GarmentCatalog.BASE.stream().map(Garment::id).collect(java.util.stream.Collectors.toSet());
        for (WardrobeIapCatalog offer : WardrobeIapCatalog.values()) {
            for (String cosmeticId : offer.unlockedGarmentIds()) {
                assertFalse(baseIds.contains(cosmeticId),
                    "cosmetic garment " + cosmeticId + " from " + offer + " must not appear in the base catalog RoundGenerator draws from");
            }
        }
    }

    @Test
    void purchasingCosmeticPackNeverChangesRoundGeneratorOutputForAFixedSeed() {
        RoundGenerator generator = RoundGenerator.withBaseCatalog();
        long seed = 2024L;

        // Snapshot every round's target + tray + timer BEFORE any purchase.
        List<Round> before = snapshotRounds(generator, seed);

        // Simulate purchasing every cosmetic pack.
        PlayerUnlocks unlocks = new PlayerUnlocks();
        unlocks.grant(WardrobeIapCatalog.WS_PACK_STREETWEAR);
        unlocks.grant(WardrobeIapCatalog.WS_PACK_FORMAL);
        unlocks.grant(WardrobeIapCatalog.WS_PACK_SEASONAL_BUNDLE);
        assertTrue(unlocks.owns(WardrobeIapCatalog.WS_PACK_STREETWEAR));
        assertTrue(unlocks.owns(WardrobeIapCatalog.WS_PACK_FORMAL));
        assertTrue(unlocks.owns(WardrobeIapCatalog.WS_PACK_SEASONAL_BUNDLE));
        assertFalse(unlocks.unlockedCosmeticGarmentIds().isEmpty(), "sanity: purchasing packs should unlock cosmetic garments");

        // RoundGenerator never even sees PlayerUnlocks/IapService, but re-run it with a
        // brand new instance to make the "no hidden shared mutable state" guarantee explicit.
        RoundGenerator generatorAfterPurchase = RoundGenerator.withBaseCatalog();
        List<Round> after = snapshotRounds(generatorAfterPurchase, seed);

        assertEquals(before.size(), after.size());
        for (int i = 0; i < before.size(); i++) {
            Round b = before.get(i);
            Round a = after.get(i);
            assertEquals(b.timerSeconds(), a.timerSeconds(), 0.0001f,
                "owning cosmetic packs must not change round " + (i + 1) + "'s timer");
            assertEquals(
                b.tray().stream().map(Garment::id).toList(),
                a.tray().stream().map(Garment::id).toList(),
                "owning cosmetic packs must not change round " + (i + 1) + "'s tray (contents, order, or decoy count)");
            for (GarmentSlot slot : GarmentSlot.values()) {
                assertEquals(b.target().required(slot).id(), a.target().required(slot).id(),
                    "owning cosmetic packs must not change round " + (i + 1) + "'s target outfit");
            }
        }
    }

    private static List<Round> snapshotRounds(RoundGenerator generator, long seed) {
        return java.util.stream.IntStream.rangeClosed(1, 20)
            .mapToObj(round -> generator.generateRound(round, seed))
            .toList();
    }
}
