package com.moneyfirst.pourperfect.cafe;

import com.badlogic.gdx.Preferences;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Minimal cafe meta-progression: cups (earned one per solved level via {@link #CUPS_PER_SOLVE})
 * spent on cosmetic {@link CafeUpgrade}s. The cafe itself is locked until either the player owns
 * the {@code CAFE_EXPANSION} IAP or reaches level 20, whichever comes first - see
 * {@link #refreshEligibility(boolean, int)}. Persisted via libGDX {@link Preferences}, same
 * pattern as {@code LevelProgressionState}.
 */
public final class CafeProgress {

    static final String KEY_CUPS = "pp_cafe_cups";
    static final String KEY_OPEN = "pp_cafe_open";
    static final String KEY_UNLOCKED_UPGRADES = "pp_cafe_unlocked_upgrades";
    private static final String UNLOCKED_SEPARATOR = ",";

    public static final int CAFE_EXPANSION_LEVEL_UNLOCK = 20;
    public static final int CUPS_PER_SOLVE = 5;

    private final Preferences preferences;
    private int cups;
    private boolean open;
    private final Set<String> unlockedUpgradeIds = new LinkedHashSet<>();

    private CafeProgress(Preferences preferences) {
        this.preferences = preferences;
    }

    public static CafeProgress load(Preferences preferences) {
        CafeProgress progress = new CafeProgress(preferences);
        progress.cups = preferences.getInteger(KEY_CUPS, 0);
        progress.open = preferences.getBoolean(KEY_OPEN, false);
        String stored = preferences.getString(KEY_UNLOCKED_UPGRADES, "");
        if (!stored.isEmpty()) {
            for (String id : stored.split(UNLOCKED_SEPARATOR)) {
                if (!id.isBlank()) {
                    progress.unlockedUpgradeIds.add(id);
                }
            }
        }
        return progress;
    }

    public void save() {
        preferences.putInteger(KEY_CUPS, cups);
        preferences.putBoolean(KEY_OPEN, open);
        preferences.putString(KEY_UNLOCKED_UPGRADES, String.join(UNLOCKED_SEPARATOR, unlockedUpgradeIds));
        preferences.flush();
    }

    public int getCups() {
        return cups;
    }

    public boolean isOpen() {
        return open;
    }

    public Set<String> getUnlockedUpgradeIds() {
        return Collections.unmodifiableSet(unlockedUpgradeIds);
    }

    public boolean isUnlocked(CafeUpgrade upgrade) {
        return unlockedUpgradeIds.contains(upgrade.getId());
    }

    /** Call after every solved level - the sole source of cafe currency. */
    public void earnCupsForSolve() {
        cups += CUPS_PER_SOLVE;
    }

    /**
     * Re-evaluates whether the cafe should be open. Once open, it stays open (an expired/refunded
     * IAP won't lock a player out of cosmetics they already unlocked with earned cups).
     */
    public void refreshEligibility(boolean cafeExpansionOwned, int currentLevelIndex) {
        if (cafeExpansionOwned || currentLevelIndex >= CAFE_EXPANSION_LEVEL_UNLOCK) {
            open = true;
        }
    }

    /** @return true if the purchase succeeded; false (with no state change) if not open, already
     *          owned, or insufficient cups. */
    public boolean purchaseUpgrade(CafeUpgrade upgrade) {
        if (!open || isUnlocked(upgrade) || cups < upgrade.getCostInCups()) {
            return false;
        }
        cups -= upgrade.getCostInCups();
        unlockedUpgradeIds.add(upgrade.getId());
        return true;
    }
}
