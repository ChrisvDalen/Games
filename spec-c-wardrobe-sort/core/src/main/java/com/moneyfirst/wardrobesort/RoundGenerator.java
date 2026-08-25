package com.moneyfirst.wardrobesort;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;

/**
 * Builds a seeded target outfit plus a shuffled tray for a given round
 * number. Deterministic: the same (seed, roundNumber) pair always produces
 * byte-for-byte the same {@link Round} (same target garments, same tray
 * contents, same tray order, same timer).
 *
 * <p>Difficulty scales with round number: decoy count trends up and the
 * timer trends down, both clamped to sane floors/ceilings so a round is
 * always theoretically completable (the tray always contains all five
 * garments the target needs, and the timer never drops below
 * {@link #MIN_TIMER_SECONDS}).
 *
 * <p>Only ever draws from the fixed {@link GarmentCatalog#BASE} list - never
 * from cosmetic-pack garments - so owning/purchasing a {@link WardrobeIapCatalog}
 * cosmetic pack can never change what a timed round looks like.
 */
public final class RoundGenerator {

    /** Fewest decoys a round will ever have (round 1). */
    public static final int MIN_DECOYS = 2;
    /** Most decoys a round will ever have, regardless of how high the round number climbs. */
    public static final int MAX_DECOYS = 12;
    /** Every two rounds, one more decoy is added, up to {@link #MAX_DECOYS}. */
    private static final int ROUNDS_PER_DECOY_STEP = 2;

    /** Starting countdown for round 1. */
    public static final float START_TIMER_SECONDS = 45f;
    /** The timer never drops below this floor, no matter how high the round number climbs. */
    public static final float MIN_TIMER_SECONDS = 15f;
    /** Seconds shaved off the timer per round past the first. */
    public static final float TIMER_DECREMENT_PER_ROUND = 1.5f;

    private final List<Garment> catalog;
    private final Map<GarmentSlot, List<Garment>> bySlot;

    public RoundGenerator(List<Garment> catalog) {
        Objects.requireNonNull(catalog, "catalog");
        if (catalog.isEmpty()) {
            throw new IllegalArgumentException("catalog must not be empty");
        }
        this.catalog = List.copyOf(catalog);

        EnumMap<GarmentSlot, List<Garment>> index = new EnumMap<>(GarmentSlot.class);
        for (GarmentSlot slot : GarmentSlot.values()) {
            index.put(slot, new ArrayList<>());
        }
        for (Garment g : this.catalog) {
            index.get(g.slot()).add(g);
        }
        for (GarmentSlot slot : GarmentSlot.values()) {
            if (index.get(slot).isEmpty()) {
                throw new IllegalArgumentException("catalog has no garments for slot " + slot);
            }
            index.put(slot, Collections.unmodifiableList(new ArrayList<>(index.get(slot))));
        }
        this.bySlot = Collections.unmodifiableMap(index);
    }

    /** Convenience factory using the always-available base catalog. */
    public static RoundGenerator withBaseCatalog() {
        return new RoundGenerator(GarmentCatalog.BASE);
    }

    /**
     * Deterministically generates round {@code roundNumber} for {@code seed}.
     * The tray always contains every garment the target requires, plus a
     * scaled, capped number of decoys (same-slot garments the target does
     * NOT want), then the whole tray is shuffled.
     */
    public Round generateRound(int roundNumber, long seed) {
        if (roundNumber < 1) {
            throw new IllegalArgumentException("roundNumber must be >= 1, was " + roundNumber);
        }
        Random rng = new Random(mix(seed, roundNumber));

        EnumMap<GarmentSlot, Garment> targetBySlot = new EnumMap<>(GarmentSlot.class);
        for (GarmentSlot slot : GarmentSlot.values()) {
            List<Garment> options = bySlot.get(slot);
            Garment chosen = options.get(rng.nextInt(options.size()));
            targetBySlot.put(slot, chosen);
        }
        OutfitTarget target = new OutfitTarget(targetBySlot);

        List<Garment> tray = new ArrayList<>(targetBySlot.values());

        List<Garment> decoyPool = new ArrayList<>();
        for (GarmentSlot slot : GarmentSlot.values()) {
            Garment chosen = targetBySlot.get(slot);
            for (Garment candidate : bySlot.get(slot)) {
                if (!candidate.id().equals(chosen.id())) {
                    decoyPool.add(candidate);
                }
            }
        }
        Collections.shuffle(decoyPool, rng);

        int requestedDecoys = computeDecoyCount(roundNumber);
        int actualDecoys = Math.min(requestedDecoys, decoyPool.size());
        tray.addAll(decoyPool.subList(0, actualDecoys));

        Collections.shuffle(tray, rng);

        float timerSeconds = computeTimerSeconds(roundNumber);

        return new Round(roundNumber, seed, target, tray, timerSeconds);
    }

    /** Decoy count for a given round: rises every {@link #ROUNDS_PER_DECOY_STEP} rounds, capped at {@link #MAX_DECOYS}. */
    public static int computeDecoyCount(int roundNumber) {
        int raw = MIN_DECOYS + (roundNumber - 1) / ROUNDS_PER_DECOY_STEP;
        return Math.min(raw, MAX_DECOYS);
    }

    /** Countdown length for a given round: falls linearly, floored at {@link #MIN_TIMER_SECONDS}. */
    public static float computeTimerSeconds(int roundNumber) {
        float t = START_TIMER_SECONDS - (roundNumber - 1) * TIMER_DECREMENT_PER_ROUND;
        return Math.max(MIN_TIMER_SECONDS, t);
    }

    private static long mix(long seed, int roundNumber) {
        long h = seed ^ 0x9E3779B97F4A7C15L;
        h = h * 0x9E3779B97F4A7C15L + roundNumber;
        h ^= (h >>> 32);
        return h;
    }
}
