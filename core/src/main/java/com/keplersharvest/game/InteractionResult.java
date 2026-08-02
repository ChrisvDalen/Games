package com.keplersharvest.game;

import com.keplersharvest.dialogue.DialogueRunner;
import com.keplersharvest.mystery.CrewLogDefinition;

/**
 * What an interaction did, and what the UI should open as a result.
 *
 * <p>Returning this instead of calling into screens keeps the interaction rules free of any
 * dependency on Scene2D.
 */
public sealed interface InteractionResult {

    /** Nothing in range, or the action was refused. */
    record Nothing(String reason) implements InteractionResult {
    }

    /** Handled; just show the text. */
    record Handled(String message) implements InteractionResult {
    }

    record StartDialogue(String colonistId, String colonistName, DialogueRunner runner) implements InteractionResult {
    }

    record OpenCrafting(String stationId, String stationName) implements InteractionResult {
    }

    record ReadCrewLog(CrewLogDefinition log) implements InteractionResult {
    }

    /** The player used a bed; the caller decides whether to confirm first. */
    record OfferSleep() implements InteractionResult {
    }
}
