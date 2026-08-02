package com.keplersharvest.mystery;

import java.util.List;
import java.util.Objects;

/**
 * The conclusion the player reaches once enough of the picture is assembled.
 *
 * @param requiredLogs    how many crew logs must be recovered
 * @param requiredEvidence evidence ids that must all be in the journal
 */
public record RevealDefinition(
        String id,
        String title,
        List<String> body,
        int requiredLogs,
        List<String> requiredEvidence) {

    public RevealDefinition {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(title, "title");
        body = List.copyOf(Objects.requireNonNull(body, "body"));
        requiredEvidence = List.copyOf(Objects.requireNonNull(requiredEvidence, "requiredEvidence"));
    }
}
