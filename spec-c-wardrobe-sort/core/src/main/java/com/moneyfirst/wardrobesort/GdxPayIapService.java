package com.moneyfirst.wardrobesort;

import com.badlogic.gdx.pay.Offer;
import com.badlogic.gdx.pay.OfferType;
import com.badlogic.gdx.pay.PurchaseManager;
import com.badlogic.gdx.pay.PurchaseManagerConfig;
import com.badlogic.gdx.pay.PurchaseObserver;
import com.badlogic.gdx.pay.Transaction;

import java.util.Objects;

/**
 * Real {@link IapService} implementation built directly on gdx-pay's
 * {@link PurchaseManager} (the same class abstracts Google Play Billing and
 * Apple StoreKit - see docs/MONEY_FIRST_ARCHITECTURE.md). The concrete
 * {@code PurchaseManager} instance is injected by platform launcher code:
 * Android wires {@code com.badlogicgames.gdxpay:gdx-pay-android-googlebilling}'s
 * manager, iOS wires {@code com.badlogicgames.gdxpay:gdx-pay-iosrobovm-apple}'s.
 * Gameplay/UI code never sees {@code PurchaseManager} directly - only this
 * class's {@link IapService} surface.
 *
 * <p>Every {@link WardrobeIapCatalog} product is registered as an
 * {@link OfferType#ENTITLEMENT} (a permanent, non-consumable unlock) since
 * cosmetic packs, remove-ads, and avatar-personalization are all one-time
 * purchases that stay owned forever - never repurchased/consumed.
 */
public final class GdxPayIapService implements IapService, PurchaseObserver {

    private final PurchaseManager purchaseManager;
    private final PlayerUnlocks unlocks = new PlayerUnlocks();

    private volatile boolean ready = false;

    private Runnable pendingPurchaseSuccess;
    private Runnable pendingPurchaseFailure;
    private Runnable pendingRestoreComplete;
    private Runnable pendingRestoreFailure;

    public GdxPayIapService(PurchaseManager purchaseManager) {
        this.purchaseManager = Objects.requireNonNull(purchaseManager, "purchaseManager");
        // Third argument is gdx-pay's "autoFetchInformation": true fetches store
        // price/title/description metadata for every offer right after install,
        // which the shop screen uses to show the store's real localized price
        // rather than the referencePriceEur() display placeholder.
        this.purchaseManager.install(this, buildConfig(), true);
    }

    private static PurchaseManagerConfig buildConfig() {
        PurchaseManagerConfig config = new PurchaseManagerConfig();
        for (WardrobeIapCatalog offer : WardrobeIapCatalog.values()) {
            config.addOffer(new Offer().setType(OfferType.ENTITLEMENT).setIdentifier(offer.productId()));
        }
        return config;
    }

    @Override
    public boolean isReady() {
        return ready;
    }

    @Override
    public void purchase(WardrobeIapCatalog offer, Runnable onSuccess, Runnable onFailure) {
        Objects.requireNonNull(offer, "offer");
        this.pendingPurchaseSuccess = onSuccess;
        this.pendingPurchaseFailure = onFailure;
        purchaseManager.purchase(offer.productId());
    }

    @Override
    public boolean isOwned(WardrobeIapCatalog offer) {
        return unlocks.owns(offer);
    }

    @Override
    public void restore(Runnable onComplete, Runnable onFailure) {
        this.pendingRestoreComplete = onComplete;
        this.pendingRestoreFailure = onFailure;
        purchaseManager.purchaseRestore();
    }

    @Override
    public PlayerUnlocks unlocks() {
        return unlocks;
    }

    // -- PurchaseObserver --

    @Override
    public void handleInstall() {
        ready = true;
    }

    @Override
    public void handleInstallError(Throwable t) {
        ready = false;
    }

    @Override
    public void handleRestore(Transaction[] transactions) {
        if (transactions != null) {
            for (Transaction t : transactions) {
                applyTransaction(t);
            }
        }
        Runnable callback = pendingRestoreComplete;
        clearRestoreCallbacks();
        if (callback != null) {
            callback.run();
        }
    }

    @Override
    public void handleRestoreError(Throwable t) {
        Runnable callback = pendingRestoreFailure;
        clearRestoreCallbacks();
        if (callback != null) {
            callback.run();
        }
    }

    @Override
    public void handlePurchase(Transaction transaction) {
        applyTransaction(transaction);
        Runnable callback = pendingPurchaseSuccess;
        clearPurchaseCallbacks();
        if (callback != null) {
            callback.run();
        }
    }

    @Override
    public void handlePurchaseError(Throwable t) {
        Runnable callback = pendingPurchaseFailure;
        clearPurchaseCallbacks();
        if (callback != null) {
            callback.run();
        }
    }

    @Override
    public void handlePurchaseCanceled() {
        Runnable callback = pendingPurchaseFailure;
        clearPurchaseCallbacks();
        if (callback != null) {
            callback.run();
        }
    }

    private void applyTransaction(Transaction transaction) {
        if (transaction == null || transaction.getIdentifier() == null || !transaction.isPurchased()) {
            return;
        }
        try {
            unlocks.grant(WardrobeIapCatalog.fromProductId(transaction.getIdentifier()));
        } catch (IllegalArgumentException unknownProduct) {
            // A transaction for a product id outside this game's catalog (shouldn't
            // happen in practice) - ignore rather than crash the purchase flow.
        }
    }

    private void clearPurchaseCallbacks() {
        pendingPurchaseSuccess = null;
        pendingPurchaseFailure = null;
    }

    private void clearRestoreCallbacks() {
        pendingRestoreComplete = null;
        pendingRestoreFailure = null;
    }
}
