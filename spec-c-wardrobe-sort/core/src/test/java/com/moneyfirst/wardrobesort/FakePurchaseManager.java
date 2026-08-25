package com.moneyfirst.wardrobesort;

import com.badlogic.gdx.pay.Information;
import com.badlogic.gdx.pay.PurchaseManager;
import com.badlogic.gdx.pay.PurchaseManagerConfig;
import com.badlogic.gdx.pay.PurchaseObserver;
import com.badlogic.gdx.pay.Transaction;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * An in-memory {@link PurchaseManager} test double: no real store, no
 * network - it just remembers what {@link #purchase(String)} was called
 * with and lets the test drive {@link PurchaseObserver} callbacks directly,
 * the same way a real store backend eventually would. Used only by
 * {@link GdxPayIapServiceTest} to verify {@link GdxPayIapService}'s wiring
 * without needing a real Google Play / App Store connection.
 */
final class FakePurchaseManager implements PurchaseManager {

    PurchaseObserver observer;
    PurchaseManagerConfig installedConfig;
    boolean installedFlag = false;
    final List<String> purchaseCalls = new ArrayList<>();
    boolean restoreCalled = false;

    @Override
    public String storeName() {
        return "FakeStore";
    }

    @Override
    public void install(PurchaseObserver observer, PurchaseManagerConfig config, boolean autoFetchInformation) {
        this.observer = observer;
        this.installedConfig = config;
        this.installedFlag = true;
        observer.handleInstall();
    }

    @Override
    public boolean installed() {
        return installedFlag;
    }

    @Override
    public void dispose() {
        installedFlag = false;
    }

    @Override
    public void purchase(String identifier) {
        purchaseCalls.add(identifier);
    }

    @Override
    public void purchaseRestore() {
        restoreCalled = true;
    }

    @Override
    public Information getInformation(String identifier) {
        return Information.UNAVAILABLE;
    }

    /** Test helper: simulates the store confirming a successful purchase. */
    void completePurchase(String productId) {
        Transaction t = new Transaction();
        t.setIdentifier(productId);
        t.setPurchaseTime(new Date());
        t.setStoreName(storeName());
        observer.handlePurchase(t);
    }
}
