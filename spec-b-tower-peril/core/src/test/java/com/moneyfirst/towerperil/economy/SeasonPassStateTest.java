package com.moneyfirst.towerperil.economy;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SeasonPassStateTest {

    private static final long DAY_ZERO = 20_000L; // arbitrary epoch-day anchor

    @Test
    void inactiveByDefault() {
        SeasonPassState state = SeasonPassState.inactive();
        assertFalse(state.isActive(DAY_ZERO));
    }

    @Test
    void purchasingActivatesForTheRenewalPeriod() {
        SeasonPassState state = SeasonPassState.purchasedOn(DAY_ZERO);

        assertTrue(state.isActive(DAY_ZERO));
        assertTrue(state.isActive(DAY_ZERO + SeasonPassState.RENEWAL_PERIOD_DAYS - 1));
        assertFalse(state.isActive(DAY_ZERO + SeasonPassState.RENEWAL_PERIOD_DAYS));
        assertFalse(state.isActive(DAY_ZERO + SeasonPassState.RENEWAL_PERIOD_DAYS + 5));
    }

    @Test
    void renewingExtendsActivityAnotherFullPeriod() {
        SeasonPassState state = SeasonPassState.purchasedOn(DAY_ZERO);
        long justBeforeExpiry = DAY_ZERO + SeasonPassState.RENEWAL_PERIOD_DAYS - 1;

        state.renew(justBeforeExpiry);

        assertTrue(state.isActive(justBeforeExpiry));
        assertTrue(state.isActive(justBeforeExpiry + SeasonPassState.RENEWAL_PERIOD_DAYS - 1));
        assertFalse(state.isActive(justBeforeExpiry + SeasonPassState.RENEWAL_PERIOD_DAYS));
    }

    @Test
    void expireImmediatelyDeactivatesRegardlessOfRenewalDate() {
        SeasonPassState state = SeasonPassState.purchasedOn(DAY_ZERO);
        state.expire();
        assertFalse(state.isActive(DAY_ZERO));
    }

    @Test
    void dailyClaimGrantsGemsOnceThenBlocksUntilTheNextDay() {
        SeasonPassState state = SeasonPassState.purchasedOn(DAY_ZERO);
        PlayerEconomy economy = new PlayerEconomy();

        assertTrue(state.claimDaily(DAY_ZERO, economy));
        assertEquals(SeasonPassState.DAILY_BONUS_GEMS, economy.getGems());

        assertFalse(state.claimDaily(DAY_ZERO, economy), "Same-day daily claim must be blocked");
        assertEquals(SeasonPassState.DAILY_BONUS_GEMS, economy.getGems(), "Blocked claim must not grant gems again");

        assertTrue(state.claimDaily(DAY_ZERO + 1, economy), "Next day's claim must succeed");
        assertEquals(SeasonPassState.DAILY_BONUS_GEMS * 2L, economy.getGems());
    }

    @Test
    void weeklyClaimGrantsGemsOnceThenBlocksUntilTheNextWeek() {
        SeasonPassState state = SeasonPassState.purchasedOn(DAY_ZERO);
        PlayerEconomy economy = new PlayerEconomy();
        long week = Math.floorDiv(DAY_ZERO, 7L);
        long firstDayOfWeek = week * 7L;

        assertTrue(state.claimWeekly(firstDayOfWeek, economy));
        assertEquals(SeasonPassState.WEEKLY_BONUS_GEMS, economy.getGems());

        assertFalse(state.claimWeekly(firstDayOfWeek + 3, economy), "Same-week claim must be blocked");
        assertEquals(SeasonPassState.WEEKLY_BONUS_GEMS, economy.getGems());

        assertTrue(state.claimWeekly(firstDayOfWeek + 7, economy), "Next week's claim must succeed");
        assertEquals(SeasonPassState.WEEKLY_BONUS_GEMS * 2L, economy.getGems());
    }

    @Test
    void expiredPassCannotClaimEntitlements() {
        SeasonPassState state = SeasonPassState.purchasedOn(DAY_ZERO);
        PlayerEconomy economy = new PlayerEconomy();
        long afterExpiry = DAY_ZERO + SeasonPassState.RENEWAL_PERIOD_DAYS + 1;

        assertFalse(state.claimDaily(afterExpiry, economy));
        assertFalse(state.claimWeekly(afterExpiry, economy));
        assertEquals(0, economy.getGems());
    }
}
