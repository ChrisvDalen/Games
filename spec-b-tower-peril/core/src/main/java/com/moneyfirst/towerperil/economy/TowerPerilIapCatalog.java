package com.moneyfirst.towerperil.economy;

import com.moneyfirst.towerperil.battle.Rarity;
import com.moneyfirst.towerperil.battle.Unit;
import com.moneyfirst.towerperil.battle.UnitType;

/**
 * Every purchasable product in Tower Peril. Product ids here must match the
 * in-app-purchase / subscription products configured in Play Console and
 * App Store Connect before release (see README.md TODOs).
 *
 * <p>Gem packs follow a classic gacha bonus curve - the bigger the pack, the
 * higher the bonus percentage on top of the linear €/gem rate - to reward
 * (and nudge toward) bigger spends.
 */
public enum TowerPerilIapCatalog {

    /** One-time starter bundle: a small guaranteed-rare unit pack for new players. */
    STARTER_UNIT_PACK("tp_starter_units", 3.99, false, 0, 0,
            "A guaranteed-rare starter squad to jump-start your roster.") {
        @Override
        public void apply(PlayerEconomy economy, long todayEpochDay) {
            economy.addUnit(Unit.rolled(UnitType.WARRIOR, Rarity.RARE));
            economy.addUnit(Unit.rolled(UnitType.ARCHER, Rarity.RARE));
            economy.addUnit(Unit.rolled(UnitType.MAGE, Rarity.COMMON));
        }
    },

    GEMS_SMALL("tp_gems_small", 0.99, false, 100, 0, "A small pouch of gems."),
    GEMS_MEDIUM("tp_gems_medium", 4.99, false, 550, 10, "A handful more gems, plus a 10% bonus."),
    GEMS_LARGE("tp_gems_large", 9.99, false, 1200, 20, "A big gem haul, plus a 20% bonus."),
    GEMS_HUGE("tp_gems_huge", 19.99, false, 2800, 40, "The whale-tier gem chest, plus a 40% bonus."),

    /** Recurring monthly subscription: daily + weekly bonus-gem entitlements. */
    SEASON_PASS("tp_season_pass", 7.99, true, 0, 0,
            "Monthly pass: daily and weekly bonus gem claims for as long as it's active.") {
        @Override
        public void apply(PlayerEconomy economy, long todayEpochDay) {
            SeasonPassState existing = economy.getSeasonPassState();
            if (existing.isActive(todayEpochDay)) {
                existing.renew(todayEpochDay);
            } else {
                economy.setSeasonPassState(SeasonPassState.purchasedOn(todayEpochDay));
            }
        }
    };

    private final String productId;
    private final double priceEur;
    private final boolean subscription;
    private final int gemAmount;
    private final int bonusPercent;
    private final String description;

    TowerPerilIapCatalog(String productId, double priceEur, boolean subscription,
                          int gemAmount, int bonusPercent, String description) {
        this.productId = productId;
        this.priceEur = priceEur;
        this.subscription = subscription;
        this.gemAmount = gemAmount;
        this.bonusPercent = bonusPercent;
        this.description = description;
    }

    public String getProductId() {
        return productId;
    }

    public double getPriceEur() {
        return priceEur;
    }

    public boolean isSubscription() {
        return subscription;
    }

    public int getGemAmount() {
        return gemAmount;
    }

    public int getBonusPercent() {
        return bonusPercent;
    }

    public String getDescription() {
        return description;
    }

    /** Base gems plus the bonus-% curve for bigger packs - what the player actually receives. */
    public int totalGems() {
        return gemAmount + (gemAmount * bonusPercent) / 100;
    }

    /** Applies this product's grant to the player's economy. Gem packs credit gems by default. */
    public void apply(PlayerEconomy economy, long todayEpochDay) {
        if (gemAmount > 0) {
            economy.addGems(totalGems());
        }
    }

    public static TowerPerilIapCatalog byProductId(String productId) {
        for (TowerPerilIapCatalog product : values()) {
            if (product.productId.equals(productId)) {
                return product;
            }
        }
        throw new IllegalArgumentException("Unknown Tower Peril product id: " + productId);
    }
}
