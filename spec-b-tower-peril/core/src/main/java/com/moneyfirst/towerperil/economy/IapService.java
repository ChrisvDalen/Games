package com.moneyfirst.towerperil.economy;

import com.badlogic.gdx.pay.Offer;
import com.badlogic.gdx.pay.OfferType;
import com.badlogic.gdx.pay.PurchaseManager;
import com.badlogic.gdx.pay.PurchaseManagerConfig;
import com.badlogic.gdx.pay.PurchaseObserver;
import com.badlogic.gdx.pay.Transaction;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Thin, platform-agnostic wrapper around gdx-pay's {@link PurchaseManager}.
 * Gameplay/UI code talks only to this class - never to {@code PurchaseManager}
 * or {@link PurchaseObserver} directly - which is what makes the same
 * gameplay code work unmodified against Android's Google Play Billing
 * backend and iOS's Apple StoreKit backend (both gdx-pay implementations of
 * {@code PurchaseManager}); the platform launcher just injects a different
 * concrete manager. Also exposes subscription-aware
 * {@link #isSeasonPassActive}, since the season pass is a recurring product.
 */
public final class IapService implements PurchaseObserver {

    /** Listener surface for gameplay/UI code, so nothing outside this class implements PurchaseObserver. */
    public interface Listener {
        default void onStoreReady() {
        }

        default void onStoreUnavailable(Throwable error) {
        }

        default void onPurchaseSucceeded(String productId, Transaction transaction) {
        }

        default void onPurchaseFailed(String productId, Throwable error) {
        }

        default void onPurchaseCanceled() {
        }

        default void onRestoreCompleted(Transaction[] transactions) {
        }

        default void onRestoreFailed(Throwable error) {
        }
    }

    private final PurchaseManager purchaseManager;
    private final Set<String> ownedProductIds = new HashSet<>();
    private final Set<Listener> listeners = new CopyOnWriteArraySet<>();
    private String pendingPurchaseProductId;
    private boolean installed;

    public IapService(PurchaseManager purchaseManager, PurchaseManagerConfig config) {
        this.purchaseManager = purchaseManager;
        purchaseManager.install(this, config, true);
    }

    public void addListener(Listener listener) {
        listeners.add(listener);
    }

    public void removeListener(Listener listener) {
        listeners.remove(listener);
    }

    public boolean isReady() {
        return installed;
    }

    public void purchase(String productId) {
        if (!installed) {
            throw new IllegalStateException("Store is not installed yet");
        }
        pendingPurchaseProductId = productId;
        purchaseManager.purchase(productId);
    }

    public boolean isOwned(String productId) {
        return ownedProductIds.contains(productId);
    }

    /** Subscription-aware convenience for the one subscription product Tower Peril sells. */
    public boolean isSeasonPassActive() {
        return isOwned(TowerPerilIapCatalog.SEASON_PASS.getProductId());
    }

    public void restore() {
        if (!installed) {
            throw new IllegalStateException("Store is not installed yet");
        }
        purchaseManager.purchaseRestore();
    }

    public void dispose() {
        purchaseManager.dispose();
    }

    /** Builds the gdx-pay config listing every Tower Peril product, including the season-pass subscription. */
    public static PurchaseManagerConfig buildConfig() {
        PurchaseManagerConfig config = new PurchaseManagerConfig();
        for (TowerPerilIapCatalog product : TowerPerilIapCatalog.values()) {
            Offer offer = new Offer()
                    .setType(product.isSubscription() ? OfferType.SUBSCRIPTION : OfferType.CONSUMABLE)
                    .setIdentifier(product.getProductId());
            config.addOffer(offer);
        }
        return config;
    }

    // ---- PurchaseObserver ----

    @Override
    public void handleInstall() {
        installed = true;
        for (Listener listener : listeners) {
            listener.onStoreReady();
        }
    }

    @Override
    public void handleInstallError(Throwable e) {
        installed = false;
        for (Listener listener : listeners) {
            listener.onStoreUnavailable(e);
        }
    }

    @Override
    public void handleRestore(Transaction[] transactions) {
        for (Transaction transaction : transactions) {
            if (transaction.isPurchased()) {
                ownedProductIds.add(transaction.getIdentifier());
            }
        }
        for (Listener listener : listeners) {
            listener.onRestoreCompleted(transactions);
        }
    }

    @Override
    public void handleRestoreError(Throwable e) {
        for (Listener listener : listeners) {
            listener.onRestoreFailed(e);
        }
    }

    @Override
    public void handlePurchase(Transaction transaction) {
        if (transaction.isPurchased()) {
            ownedProductIds.add(transaction.getIdentifier());
        }
        String productId = pendingPurchaseProductId != null ? pendingPurchaseProductId : transaction.getIdentifier();
        pendingPurchaseProductId = null;
        for (Listener listener : listeners) {
            listener.onPurchaseSucceeded(productId, transaction);
        }
    }

    @Override
    public void handlePurchaseError(Throwable e) {
        String productId = pendingPurchaseProductId;
        pendingPurchaseProductId = null;
        for (Listener listener : listeners) {
            listener.onPurchaseFailed(productId, e);
        }
    }

    @Override
    public void handlePurchaseCanceled() {
        pendingPurchaseProductId = null;
        for (Listener listener : listeners) {
            listener.onPurchaseCanceled();
        }
    }
}
