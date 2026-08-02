package com.keplersharvest.game;

import com.keplersharvest.world.GridPoint;

import java.util.Optional;

/**
 * What the player is currently pointed at, and the prompt to show for it.
 *
 * @param refId the colonist, module, log or object id behind the prompt
 */
public record InteractionTarget(Kind kind, GridPoint tile, String prompt, Optional<String> refId) {

    /** Categories of thing the interaction indicator can highlight. */
    public enum Kind {
        COLONIST,
        MODULE,
        RESOURCE_NODE,
        CREW_LOG,
        CRAFTING_STATION,
        BED,
        SIGN,
        HARVESTABLE_CROP,
        GROWING_CROP,
        SOIL
    }

    public static InteractionTarget of(Kind kind, GridPoint tile, String prompt, String refId) {
        return new InteractionTarget(kind, tile, prompt, Optional.ofNullable(refId));
    }
}
