package com.keplersharvest.mystery;

import java.util.List;
import java.util.Objects;

/**
 * A recording left by the colony's original crew.
 *
 * @param order     reading order, used to sort the journal
 * @param grantsEvidence journal entries this log establishes
 */
public record CrewLogDefinition(
        String id,
        String title,
        String author,
        String stardate,
        List<String> body,
        int order,
        List<String> grantsEvidence) {

    public CrewLogDefinition {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(author, "author");
        Objects.requireNonNull(stardate, "stardate");
        body = List.copyOf(Objects.requireNonNull(body, "body"));
        grantsEvidence = List.copyOf(Objects.requireNonNull(grantsEvidence, "grantsEvidence"));
        if (body.isEmpty()) {
            throw new IllegalArgumentException("Crew log " + id + " has no text");
        }
    }
}
