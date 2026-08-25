package com.moneyfirst.wardrobesort;

/**
 * Thin, platform-agnostic purchase facade gameplay/UI code talks to, so it
 * never touches gdx-pay's {@code PurchaseManager} directly. See
 * {@link GdxPayIapService} for the real implementation built on gdx-pay, and
 * docs/MONEY_FIRST_ARCHITECTURE.md for the shared IapService pattern.
 */
public interface IapService {

    /** True once the underlying PurchaseManager has finished installing and is ready to accept purchase()/purchaseRestore() calls. */
    boolean isReady();

    /** Starts a purchase flow for {@code offer}. Exactly one of the two callbacks fires, on the platform's own thread. */
    void purchase(WardrobeIapCatalog offer, Runnable onSuccess, Runnable onFailure);

    boolean isOwned(WardrobeIapCatalog offer);

    /** Re-queries the store for previously-purchased non-consumables (required by both Apple and Google review guidelines). */
    void restore(Runnable onComplete, Runnable onFailure);

    /** The aggregated view of everything the player owns, kept in sync as purchases/restores land. */
    PlayerUnlocks unlocks();
}
