package com.keplersharvest.farming;

import com.keplersharvest.world.GridPoint;

/**
 * Outcome of a farming action. Modelled as a sealed hierarchy so the UI can report exactly why an
 * action did nothing instead of silently swallowing it.
 */
public sealed interface FarmResult {

    GridPoint position();

    record Tilled(GridPoint position) implements FarmResult {
    }

    record Planted(GridPoint position, CropDefinition crop) implements FarmResult {
    }

    record Watered(GridPoint position) implements FarmResult {
    }

    record Harvested(GridPoint position, String produceItemId, int amount, boolean regrew) implements FarmResult {
    }

    /** A withered plant was pulled up, freeing the soil. */
    record Cleared(GridPoint position) implements FarmResult {
    }

    record Rejected(GridPoint position, String reason) implements FarmResult {
    }

    default boolean succeeded() {
        return !(this instanceof Rejected);
    }

    /** Short player-facing description, used by the HUD toast line. */
    default String message() {
        return switch (this) {
            case Tilled t -> "Soil broken open.";
            case Planted p -> "Planted " + p.crop().name() + ".";
            case Watered w -> "Watered.";
            case Harvested h -> "Harvested " + h.amount() + " x " + h.produceItemId().replace('_', ' ')
                    + (h.regrew() ? " (the plant will bear again)" : "");
            case Cleared c -> "Cleared the dead plant.";
            case Rejected r -> r.reason();
        };
    }
}
