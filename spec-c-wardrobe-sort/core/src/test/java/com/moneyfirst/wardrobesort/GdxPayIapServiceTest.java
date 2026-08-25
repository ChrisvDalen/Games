package com.moneyfirst.wardrobesort;

import com.badlogic.gdx.pay.Transaction;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GdxPayIapServiceTest {

    @Test
    void installsEveryCatalogProductAsAnEntitlementOfferAndBecomesReady() {
        FakePurchaseManager fake = new FakePurchaseManager();

        GdxPayIapService service = new GdxPayIapService(fake);

        assertTrue(fake.installedFlag);
        assertTrue(service.isReady());
        assertEquals(WardrobeIapCatalog.values().length, fake.installedConfig.getOfferCount());
        for (WardrobeIapCatalog offer : WardrobeIapCatalog.values()) {
            assertTrue(fake.installedConfig.getOffer(offer.productId()) != null,
                "PurchaseManagerConfig should register an offer for " + offer.productId());
        }
    }

    @Test
    void purchaseDelegatesToPurchaseManagerWithTheProductId() {
        FakePurchaseManager fake = new FakePurchaseManager();
        GdxPayIapService service = new GdxPayIapService(fake);

        service.purchase(WardrobeIapCatalog.WS_REMOVE_ADS, () -> {
        }, () -> {
        });

        assertEquals(1, fake.purchaseCalls.size());
        assertEquals("ws_remove_ads", fake.purchaseCalls.get(0));
    }

    @Test
    void successfulPurchaseGrantsTheUnlockAndFiresOnSuccess() {
        FakePurchaseManager fake = new FakePurchaseManager();
        GdxPayIapService service = new GdxPayIapService(fake);
        AtomicBoolean succeeded = new AtomicBoolean(false);
        AtomicBoolean failed = new AtomicBoolean(false);

        service.purchase(WardrobeIapCatalog.WS_PACK_STREETWEAR, () -> succeeded.set(true), () -> failed.set(true));
        fake.completePurchase("ws_pack_streetwear");

        assertTrue(succeeded.get());
        assertFalse(failed.get());
        assertTrue(service.isOwned(WardrobeIapCatalog.WS_PACK_STREETWEAR));
        assertTrue(service.unlocks().owns(WardrobeIapCatalog.WS_PACK_STREETWEAR));
    }

    @Test
    void purchaseErrorFiresOnFailureAndGrantsNothing() {
        FakePurchaseManager fake = new FakePurchaseManager();
        GdxPayIapService service = new GdxPayIapService(fake);
        AtomicBoolean succeeded = new AtomicBoolean(false);
        AtomicBoolean failed = new AtomicBoolean(false);

        service.purchase(WardrobeIapCatalog.WS_REMOVE_ADS, () -> succeeded.set(true), () -> failed.set(true));
        fake.observer.handlePurchaseError(new RuntimeException("simulated store error"));

        assertFalse(succeeded.get());
        assertTrue(failed.get());
        assertFalse(service.isOwned(WardrobeIapCatalog.WS_REMOVE_ADS));
    }

    @Test
    void purchaseCanceledFiresOnFailure() {
        FakePurchaseManager fake = new FakePurchaseManager();
        GdxPayIapService service = new GdxPayIapService(fake);
        AtomicBoolean failed = new AtomicBoolean(false);

        service.purchase(WardrobeIapCatalog.WS_REMOVE_ADS, () -> {
        }, () -> failed.set(true));
        fake.observer.handlePurchaseCanceled();

        assertTrue(failed.get());
        assertFalse(service.isOwned(WardrobeIapCatalog.WS_REMOVE_ADS));
    }

    @Test
    void restoreDelegatesAndGrantsEveryRestoredTransaction() {
        FakePurchaseManager fake = new FakePurchaseManager();
        GdxPayIapService service = new GdxPayIapService(fake);
        AtomicBoolean completed = new AtomicBoolean(false);

        service.restore(() -> completed.set(true), () -> {
        });
        assertTrue(fake.restoreCalled);

        Transaction t1 = new Transaction();
        t1.setIdentifier("ws_remove_ads");
        t1.setPurchaseTime(new Date());
        Transaction t2 = new Transaction();
        t2.setIdentifier("ws_pack_formal");
        t2.setPurchaseTime(new Date());
        fake.observer.handleRestore(new Transaction[] {t1, t2});

        assertTrue(completed.get());
        assertTrue(service.isOwned(WardrobeIapCatalog.WS_REMOVE_ADS));
        assertTrue(service.isOwned(WardrobeIapCatalog.WS_PACK_FORMAL));
    }

    @Test
    void reversedTransactionIsNotGranted() {
        FakePurchaseManager fake = new FakePurchaseManager();
        GdxPayIapService service = new GdxPayIapService(fake);

        Transaction t = new Transaction();
        t.setIdentifier("ws_remove_ads");
        t.setPurchaseTime(new Date());
        t.setReversalTime(new Date()); // refunded/canceled after the fact

        fake.observer.handleRestore(new Transaction[] {t});

        assertFalse(service.isOwned(WardrobeIapCatalog.WS_REMOVE_ADS), "a reversed transaction must not grant ownership");
    }
}
