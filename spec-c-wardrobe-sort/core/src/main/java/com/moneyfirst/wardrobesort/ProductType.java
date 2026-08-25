package com.moneyfirst.wardrobesort;

/** Broad shape of a {@link WardrobeIapCatalog} entry, for shop-screen grouping and unlock logic. */
public enum ProductType {
    /** Unlocks a list of cosmetic garment ids for freeplay/custom-outfit mode. Never affects timed-round difficulty/timer/scoring. */
    COSMETIC_PACK,
    /** A boolean feature flag on {@link PlayerUnlocks} (remove-ads, avatar-personalization). */
    FEATURE_UNLOCK
}
