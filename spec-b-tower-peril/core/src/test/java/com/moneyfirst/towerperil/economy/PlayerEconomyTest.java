package com.moneyfirst.towerperil.economy;

import com.moneyfirst.towerperil.battle.Rarity;
import com.moneyfirst.towerperil.battle.Unit;
import com.moneyfirst.towerperil.battle.UnitType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerEconomyTest {

    @Test
    void gemsStartAtZero() {
        assertEquals(0, new PlayerEconomy().getGems());
    }

    @Test
    void addGemsIncreasesBalance() {
        PlayerEconomy economy = new PlayerEconomy();
        economy.addGems(150);
        assertEquals(150, economy.getGems());
    }

    @Test
    void addingNegativeGemsThrowsAndLeavesBalanceUnchanged() {
        PlayerEconomy economy = new PlayerEconomy();
        economy.addGems(50);
        assertThrows(IllegalArgumentException.class, () -> economy.addGems(-10));
        assertEquals(50, economy.getGems());
    }

    @Test
    void spendingMoreThanTheBalanceThrowsAndNeverGoesNegative() {
        PlayerEconomy economy = new PlayerEconomy();
        economy.addGems(100);

        assertThrows(IllegalStateException.class, () -> economy.spendGems(101));
        assertEquals(100, economy.getGems(), "A rejected overspend must not touch the balance");
        assertTrue(economy.getGems() >= 0);
    }

    @Test
    void spendingExactBalanceIsAllowedAndLeavesZero() {
        PlayerEconomy economy = new PlayerEconomy();
        economy.addGems(100);
        economy.spendGems(100);
        assertEquals(0, economy.getGems());
    }

    @Test
    void spendingNegativeAmountThrows() {
        PlayerEconomy economy = new PlayerEconomy();
        economy.addGems(10);
        assertThrows(IllegalArgumentException.class, () -> economy.spendGems(-5));
    }

    @Test
    void manyRandomSpendsAndGrantsNeverProduceANegativeBalance() {
        PlayerEconomy economy = new PlayerEconomy();
        long[] deltas = {50, -30, 20, -100, 5, -1000, 40, -40, 0, 1000, -999};
        for (long delta : deltas) {
            if (delta >= 0) {
                economy.addGems(delta);
            } else {
                long amount = -delta;
                if (economy.canAfford(amount)) {
                    economy.spendGems(amount);
                } else {
                    assertThrows(IllegalStateException.class, () -> economy.spendGems(amount));
                }
            }
            assertTrue(economy.getGems() >= 0, "Balance went negative after delta " + delta);
        }
    }

    @Test
    void canAffordReflectsCurrentBalance() {
        PlayerEconomy economy = new PlayerEconomy();
        economy.addGems(75);
        assertTrue(economy.canAfford(75));
        assertTrue(economy.canAfford(0));
        assertTrue(!economy.canAfford(76));
    }

    @Test
    void rosterTracksAddedUnits() {
        PlayerEconomy economy = new PlayerEconomy();
        Unit unit = new Unit("u1", UnitType.WARRIOR, Rarity.COMMON);
        economy.addUnit(unit);
        assertEquals(1, economy.getRoster().size());
        assertEquals(unit, economy.getRoster().get(0));
    }

    @Test
    void rosterViewIsUnmodifiableFromOutside() {
        PlayerEconomy economy = new PlayerEconomy();
        assertThrows(UnsupportedOperationException.class,
                () -> economy.getRoster().add(new Unit("x", UnitType.TANK, Rarity.COMMON)));
    }
}
