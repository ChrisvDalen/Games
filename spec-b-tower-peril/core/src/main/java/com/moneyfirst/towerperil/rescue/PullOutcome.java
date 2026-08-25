package com.moneyfirst.towerperil.rescue;

import java.util.Collections;
import java.util.List;

/** The result of resolving a single pin pull. */
public final class PullOutcome {
    private final Pin pinPulled;
    private final List<Character> rescued;
    private final List<Character> lost;

    public PullOutcome(Pin pinPulled, List<Character> rescued, List<Character> lost) {
        this.pinPulled = pinPulled;
        this.rescued = Collections.unmodifiableList(rescued);
        this.lost = Collections.unmodifiableList(lost);
    }

    public Pin getPinPulled() {
        return pinPulled;
    }

    /** Characters that slid off the grid's open exit this pull. */
    public List<Character> getRescued() {
        return rescued;
    }

    /** Characters caught by a hazard that stood between them and the exit this pull. */
    public List<Character> getLost() {
        return lost;
    }

    @Override
    public String toString() {
        return "PullOutcome{pin=" + pinPulled.getId() + ", rescued=" + rescued.size() + ", lost=" + lost.size() + "}";
    }
}
