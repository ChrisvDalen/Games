package com.moneyfirst.pourperfect.iap;

import com.badlogic.gdx.pay.Information;
import com.badlogic.gdx.pay.PurchaseManager;
import com.badlogic.gdx.pay.PurchaseManagerConfig;
import com.badlogic.gdx.pay.PurchaseObserver;
import com.badlogic.gdx.pay.Transaction;
import com.moneyfirst.pourperfect.fakes.InMemoryPreferences;
import com.moneyfirst.pourperfect.state.LevelProgressionState;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Exercises {@link GdxPayIapService} against a fake {@link PurchaseManager} that implements
 * gdx-pay's real interfaces (not core's own {@code IapService}), so this test also doubles as
 * verification that our usage of the actual gdx-pay-client API (install/purchase/purchaseRestore
 * and the {@link PurchaseObserver} callbacks) is wired correctly.
 */
class GdxPayIapServiceTest {

    @Test
    void installRegistersAllThreeCatalogProductsAsOffers() {
        FakePurchaseManager manager = new FakePurchaseManager();
        GdxPayIapService service = new GdxPayIapService(manager);
        service.attachProgressionState(LevelProgressionState.load(new InMemoryPreferences()));

        service.install();

        assertEquals(PourPerfectIapCatalog.values().length, manager.lastConfig.getOfferCount());
        for (PourPerfectIapCatalog product : PourPerfectIapCatalog.values()) {
            assertEquals(product.getProductId(), manager.lastConfig.getOffer(product.getProductId()).getIdentifier());
        }
    }

    @Test
    void successfulPurchaseMarksProductOwnedAndAppliesItsEffect() {
        FakePurchaseManager manager = new FakePurchaseManager();
        LevelProgressionState state = LevelProgressionState.load(new InMemoryPreferences());
        GdxPayIapService service = new GdxPayIapService(manager);
        service.attachProgressionState(state);
        service.install();

        service.purchase(PourPerfectIapCatalog.REMOVE_ADS);
        manager.completePurchase(PourPerfectIapCatalog.REMOVE_ADS.getProductId());

        assertTrue(service.isOwned(PourPerfectIapCatalog.REMOVE_ADS));
        assertTrue(state.isAdsRemoved());
        assertEquals(List.of(PourPerfectIapCatalog.REMOVE_ADS.getProductId()), manager.purchaseCalls);
    }

    @Test
    void restoreAppliesEffectsForEveryRestoredTransaction() {
        FakePurchaseManager manager = new FakePurchaseManager();
        LevelProgressionState state = LevelProgressionState.load(new InMemoryPreferences());
        GdxPayIapService service = new GdxPayIapService(manager);
        service.attachProgressionState(state);
        service.install();

        service.restore();
        manager.completeRestore(
                PourPerfectIapCatalog.REMOVE_ADS.getProductId(),
                PourPerfectIapCatalog.CAFE_EXPANSION.getProductId());

        assertTrue(service.isOwned(PourPerfectIapCatalog.REMOVE_ADS));
        assertTrue(service.isOwned(PourPerfectIapCatalog.CAFE_EXPANSION));
        assertFalse(service.isOwned(PourPerfectIapCatalog.STARTER_PACK));
        assertTrue(state.isAdsRemoved());
        assertTrue(state.isCafeUnlocked());
    }

    @Test
    void purchaseBeforeInstallCompletesIsIgnoredRatherThanCrashing() {
        FakePurchaseManager manager = new FakePurchaseManager();
        GdxPayIapService service = new GdxPayIapService(manager);
        service.attachProgressionState(LevelProgressionState.load(new InMemoryPreferences()));

        service.purchase(PourPerfectIapCatalog.REMOVE_ADS); // install() was never called

        assertTrue(manager.purchaseCalls.isEmpty());
    }

    @Test
    void unownedProductReportsFalse() {
        FakePurchaseManager manager = new FakePurchaseManager();
        GdxPayIapService service = new GdxPayIapService(manager);
        service.attachProgressionState(LevelProgressionState.load(new InMemoryPreferences()));
        service.install();

        assertFalse(service.isOwned(PourPerfectIapCatalog.STARTER_PACK));
    }

    @Test
    void purchaseCompletingBeforeProgressionStateIsAttachedStillRecordsOwnershipButLogsInsteadOfCrashing() {
        FakePurchaseManager manager = new FakePurchaseManager();
        GdxPayIapService service = new GdxPayIapService(manager);
        // Deliberately no attachProgressionState() call here.
        service.install();

        service.purchase(PourPerfectIapCatalog.REMOVE_ADS);
        manager.completePurchase(PourPerfectIapCatalog.REMOVE_ADS.getProductId());

        assertTrue(service.isOwned(PourPerfectIapCatalog.REMOVE_ADS));
    }

    /** Minimal fake of gdx-pay's real {@link PurchaseManager}/{@link PurchaseObserver} contract. */
    private static final class FakePurchaseManager implements PurchaseManager {
        PurchaseObserver observer;
        PurchaseManagerConfig lastConfig;
        final List<String> purchaseCalls = new ArrayList<>();

        @Override
        public String storeName() {
            return PurchaseManagerConfig.STORE_NAME_ANDROID_GOOGLE;
        }

        @Override
        public void install(PurchaseObserver observer, PurchaseManagerConfig config, boolean autoFetchInformation) {
            this.observer = observer;
            this.lastConfig = config;
            observer.handleInstall();
        }

        @Override
        public boolean installed() {
            return observer != null;
        }

        @Override
        public void dispose() {
        }

        @Override
        public void purchase(String identifier) {
            purchaseCalls.add(identifier);
        }

        @Override
        public void purchaseRestore() {
        }

        @Override
        public Information getInformation(String identifier) {
            return Information.UNAVAILABLE;
        }

        void completePurchase(String productId) {
            Transaction transaction = new Transaction();
            transaction.setIdentifier(productId);
            observer.handlePurchase(transaction);
        }

        void completeRestore(String... productIds) {
            Transaction[] transactions = new Transaction[productIds.length];
            for (int i = 0; i < productIds.length; i++) {
                Transaction t = new Transaction();
                t.setIdentifier(productIds[i]);
                transactions[i] = t;
            }
            observer.handleRestore(transactions);
        }
    }
}
