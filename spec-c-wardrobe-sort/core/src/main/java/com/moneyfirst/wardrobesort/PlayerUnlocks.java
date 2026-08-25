package com.moneyfirst.wardrobesort;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * What the player currently owns: which {@link WardrobeIapCatalog} products
 * they've purchased, aggregated into the cosmetic garment ids and feature
 * flags gameplay code actually cares about.
 *
 * <p>This is a plain in-memory model; {@link IapService} implementations are
 * responsible for populating it from purchase/restore results and for
 * persisting it (e.g. to {@code Gdx.app.getPreferences}) - out of scope for
 * this pure-logic class.
 */
public final class PlayerUnlocks {

    private final Set<String> ownedProductIds = new HashSet<>();

    /** Marks a product as owned. Idempotent. */
    public void grant(WardrobeIapCatalog offer) {
        ownedProductIds.add(offer.productId());
    }

    public void revoke(WardrobeIapCatalog offer) {
        ownedProductIds.remove(offer.productId());
    }

    public boolean owns(WardrobeIapCatalog offer) {
        return ownedProductIds.contains(offer.productId());
    }

    public boolean isRemoveAdsActive() {
        return owns(WardrobeIapCatalog.WS_REMOVE_ADS);
    }

    /**
     * Whether the "photo avatar" personalization mode is unlocked. This flag
     * is real and fully wired here; the photo-upload/background-removal
     * pipeline it would gate is a separate backend service, out of scope for
     * this engine module.
     */
    public boolean isAvatarPersonalizationActive() {
        return owns(WardrobeIapCatalog.WS_AVATAR_PERSONALIZATION);
    }

    /** All cosmetic garment ids unlocked by every owned cosmetic pack, for freeplay/custom-outfit mode. */
    public Set<String> unlockedCosmeticGarmentIds() {
        Set<String> ids = new LinkedHashSet<>();
        for (WardrobeIapCatalog offer : WardrobeIapCatalog.values()) {
            if (offer.isCosmeticPack() && owns(offer)) {
                ids.addAll(offer.unlockedGarmentIds());
            }
        }
        return Collections.unmodifiableSet(ids);
    }

    public Set<String> ownedProductIds() {
        return Collections.unmodifiableSet(new HashSet<>(ownedProductIds));
    }
}
