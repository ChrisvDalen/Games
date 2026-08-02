package com.keplersharvest.inventory;

/** Fixed set of item kinds. Drives UI grouping and a few gameplay rules. */
public enum ItemCategory {
    /** Raw materials: minerals, salvage, harvested fibre. */
    RESOURCE,
    /** Plantable in tilled soil. */
    SEED,
    /** Produce harvested from a mature crop. */
    CROP,
    /** Non-stackable, used with the tool key. */
    TOOL,
    /** Crafted intermediate used by module repairs. */
    COMPONENT,
    /** Story object; never consumed. */
    ARTIFACT
}
