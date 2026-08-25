package com.moneyfirst.wardrobesort;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GarmentCatalogTest {

    @Test
    void baseCatalogHasTheSameGarmentCountForEverySlot() {
        for (GarmentSlot slot : GarmentSlot.values()) {
            long count = GarmentCatalog.BASE.stream().filter(g -> g.slot() == slot).count();
            assertEquals(GarmentCatalog.GARMENTS_PER_SLOT, count, "slot " + slot + " should have exactly GARMENTS_PER_SLOT base garments");
        }
    }

    @Test
    void everyBaseGarmentHasAUniqueId() {
        List<String> ids = GarmentCatalog.BASE.stream().map(Garment::id).collect(Collectors.toList());
        assertEquals(ids.size(), ids.stream().distinct().count());
    }

    @Test
    void findByIdResolvesBothBaseAndCosmeticGarments() {
        assertNotNull(GarmentCatalog.findById("head_cap"));
        assertNotNull(GarmentCatalog.findById("cos_streetwear_head"));
        assertNull(GarmentCatalog.findById("does_not_exist"));
    }

    @Test
    void allIncludesBaseAndAllThreeCosmeticPacksWithNoOverlap() {
        int expected = GarmentCatalog.BASE.size()
            + GarmentCatalog.STREETWEAR_PACK.size()
            + GarmentCatalog.FORMAL_PACK.size()
            + GarmentCatalog.SEASONAL_BUNDLE_PACK.size();
        assertEquals(expected, GarmentCatalog.ALL.size());

        long distinctIds = GarmentCatalog.ALL.stream().map(Garment::id).distinct().count();
        assertEquals(GarmentCatalog.ALL.size(), distinctIds, "no garment id should be reused across base/cosmetic packs");
    }

    @Test
    void everyCosmeticGarmentIsFlaggedAsCosmeticAndEveryBaseGarmentIsNot() {
        for (Garment g : GarmentCatalog.BASE) {
            assertTrue(!g.isCosmeticPackGarment(), g.id() + " is in BASE and should not be flagged cosmetic");
        }
        for (Garment g : GarmentCatalog.STREETWEAR_PACK) {
            assertTrue(g.isCosmeticPackGarment(), g.id() + " is a cosmetic-pack garment");
        }
    }
}
