package com.keplersharvest.world;

import java.util.Locale;
import java.util.Optional;

/**
 * The interactable things a map may place.
 *
 * <p>Maps name these in the {@code class}/{@code type} field of a Tiled object, so a new map can
 * introduce content without touching engine code.
 */
public enum MapObjectKind {
    /** Named arrival point. Referenced by portals and by the new-game start. */
    SPAWN,
    /** Walk-on transition to another map. */
    PORTAL,
    /** A repairable colony module. Property: {@code module}. */
    MODULE,
    /** Gatherable mineral, plant or salvage. Properties: {@code item}, {@code respawnDays}. */
    RESOURCE_NODE,
    /** Bench that can run recipes. Property: {@code station}. */
    CRAFTING_STATION,
    /** A discoverable crew log. Property: {@code log}. */
    CREW_LOG,
    /** Sleeping here ends the day. */
    BED,
    /** A named place that counts as "discovered" when the player stands on it. */
    LANDMARK,
    /** Readable flavour text. Property: {@code text}. */
    SIGN;

    public static Optional<MapObjectKind> parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(valueOf(raw.trim().toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
