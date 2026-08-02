package com.keplersharvest.farming;

/** Whether a farm tile has been broken open for planting. */
public enum SoilState {
    /** Untouched regolith. Cannot hold seed. */
    WILD,
    /** Tilled and plantable. */
    TILLED
}
