package com.keplersharvest.farming;

import com.keplersharvest.world.GridPoint;

import java.util.Objects;
import java.util.Optional;

/**
 * Mutable state of a single farm tile.
 *
 * <p>Deliberately dumb: growth rules live in {@link FarmLand} so they can be read in one place.
 */
public final class FarmPlot {

    private final GridPoint position;
    private SoilState soil;
    private String cropId;
    private int grownDays;
    private int dryDays;
    private boolean watered;
    private boolean withered;

    public FarmPlot(GridPoint position) {
        this(position, SoilState.WILD);
    }

    public FarmPlot(GridPoint position, SoilState soil) {
        this.position = Objects.requireNonNull(position, "position");
        this.soil = Objects.requireNonNull(soil, "soil");
    }

    public GridPoint position() {
        return position;
    }

    public SoilState soil() {
        return soil;
    }

    public void setSoil(SoilState soil) {
        this.soil = Objects.requireNonNull(soil, "soil");
    }

    public Optional<String> cropId() {
        return Optional.ofNullable(cropId);
    }

    public boolean hasCrop() {
        return cropId != null;
    }

    public int grownDays() {
        return grownDays;
    }

    public int dryDays() {
        return dryDays;
    }

    public boolean watered() {
        return watered;
    }

    public boolean withered() {
        return withered;
    }

    void plant(String cropId) {
        this.cropId = Objects.requireNonNull(cropId, "cropId");
        this.grownDays = 0;
        this.dryDays = 0;
        this.withered = false;
    }

    void clearCrop() {
        this.cropId = null;
        this.grownDays = 0;
        this.dryDays = 0;
        this.watered = false;
        this.withered = false;
    }

    void setWatered(boolean watered) {
        this.watered = watered;
    }

    void setGrownDays(int grownDays) {
        this.grownDays = Math.max(0, grownDays);
    }

    void setDryDays(int dryDays) {
        this.dryDays = Math.max(0, dryDays);
    }

    void setWithered(boolean withered) {
        this.withered = withered;
    }

    /** Restores a plot from save data without running growth rules. */
    public void restore(SoilState soil, String cropId, int grownDays, int dryDays, boolean watered, boolean withered) {
        this.soil = Objects.requireNonNull(soil, "soil");
        this.cropId = cropId;
        this.grownDays = Math.max(0, grownDays);
        this.dryDays = Math.max(0, dryDays);
        this.watered = watered;
        this.withered = withered;
    }

    public boolean isEmptySoil() {
        return soil == SoilState.TILLED && cropId == null;
    }
}
