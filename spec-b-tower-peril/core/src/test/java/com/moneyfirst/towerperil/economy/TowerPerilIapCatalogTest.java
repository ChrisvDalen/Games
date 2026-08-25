package com.moneyfirst.towerperil.economy;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TowerPerilIapCatalogTest {

    @Test
    void onlySeasonPassIsFlaggedAsASubscription() {
        for (TowerPerilIapCatalog product : TowerPerilIapCatalog.values()) {
            if (product == TowerPerilIapCatalog.SEASON_PASS) {
                assertTrue(product.isSubscription());
            } else {
                assertFalse(product.isSubscription(), product + " must not be a subscription");
            }
        }
    }

    @Test
    void gemBonusCurveIncreasesWithPackSize() {
        assertEquals(0, TowerPerilIapCatalog.GEMS_SMALL.getBonusPercent());
        assertTrue(TowerPerilIapCatalog.GEMS_MEDIUM.getBonusPercent() > TowerPerilIapCatalog.GEMS_SMALL.getBonusPercent());
        assertTrue(TowerPerilIapCatalog.GEMS_LARGE.getBonusPercent() > TowerPerilIapCatalog.GEMS_MEDIUM.getBonusPercent());
        assertTrue(TowerPerilIapCatalog.GEMS_HUGE.getBonusPercent() > TowerPerilIapCatalog.GEMS_LARGE.getBonusPercent());
    }

    @Test
    void totalGemsIncludesTheBonusPercent() {
        assertEquals(550 + 55, TowerPerilIapCatalog.GEMS_MEDIUM.totalGems());
        assertEquals(2800 + 1120, TowerPerilIapCatalog.GEMS_HUGE.totalGems());
    }

    @Test
    void applyingAGemPackCreditsTotalGemsToTheEconomy() {
        PlayerEconomy economy = new PlayerEconomy();
        TowerPerilIapCatalog.GEMS_LARGE.apply(economy, 0L);
        assertEquals(TowerPerilIapCatalog.GEMS_LARGE.totalGems(), economy.getGems());
    }

    @Test
    void applyingTheStarterPackGrantsUnitsNotGems() {
        PlayerEconomy economy = new PlayerEconomy();
        TowerPerilIapCatalog.STARTER_UNIT_PACK.apply(economy, 0L);
        assertEquals(0, economy.getGems());
        assertTrue(economy.getRoster().size() >= 3);
    }

    @Test
    void applyingSeasonPassActivatesIt() {
        PlayerEconomy economy = new PlayerEconomy();
        long today = 500L;
        TowerPerilIapCatalog.SEASON_PASS.apply(economy, today);
        assertTrue(economy.getSeasonPassState().isActive(today));
    }

    @Test
    void reApplyingSeasonPassWhileActiveExtendsFromToday() {
        PlayerEconomy economy = new PlayerEconomy();
        TowerPerilIapCatalog.SEASON_PASS.apply(economy, 0L);
        long renewalPoint = economy.getSeasonPassState().getRenewalEpochDay() - 1;

        TowerPerilIapCatalog.SEASON_PASS.apply(economy, renewalPoint);

        assertTrue(economy.getSeasonPassState().isActive(renewalPoint + SeasonPassState.RENEWAL_PERIOD_DAYS - 1));
    }

    @Test
    void byProductIdResolvesEveryCatalogEntry() {
        for (TowerPerilIapCatalog product : TowerPerilIapCatalog.values()) {
            assertEquals(product, TowerPerilIapCatalog.byProductId(product.getProductId()));
        }
    }

    @Test
    void byProductIdRejectsUnknownIds() {
        try {
            TowerPerilIapCatalog.byProductId("does_not_exist");
            throw new AssertionError("Expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            // expected
        }
    }
}
