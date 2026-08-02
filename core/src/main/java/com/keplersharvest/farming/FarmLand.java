package com.keplersharvest.farming;

import com.keplersharvest.world.GridPoint;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.random.RandomGenerator;

/**
 * All cultivated tiles of one map, plus the day-to-day growth rules.
 *
 * <p>Holds no reference to rendering or input. {@link #advanceDay()} is the single place where time
 * changes crops, which makes multi-day growth straightforward to test.
 */
public final class FarmLand {

    private final String mapId;
    private final Map<String, CropDefinition> crops;
    private final Predicate<GridPoint> farmable;
    private final RandomGenerator random;
    private final Map<GridPoint, FarmPlot> plots = new LinkedHashMap<>();

    public FarmLand(String mapId,
                    Map<String, CropDefinition> crops,
                    Predicate<GridPoint> farmable,
                    RandomGenerator random) {
        this.mapId = Objects.requireNonNull(mapId, "mapId");
        this.crops = Map.copyOf(Objects.requireNonNull(crops, "crops"));
        this.farmable = Objects.requireNonNull(farmable, "farmable");
        this.random = Objects.requireNonNull(random, "random");
    }

    public String mapId() {
        return mapId;
    }

    public Optional<FarmPlot> plotAt(GridPoint position) {
        return Optional.ofNullable(plots.get(position));
    }

    public Collection<FarmPlot> plots() {
        return plots.values();
    }

    /** Adds or replaces a plot without applying rules; used when loading a save. */
    public FarmPlot restorePlot(GridPoint position) {
        return plots.computeIfAbsent(position, FarmPlot::new);
    }

    public FarmResult till(GridPoint position) {
        if (!farmable.test(position)) {
            return new FarmResult.Rejected(position, "This ground will not take a blade.");
        }
        FarmPlot plot = plots.computeIfAbsent(position, FarmPlot::new);
        if (plot.soil() == SoilState.TILLED) {
            return new FarmResult.Rejected(position, "Already tilled.");
        }
        plot.setSoil(SoilState.TILLED);
        return new FarmResult.Tilled(position);
    }

    public FarmResult plant(GridPoint position, String cropId) {
        CropDefinition crop = crops.get(cropId);
        if (crop == null) {
            return new FarmResult.Rejected(position, "Unknown seed.");
        }
        FarmPlot plot = plots.get(position);
        if (plot == null || plot.soil() != SoilState.TILLED) {
            return new FarmResult.Rejected(position, "The soil needs tilling first.");
        }
        if (plot.hasCrop()) {
            return new FarmResult.Rejected(position, "Something is already growing here.");
        }
        plot.plant(cropId);
        return new FarmResult.Planted(position, crop);
    }

    public FarmResult water(GridPoint position) {
        FarmPlot plot = plots.get(position);
        if (plot == null || !plot.hasCrop()) {
            return new FarmResult.Rejected(position, "Nothing here to water.");
        }
        if (plot.withered()) {
            return new FarmResult.Rejected(position, "This plant is beyond saving.");
        }
        if (plot.watered()) {
            return new FarmResult.Rejected(position, "Already watered today.");
        }
        plot.setWatered(true);
        return new FarmResult.Watered(position);
    }

    /** True when {@link #harvest} would yield produce. Lets callers check inventory room first. */
    public boolean readyToHarvest(GridPoint position) {
        return plotAt(position)
                .filter(FarmPlot::hasCrop)
                .filter(plot -> !plot.withered())
                .flatMap(plot -> plot.cropId().map(crops::get)
                        .map(crop -> crop.matureAt(plot.grownDays())))
                .orElse(false);
    }

    public Optional<CropDefinition> cropAt(GridPoint position) {
        return plotAt(position).flatMap(FarmPlot::cropId).map(crops::get);
    }

    public FarmResult harvest(GridPoint position) {
        FarmPlot plot = plots.get(position);
        if (plot == null || !plot.hasCrop()) {
            return new FarmResult.Rejected(position, "Nothing to harvest.");
        }
        if (plot.withered()) {
            plot.clearCrop();
            return new FarmResult.Cleared(position);
        }
        CropDefinition crop = crops.get(plot.cropId().orElseThrow());
        if (crop == null) {
            plot.clearCrop();
            return new FarmResult.Cleared(position);
        }
        if (!crop.matureAt(plot.grownDays())) {
            int left = crop.daysToMaturity() - plot.grownDays();
            return new FarmResult.Rejected(position, "Not ready - about " + left + " more watered day"
                    + (left == 1 ? "" : "s") + ".");
        }
        int span = crop.produceMax() - crop.produceMin() + 1;
        int amount = crop.produceMin() + random.nextInt(span);
        boolean regrew = crop.regrows();
        if (regrew) {
            plot.setGrownDays(Math.max(0, crop.daysToMaturity() - crop.regrowDays()));
            plot.setDryDays(0);
        } else {
            plot.clearCrop();
        }
        return new FarmResult.Harvested(position, crop.produceItemId(), amount, regrew);
    }

    /**
     * Applies one night of growth to every plot.
     *
     * <p>A crop advances when it was watered that day (or does not need water). Otherwise it counts
     * a dry day and eventually withers. Watering never carries over to the next day.
     */
    public void advanceDay() {
        for (FarmPlot plot : plots.values()) {
            if (!plot.hasCrop() || plot.withered()) {
                plot.setWatered(false);
                continue;
            }
            CropDefinition crop = crops.get(plot.cropId().orElseThrow());
            if (crop == null) {
                plot.setWatered(false);
                continue;
            }
            if (plot.watered() || !crop.requiresWater()) {
                plot.setGrownDays(plot.grownDays() + 1);
                plot.setDryDays(0);
            } else {
                plot.setDryDays(plot.dryDays() + 1);
                if (crop.witherAfterDryDays() > 0 && plot.dryDays() >= crop.witherAfterDryDays()) {
                    plot.setWithered(true);
                }
            }
            plot.setWatered(false);
        }
    }

    public int plantedCount() {
        return (int) plots.values().stream().filter(FarmPlot::hasCrop).count();
    }
}
