package com.moneyfirst.towerperil.economy;

/**
 * Tracks the Tower Peril season pass subscription: whether it's active, when
 * it next renews/expires, and daily/weekly bonus-claim entitlements. Time is
 * expressed as caller-supplied epoch-day / epoch-week numbers (not wall
 * clock reads) so this class stays pure and deterministically testable -
 * platform code passes {@code LocalDate.now().toEpochDay()}.
 */
public final class SeasonPassState {
    public static final int RENEWAL_PERIOD_DAYS = 30;
    public static final int DAILY_BONUS_GEMS = 20;
    public static final int WEEKLY_BONUS_GEMS = 150;

    private boolean active;
    private long renewalEpochDay;
    private long lastDailyClaimEpochDay = Long.MIN_VALUE;
    private long lastWeeklyClaimEpochWeek = Long.MIN_VALUE;

    private SeasonPassState(boolean active, long renewalEpochDay) {
        this.active = active;
        this.renewalEpochDay = renewalEpochDay;
    }

    public static SeasonPassState inactive() {
        return new SeasonPassState(false, Long.MIN_VALUE);
    }

    public static SeasonPassState purchasedOn(long todayEpochDay) {
        return new SeasonPassState(true, todayEpochDay + RENEWAL_PERIOD_DAYS);
    }

    /** True if the pass was ever purchased and today is still before its renewal date. */
    public boolean isActive(long todayEpochDay) {
        return active && todayEpochDay < renewalEpochDay;
    }

    /** Extends the pass another {@link #RENEWAL_PERIOD_DAYS} days from today - called on a successful recurring charge. */
    public void renew(long todayEpochDay) {
        active = true;
        renewalEpochDay = todayEpochDay + RENEWAL_PERIOD_DAYS;
    }

    /** Marks the subscription lapsed (e.g. billing failed / user cancelled and the period elapsed). */
    public void expire() {
        active = false;
    }

    public long getRenewalEpochDay() {
        return renewalEpochDay;
    }

    /**
     * Grants the daily entitlement if the pass is active and it hasn't
     * already been claimed today.
     *
     * @return true if gems were granted.
     */
    public boolean claimDaily(long todayEpochDay, PlayerEconomy economy) {
        if (!isActive(todayEpochDay) || lastDailyClaimEpochDay == todayEpochDay) {
            return false;
        }
        lastDailyClaimEpochDay = todayEpochDay;
        economy.addGems(DAILY_BONUS_GEMS);
        return true;
    }

    /**
     * Grants the weekly entitlement if the pass is active and this calendar
     * week hasn't already been claimed.
     *
     * @return true if gems were granted.
     */
    public boolean claimWeekly(long todayEpochDay, PlayerEconomy economy) {
        long week = Math.floorDiv(todayEpochDay, 7L);
        if (!isActive(todayEpochDay) || lastWeeklyClaimEpochWeek == week) {
            return false;
        }
        lastWeeklyClaimEpochWeek = week;
        economy.addGems(WEEKLY_BONUS_GEMS);
        return true;
    }
}
