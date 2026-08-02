package com.keplersharvest.colony;

/** The gameplay effect a module grants once fully repaired. */
public enum BenefitKind {
    /** Raises the player's maximum energy. */
    MAX_ENERGY,
    /** Watered soil stays wet for an extra day. */
    WATER_RETENTION,
    /** Marks undiscovered points of interest and long-range signals on the map. */
    SURVEY,
    /** Purely narrative. */
    NONE
}
