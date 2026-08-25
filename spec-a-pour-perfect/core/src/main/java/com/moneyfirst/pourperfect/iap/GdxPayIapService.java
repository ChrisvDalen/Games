package com.moneyfirst.pourperfect.iap;

import com.badlogic.gdx.pay.Offer;
import com.badlogic.gdx.pay.OfferType;
import com.badlogic.gdx.pay.PurchaseManager;
import com.badlogic.gdx.pay.PurchaseManagerConfig;
import com.badlogic.gdx.pay.PurchaseObserver;
import com.badlogic.gdx.pay.Transaction;
import com.moneyfirst.pourperfect.state.LevelProgressionState;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Real {@link IapService} implementation, wrapping gdx-pay's {@link PurchaseManager} - the same
 * abstraction gdx-pay uses to hide the Google Play Billing vs. Apple StoreKit split, per
 * {@code docs/MONEY_FIRST_ARCHITECTURE.md}. The platform launcher (Android/iOS) is responsible
 * for constructing the platform-specific {@code PurchaseManager} (e.g.
 * {@code PurchaseManagerGoogleBilling} or {@code PurchaseManageriOSApple}) and handing it to this
 * class; {@code core} never references a platform purchase-manager class directly.
 *
 * <p>All three {@link PourPerfectIapCatalog} products are registered as {@link OfferType#ENTITLEMENT}
 * offers (none of them are consumables or subscriptions) using the same identifier on every
 * store, since Pour Perfect doesn't need per-store product ID remapping.
 *
 * <p><strong>Why {@link #attachProgressionState} instead of a constructor parameter:</strong> both
 * platform launchers construct this service - and the platform {@code PurchaseManager} it wraps -
 * before {@code Gdx.app} exists (they build the full {@code AdsService}/{@code IapService} pair
 * first, then pass both into {@code PourPerfectGame}'s constructor, which is what actually starts
 * the libGDX application). {@link LevelProgressionState} can only be loaded once a real
 * {@code Preferences} instance is available, which needs {@code Gdx.app} - so instead of forcing
 * every launcher to hand-roll platform preferences access before that point, {@code
 * PourPerfectGame.create()} loads the one canonical {@link LevelProgressionState} and attaches it
 * here itself. Purchases are always user-initiated from a running UI, well after {@code create()}
 * has run, so this is never actually a race in practice.
 */
public final class GdxPayIapService implements IapService, PurchaseObserver {

    private static final Logger LOG = Logger.getLogger(GdxPayIapService.class.getName());

    private final PurchaseManager purchaseManager;
    private final Set<String> ownedProductIds = new HashSet<>();
    private volatile boolean installed;
    private LevelProgressionState progressionState;

    public GdxPayIapService(PurchaseManager purchaseManager) {
        this.purchaseManager = purchaseManager;
    }

    /** Called once by {@code PourPerfectGame.create()} with the app's single canonical progression state. */
    public void attachProgressionState(LevelProgressionState progressionState) {
        this.progressionState = progressionState;
    }

    /** Registers the catalog with gdx-pay and asks the platform store to install/connect. */
    public void install() {
        PurchaseManagerConfig config = new PurchaseManagerConfig();
        for (PourPerfectIapCatalog product : PourPerfectIapCatalog.values()) {
            config.addOffer(new Offer().setType(OfferType.ENTITLEMENT).setIdentifier(product.getProductId()));
        }
        purchaseManager.install(this, config, true);
    }

    @Override
    public void purchase(PourPerfectIapCatalog product) {
        if (!installed) {
            LOG.warning("purchase() called before the store finished installing; ignoring " + product);
            return;
        }
        purchaseManager.purchase(product.getProductId());
    }

    @Override
    public boolean isOwned(PourPerfectIapCatalog product) {
        return ownedProductIds.contains(product.getProductId());
    }

    @Override
    public void restore() {
        if (!installed) {
            LOG.warning("restore() called before the store finished installing; ignoring");
            return;
        }
        purchaseManager.purchaseRestore();
    }

    // -- PurchaseObserver --------------------------------------------------------------------

    @Override
    public void handleInstall() {
        installed = true;
    }

    @Override
    public void handleInstallError(Throwable e) {
        installed = false;
        LOG.log(Level.WARNING, "gdx-pay install failed", e);
    }

    @Override
    public void handlePurchase(Transaction transaction) {
        applyOwnership(transaction.getIdentifier());
    }

    @Override
    public void handleRestore(Transaction[] transactions) {
        for (Transaction transaction : transactions) {
            applyOwnership(transaction.getIdentifier());
        }
    }

    @Override
    public void handlePurchaseError(Throwable e) {
        LOG.log(Level.WARNING, "purchase failed", e);
    }

    @Override
    public void handleRestoreError(Throwable e) {
        LOG.log(Level.WARNING, "restore failed", e);
    }

    @Override
    public void handlePurchaseCanceled() {
        // No state change; the player simply backed out of the platform purchase sheet.
    }

    private void applyOwnership(String productId) {
        ownedProductIds.add(productId);
        PourPerfectIapCatalog.byProductId(productId).ifPresent(product -> {
            if (progressionState == null) {
                LOG.warning("owned product " + productId + " but no LevelProgressionState is attached yet; "
                        + "its purchase effect was NOT applied. See attachProgressionState() javadoc - this "
                        + "should never happen in practice since purchases only occur once the UI is live.");
                return;
            }
            product.apply(progressionState);
            progressionState.save();
        });
    }

    /** Read-only view of currently-known-owned product IDs, mostly useful for tests/diagnostics. */
    public Set<String> getOwnedProductIds() {
        return Collections.unmodifiableSet(ownedProductIds);
    }
}
