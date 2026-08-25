package com.moneyfirst.wardrobesort;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The fixed, always-available "base" garment set that {@link RoundGenerator}
 * draws every timed round's target outfit and tray (targets + decoys) from.
 *
 * <p>Deliberately excludes every cosmetic-pack garment ({@link WardrobeIapCatalog}):
 * timed-round generation must never depend on what the player has purchased,
 * so it simply never looks at cosmetic garments in the first place. Cosmetic
 * garments are only ever surfaced in freeplay/custom-outfit mode once
 * unlocked (see {@link PlayerUnlocks#unlockedCosmeticGarmentIds()}).
 *
 * <p>Five garments per slot (25 total) gives {@link RoundGenerator} up to
 * four decoy alternatives per slot - twenty across all five slots - which
 * comfortably covers the generator's decoy-count ceiling.
 */
public final class GarmentCatalog {

    /** Garments per slot in the base catalog. Keep in sync with RoundGenerator.MAX_DECOYS headroom. */
    public static final int GARMENTS_PER_SLOT = 5;

    public static final List<Garment> BASE = Collections.unmodifiableList(buildBase());

    /** Cosmetic-only garments unlocked by {@code ws_pack_streetwear}. Never drawn into a timed round. */
    public static final List<Garment> STREETWEAR_PACK = Collections.unmodifiableList(buildStreetwearPack());
    /** Cosmetic-only garments unlocked by {@code ws_pack_formal}. Never drawn into a timed round. */
    public static final List<Garment> FORMAL_PACK = Collections.unmodifiableList(buildFormalPack());
    /** Cosmetic-only garments unlocked by {@code ws_pack_seasonal_bundle}. Never drawn into a timed round. */
    public static final List<Garment> SEASONAL_BUNDLE_PACK = Collections.unmodifiableList(buildSeasonalBundlePack());

    /** Every garment in the game, base + all cosmetic packs, for id lookups (avatar rendering, shop screen, etc). */
    public static final List<Garment> ALL;
    private static final Map<String, Garment> BY_ID;

    static {
        List<Garment> all = new ArrayList<>(BASE);
        all.addAll(STREETWEAR_PACK);
        all.addAll(FORMAL_PACK);
        all.addAll(SEASONAL_BUNDLE_PACK);
        ALL = Collections.unmodifiableList(all);

        Map<String, Garment> byId = new HashMap<>();
        for (Garment g : ALL) {
            byId.put(g.id(), g);
        }
        BY_ID = Collections.unmodifiableMap(byId);
    }

    private GarmentCatalog() {
    }

    /** Looks up any garment (base or cosmetic) by id, or returns {@code null} if unknown. */
    public static Garment findById(String garmentId) {
        return BY_ID.get(garmentId);
    }

    private static List<Garment> buildBase() {
        return List.of(
            // HEAD
            garment("head_cap", GarmentSlot.HEAD, 0.85f, 0.20f, 0.20f, 1f, 0),
            garment("head_beanie", GarmentSlot.HEAD, 0.20f, 0.45f, 0.85f, 1f, 1),
            garment("head_sunhat", GarmentSlot.HEAD, 0.95f, 0.80f, 0.30f, 1f, 2),
            garment("head_headband", GarmentSlot.HEAD, 0.60f, 0.20f, 0.75f, 1f, 3),
            garment("head_beret", GarmentSlot.HEAD, 0.25f, 0.25f, 0.25f, 1f, 4),

            // TOP
            garment("top_tshirt", GarmentSlot.TOP, 0.90f, 0.35f, 0.10f, 1f, 0),
            garment("top_hoodie", GarmentSlot.TOP, 0.30f, 0.70f, 0.40f, 1f, 1),
            garment("top_blouse", GarmentSlot.TOP, 0.95f, 0.60f, 0.75f, 1f, 2),
            garment("top_jacket", GarmentSlot.TOP, 0.15f, 0.15f, 0.55f, 1f, 3),
            garment("top_sweater", GarmentSlot.TOP, 0.70f, 0.50f, 0.20f, 1f, 4),

            // BOTTOM
            garment("bottom_jeans", GarmentSlot.BOTTOM, 0.20f, 0.30f, 0.65f, 1f, 0),
            garment("bottom_shorts", GarmentSlot.BOTTOM, 0.80f, 0.75f, 0.35f, 1f, 1),
            garment("bottom_skirt", GarmentSlot.BOTTOM, 0.85f, 0.25f, 0.45f, 1f, 2),
            garment("bottom_slacks", GarmentSlot.BOTTOM, 0.35f, 0.35f, 0.35f, 1f, 3),
            garment("bottom_joggers", GarmentSlot.BOTTOM, 0.40f, 0.55f, 0.30f, 1f, 4),

            // SHOES
            garment("shoes_sneakers", GarmentSlot.SHOES, 0.95f, 0.95f, 0.95f, 1f, 0),
            garment("shoes_boots", GarmentSlot.SHOES, 0.40f, 0.25f, 0.15f, 1f, 1),
            garment("shoes_sandals", GarmentSlot.SHOES, 0.85f, 0.65f, 0.45f, 1f, 2),
            garment("shoes_heels", GarmentSlot.SHOES, 0.75f, 0.10f, 0.15f, 1f, 3),
            garment("shoes_loafers", GarmentSlot.SHOES, 0.30f, 0.20f, 0.10f, 1f, 4),

            // ACCESSORY
            garment("acc_scarf", GarmentSlot.ACCESSORY, 0.90f, 0.45f, 0.10f, 1f, 0),
            garment("acc_glasses", GarmentSlot.ACCESSORY, 0.10f, 0.10f, 0.10f, 1f, 1),
            garment("acc_necklace", GarmentSlot.ACCESSORY, 0.85f, 0.75f, 0.20f, 1f, 2),
            garment("acc_watch", GarmentSlot.ACCESSORY, 0.55f, 0.55f, 0.60f, 1f, 3),
            garment("acc_bag", GarmentSlot.ACCESSORY, 0.65f, 0.30f, 0.55f, 1f, 4)
        );
    }

    private static Garment garment(String id, GarmentSlot slot, float r, float g, float b, float a, int shapeId) {
        return new Garment(id, slot, r, g, b, a, shapeId, "base");
    }

    private static Garment garment(String id, GarmentSlot slot, float r, float g, float b, float a, int shapeId, String pack) {
        return new Garment(id, slot, r, g, b, a, shapeId, pack);
    }

    private static List<Garment> buildStreetwearPack() {
        String pack = "ws_pack_streetwear";
        return List.of(
            garment("cos_streetwear_head", GarmentSlot.HEAD, 0.10f, 0.10f, 0.10f, 1f, 5, pack),
            garment("cos_streetwear_top", GarmentSlot.TOP, 0.95f, 0.55f, 0.05f, 1f, 5, pack),
            garment("cos_streetwear_bottom", GarmentSlot.BOTTOM, 0.05f, 0.05f, 0.05f, 1f, 5, pack),
            garment("cos_streetwear_shoes", GarmentSlot.SHOES, 0.90f, 0.90f, 0.10f, 1f, 5, pack),
            garment("cos_streetwear_acc", GarmentSlot.ACCESSORY, 0.80f, 0.10f, 0.10f, 1f, 5, pack)
        );
    }

    private static List<Garment> buildFormalPack() {
        String pack = "ws_pack_formal";
        return List.of(
            garment("cos_formal_head", GarmentSlot.HEAD, 0.05f, 0.05f, 0.15f, 1f, 6, pack),
            garment("cos_formal_top", GarmentSlot.TOP, 0.05f, 0.05f, 0.05f, 1f, 6, pack),
            garment("cos_formal_bottom", GarmentSlot.BOTTOM, 0.10f, 0.10f, 0.10f, 1f, 6, pack),
            garment("cos_formal_shoes", GarmentSlot.SHOES, 0.02f, 0.02f, 0.02f, 1f, 6, pack),
            garment("cos_formal_acc", GarmentSlot.ACCESSORY, 0.85f, 0.70f, 0.15f, 1f, 6, pack)
        );
    }

    private static List<Garment> buildSeasonalBundlePack() {
        // A larger "bundle" pack: two seasonal variants (winter + summer) per slot.
        String pack = "ws_pack_seasonal_bundle";
        return List.of(
            garment("cos_seasonal_winter_head", GarmentSlot.HEAD, 0.90f, 0.95f, 1.00f, 1f, 7, pack),
            garment("cos_seasonal_winter_top", GarmentSlot.TOP, 0.75f, 0.10f, 0.15f, 1f, 7, pack),
            garment("cos_seasonal_winter_bottom", GarmentSlot.BOTTOM, 0.20f, 0.20f, 0.30f, 1f, 7, pack),
            garment("cos_seasonal_winter_shoes", GarmentSlot.SHOES, 0.40f, 0.20f, 0.10f, 1f, 7, pack),
            garment("cos_seasonal_winter_acc", GarmentSlot.ACCESSORY, 0.95f, 0.95f, 0.95f, 1f, 7, pack),
            garment("cos_seasonal_summer_head", GarmentSlot.HEAD, 1.00f, 0.85f, 0.30f, 1f, 8, pack),
            garment("cos_seasonal_summer_top", GarmentSlot.TOP, 0.20f, 0.75f, 0.85f, 1f, 8, pack),
            garment("cos_seasonal_summer_bottom", GarmentSlot.BOTTOM, 0.95f, 0.85f, 0.60f, 1f, 8, pack),
            garment("cos_seasonal_summer_shoes", GarmentSlot.SHOES, 0.95f, 0.95f, 0.95f, 1f, 8, pack),
            garment("cos_seasonal_summer_acc", GarmentSlot.ACCESSORY, 0.20f, 0.60f, 0.30f, 1f, 8, pack)
        );
    }
}
