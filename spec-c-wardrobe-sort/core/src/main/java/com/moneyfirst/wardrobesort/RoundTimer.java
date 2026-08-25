package com.moneyfirst.wardrobesort;

/**
 * Countdown for the current round. Supports exactly one rewarded-video "+10s"
 * extension per round; a second attempt is refused.
 */
public final class RoundTimer {

    public static final float REWARDED_EXTENSION_SECONDS = 10f;

    private final float totalSeconds;
    private float remainingSeconds;
    private boolean extensionUsed = false;

    public RoundTimer(float totalSeconds) {
        if (totalSeconds <= 0f) {
            throw new IllegalArgumentException("totalSeconds must be > 0, was " + totalSeconds);
        }
        this.totalSeconds = totalSeconds;
        this.remainingSeconds = totalSeconds;
    }

    /** Advances the countdown by {@code deltaSeconds}, clamped at zero. */
    public void update(float deltaSeconds) {
        if (deltaSeconds < 0f) {
            throw new IllegalArgumentException("deltaSeconds must be >= 0, was " + deltaSeconds);
        }
        remainingSeconds = Math.max(0f, remainingSeconds - deltaSeconds);
    }

    public float remainingSeconds() {
        return remainingSeconds;
    }

    public float totalSeconds() {
        return totalSeconds;
    }

    public boolean isExpired() {
        return remainingSeconds <= 0f;
    }

    /**
     * Grants the one-time rewarded-video "+10s" extension for this round.
     *
     * @return {@code true} if the extension was applied, {@code false} if it
     *         had already been used this round (no state changes in that case).
     */
    public boolean extend() {
        if (extensionUsed) {
            return false;
        }
        extensionUsed = true;
        remainingSeconds += REWARDED_EXTENSION_SECONDS;
        return true;
    }

    public boolean isExtensionUsed() {
        return extensionUsed;
    }

    public boolean isExtensionAvailable() {
        return !extensionUsed;
    }
}
