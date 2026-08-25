package com.moneyfirst.pourperfect.iap;

import com.moneyfirst.pourperfect.state.LevelProgressionState;

import java.util.Optional;

/**
 * The three in-app products Pour Perfect sells. Product IDs are placeholders matching the naming
 * convention we'll register in Play Console / App Store Connect - see README.md for the
 * before-submission checklist. Price hints are display-only fallbacks; the real, localized price
 * always comes from the store via {@code IapService}/gdx-pay's {@code Information} lookup.
 */
public enum PourPerfectIapCatalog {

    /** Removes all interstitial/banner ads permanently. Does not affect rewarded-video opt-ins. */
    REMOVE_ADS("pp_remove_ads", "€2.99") {
        @Override
        public void apply(LevelProgressionState state) {
            state.setAdsRemoved(true);
        }
    },

    /** One-time bundle of extra hint and undo credits. */
    STARTER_PACK("pp_starter_pack", "€1.99") {
        private static final int HINTS_GRANTED = 50;
        private static final int UNDOS_GRANTED = 10;

        @Override
        public void apply(LevelProgressionState state) {
            state.grantHints(HINTS_GRANTED);
            state.grantUndos(UNDOS_GRANTED);
        }
    },

    /** Unlocks the cosmetic cafe meta-progression layer (also unlocks free at level 20+). */
    CAFE_EXPANSION("pp_cafe_expansion", "€4.99") {
        @Override
        public void apply(LevelProgressionState state) {
            state.unlockCafe();
        }
    };

    private final String productId;
    private final String priceHint;

    PourPerfectIapCatalog(String productId, String priceHint) {
        this.productId = productId;
        this.priceHint = priceHint;
    }

    public String getProductId() {
        return productId;
    }

    /** Display-only fallback price, used before the store's real localized price has loaded. */
    public String getPriceHint() {
        return priceHint;
    }

    /** Applies this product's effect to persistent progression once a purchase is confirmed. */
    public abstract void apply(LevelProgressionState state);

    public static Optional<PourPerfectIapCatalog> byProductId(String productId) {
        for (PourPerfectIapCatalog catalog : values()) {
            if (catalog.productId.equals(productId)) {
                return Optional.of(catalog);
            }
        }
        return Optional.empty();
    }
}
