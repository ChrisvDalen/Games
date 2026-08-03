package com.keplersharvest.game;

import java.util.random.RandomGenerator;

/**
 * The game's random source: a SplitMix64 generator whose entire state is a single long.
 *
 * <p>That is the whole point of not using {@link java.util.Random}. A {@code Random} cannot say how
 * far along its stream it is, so a save file could only record the original seed, and reloading
 * would rewind every future roll — harvest sizes already rolled would come up again. Here the state
 * is readable and restorable, so a reloaded game continues the sequence it would have had if it had
 * never been saved.
 *
 * <p>SplitMix64 is the algorithm behind {@code SplittableRandom}: fast, one long of state, and more
 * than good enough for deciding how many pods a plant gives up.
 */
public final class WorldRandom implements RandomGenerator {

    private static final long GOLDEN_GAMMA = 0x9E3779B97F4A7C15L;

    private long state;

    public WorldRandom(long seed) {
        this.state = seed;
    }

    /** The current position in the stream, for a save file to record. */
    public long state() {
        return state;
    }

    /** Resumes the stream from a position previously returned by {@link #state()}. */
    public void restore(long state) {
        this.state = state;
    }

    @Override
    public long nextLong() {
        long z = (state += GOLDEN_GAMMA);
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }
}
