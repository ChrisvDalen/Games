package com.moneyfirst.pourperfect.iap;

/**
 * Thin, testable surface over the platform purchase manager. {@code core} game/screen code talks
 * only to this interface and to {@link PourPerfectIapCatalog}; {@link GdxPayIapService} is the
 * real implementation, wrapping gdx-pay's {@code PurchaseManager} per
 * {@code docs/MONEY_FIRST_ARCHITECTURE.md}.
 */
public interface IapService {

    /** Starts the platform purchase flow for {@code product}. Result arrives asynchronously. */
    void purchase(PourPerfectIapCatalog product);

    /** Whether the player currently owns {@code product} (entitlements only make sense to ask this of). */
    boolean isOwned(PourPerfectIapCatalog product);

    /** Asks the platform store to restore previous purchases (App Store review requirement on iOS). */
    void restore();
}
