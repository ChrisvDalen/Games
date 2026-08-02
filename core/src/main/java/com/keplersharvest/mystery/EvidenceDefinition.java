package com.keplersharvest.mystery;

import java.util.Objects;

/**
 * One deduction in the evidence journal.
 *
 * @param source where the clue came from, e.g. "Recovered log", "Sefa Odim", "Oxygen Recycler"
 */
public record EvidenceDefinition(String id, String title, String text, String source) {

    public EvidenceDefinition {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(text, "text");
        Objects.requireNonNull(source, "source");
    }
}
