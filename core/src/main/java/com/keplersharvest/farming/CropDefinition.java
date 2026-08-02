package com.keplersharvest.farming;

import java.util.List;
import java.util.Objects;

/**
 * An alien crop, loaded from {@code config/crops.json}.
 *
 * @param stageDays       watered days spent in each visible growth stage; the sum is the time to
 *                        maturity, so different crops simply declare different lists
 * @param regrows         when true the plant returns to a late stage after harvest instead of dying
 * @param requiresWater   when false the crop grows on neglect (Cinder Moss)
 * @param witherAfterDryDays consecutive unwatered days the crop survives; 0 disables withering
 */
public record CropDefinition(
        String id,
        String name,
        String description,
        String seedItemId,
        String produceItemId,
        int produceMin,
        int produceMax,
        List<Integer> stageDays,
        boolean regrows,
        int regrowDays,
        boolean requiresWater,
        int witherAfterDryDays,
        String colour) {

    public CropDefinition {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(seedItemId, "seedItemId");
        Objects.requireNonNull(produceItemId, "produceItemId");
        stageDays = List.copyOf(Objects.requireNonNull(stageDays, "stageDays"));
        if (stageDays.isEmpty()) {
            throw new IllegalArgumentException("Crop " + id + " needs at least one growth stage");
        }
        if (stageDays.stream().anyMatch(d -> d < 1)) {
            throw new IllegalArgumentException("Crop " + id + " has a non-positive stage length");
        }
        if (produceMin < 1 || produceMax < produceMin) {
            throw new IllegalArgumentException("Crop " + id + " has an invalid produce range");
        }
        if (regrows && regrowDays < 1) {
            throw new IllegalArgumentException("Regrowing crop " + id + " needs a positive regrowDays");
        }
    }

    /** Watered days from seed to first harvest. */
    public int daysToMaturity() {
        return stageDays.stream().mapToInt(Integer::intValue).sum();
    }

    public int stageCount() {
        return stageDays.size();
    }

    /** Visible stage index (0-based) after {@code grownDays} of growth; clamped at the last stage. */
    public int stageFor(int grownDays) {
        int remaining = grownDays;
        for (int stage = 0; stage < stageDays.size(); stage++) {
            remaining -= stageDays.get(stage);
            if (remaining < 0) {
                return stage;
            }
        }
        return stageDays.size() - 1;
    }

    public boolean matureAt(int grownDays) {
        return grownDays >= daysToMaturity();
    }
}
