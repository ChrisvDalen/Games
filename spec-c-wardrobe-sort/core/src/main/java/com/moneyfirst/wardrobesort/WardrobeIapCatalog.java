package com.moneyfirst.wardrobesort;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Every purchasable product in Wardrobe Sort. Prices are the reference EUR
 * price shown in the shop UI copy; the actual charged amount always comes
 * from the store (Google Play / App Store) at purchase time via gdx-pay -
 * these are display-only, never used for billing math.
 *
 * <p>The three {@code COSMETIC_PACK} entries are purely cosmetic: owning one
 * only ever unlocks {@link #unlockedGarmentIds()} for use in freeplay/custom
 * outfits (see {@link PlayerUnlocks#unlockedCosmeticGarmentIds()}). They are
 * never consulted by {@link RoundGenerator}, so they can never change a timed
 * round's difficulty, timer, or scoring - see
 * {@code WardrobeIapCatalogTest#purchasingCosmeticPackNeverChangesRoundGeneratorOutput}.
 *
 * <p>{@code WS_AVATAR_PERSONALIZATION} models only the unlock/purchase/
 * feature-gating plumbing for a "photo avatar" mode (see
 * {@link PlayerUnlocks#isAvatarPersonalizationActive()}). The actual
 * photo-upload/background-removal pipeline needed to render a player's own
 * photo as an avatar is intentionally out of scope for this engine module -
 * it would be a separate backend service.
 */
public enum WardrobeIapCatalog {

    WS_PACK_STREETWEAR(
        "ws_pack_streetwear", "Streetwear Pack", ProductType.COSMETIC_PACK, price("0.99"),
        garmentIds(GarmentCatalog.STREETWEAR_PACK)),

    WS_PACK_FORMAL(
        "ws_pack_formal", "Formal Wear Pack", ProductType.COSMETIC_PACK, price("2.99"),
        garmentIds(GarmentCatalog.FORMAL_PACK)),

    WS_PACK_SEASONAL_BUNDLE(
        "ws_pack_seasonal_bundle", "Seasonal Bundle", ProductType.COSMETIC_PACK, price("4.99"),
        garmentIds(GarmentCatalog.SEASONAL_BUNDLE_PACK)),

    WS_REMOVE_ADS(
        "ws_remove_ads", "Remove Ads", ProductType.FEATURE_UNLOCK, price("2.99"),
        List.of()),

    WS_AVATAR_PERSONALIZATION(
        "ws_avatar_personalization", "Photo Avatar Personalization", ProductType.FEATURE_UNLOCK, price("4.99"),
        List.of());

    private final String productId;
    private final String displayName;
    private final ProductType type;
    private final BigDecimal referencePriceEur;
    private final List<String> unlockedGarmentIds;

    WardrobeIapCatalog(String productId, String displayName, ProductType type, BigDecimal referencePriceEur,
                        List<String> unlockedGarmentIds) {
        this.productId = productId;
        this.displayName = displayName;
        this.type = type;
        this.referencePriceEur = referencePriceEur;
        this.unlockedGarmentIds = List.copyOf(unlockedGarmentIds);
    }

    public String productId() {
        return productId;
    }

    public String displayName() {
        return displayName;
    }

    public ProductType type() {
        return type;
    }

    public BigDecimal referencePriceEur() {
        return referencePriceEur;
    }

    /** Cosmetic garment ids this product unlocks for freeplay/custom-outfit mode. Empty for FEATURE_UNLOCK products. */
    public List<String> unlockedGarmentIds() {
        return unlockedGarmentIds;
    }

    public boolean isCosmeticPack() {
        return type == ProductType.COSMETIC_PACK;
    }

    public static WardrobeIapCatalog fromProductId(String productId) {
        for (WardrobeIapCatalog offer : values()) {
            if (offer.productId.equals(productId)) {
                return offer;
            }
        }
        throw new IllegalArgumentException("Unknown Wardrobe Sort product id: " + productId);
    }

    private static BigDecimal price(String amount) {
        return new BigDecimal(amount);
    }

    private static List<String> garmentIds(List<Garment> garments) {
        return garments.stream().map(Garment::id).collect(Collectors.toUnmodifiableList());
    }
}
