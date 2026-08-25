package com.moneyfirst.pourperfect.cafe;

/**
 * Cosmetic-only upgrades for the cafe meta-progression layer. None of these affect puzzle
 * difficulty or grant gameplay currency/credits - they exist purely to give cups (earned by
 * solving levels) somewhere satisfying to spend once the cafe is unlocked.
 */
public enum CafeUpgrade {
    WARM_LIGHTING("warm_lighting", "Warm Lighting", 20),
    POTTED_PLANTS("potted_plants", "Potted Plants", 35),
    VINYL_JUKEBOX("vinyl_jukebox", "Vinyl Jukebox", 50),
    NEON_SIGN("neon_sign", "Neon Sign", 75),
    ESPRESSO_BAR("espresso_bar", "Espresso Bar", 120);

    private final String id;
    private final String displayName;
    private final int costInCups;

    CafeUpgrade(String id, String displayName, int costInCups) {
        this.id = id;
        this.displayName = displayName;
        this.costInCups = costInCups;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getCostInCups() {
        return costInCups;
    }
}
