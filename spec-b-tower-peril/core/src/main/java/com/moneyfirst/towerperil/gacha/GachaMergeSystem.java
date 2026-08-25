package com.moneyfirst.towerperil.gacha;

import com.moneyfirst.towerperil.battle.Rarity;
import com.moneyfirst.towerperil.battle.Unit;
import com.moneyfirst.towerperil.battle.UnitType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Gacha rolling (exact weighted rarity RNG) and merge-3 upgrading.
 *
 * <p>Weights: COMMON 60%, RARE 25%, EPIC 12%, LEGENDARY 3%, drawn from a
 * single {@code nextInt(100)} against a cumulative table so the exact
 * published odds hold precisely (no floating-point drift) and rolls are
 * fully reproducible from a seeded {@link Random}.
 */
public final class GachaMergeSystem {

    /** Rarity, weight out of 100 - must sum to 100. */
    private static final Rarity[] RARITY_ORDER = {Rarity.COMMON, Rarity.RARE, Rarity.EPIC, Rarity.LEGENDARY};
    private static final int[] WEIGHTS = {60, 25, 12, 3};
    private static final int TOTAL_WEIGHT = 100;
    private static final int MERGE_GROUP_SIZE = 3;

    static {
        int sum = 0;
        for (int w : WEIGHTS) {
            sum += w;
        }
        if (sum != TOTAL_WEIGHT) {
            throw new ExceptionInInitializerError("Gacha weights must sum to 100, got " + sum);
        }
    }

    private final Random rng;

    public GachaMergeSystem(Random rng) {
        this.rng = rng;
    }

    public static Rarity rollRarity(Random rng) {
        int roll = rng.nextInt(TOTAL_WEIGHT);
        int cumulative = 0;
        for (int i = 0; i < RARITY_ORDER.length; i++) {
            cumulative += WEIGHTS[i];
            if (roll < cumulative) {
                return RARITY_ORDER[i];
            }
        }
        // Unreachable given weights sum to TOTAL_WEIGHT, but keep the compiler happy.
        return RARITY_ORDER[RARITY_ORDER.length - 1];
    }

    public static int weightFor(Rarity rarity) {
        for (int i = 0; i < RARITY_ORDER.length; i++) {
            if (RARITY_ORDER[i] == rarity) {
                return WEIGHTS[i];
            }
        }
        throw new IllegalArgumentException("Unknown rarity " + rarity);
    }

    /** One gacha gem roll: random rarity per the weighted table, uniformly random type. */
    public Unit roll() {
        Rarity rarity = rollRarity(rng);
        UnitType[] types = UnitType.values();
        UnitType type = types[rng.nextInt(types.length)];
        return Unit.rolled(type, rarity);
    }

    public List<Unit> rollMany(int count) {
        List<Unit> results = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            results.add(roll());
        }
        return results;
    }

    /**
     * Merges every eligible group of {@value #MERGE_GROUP_SIZE} identical
     * (type + rarity) units in the roster into one unit of the next rarity
     * up, repeating until no more merges are possible (a merge can itself
     * create a fresh triple at the next tier). Returns a new roster list;
     * the input list is left untouched.
     */
    public List<Unit> mergeAll(List<Unit> roster) {
        List<Unit> working = new ArrayList<>(roster);
        boolean mergedSomething = true;
        while (mergedSomething) {
            mergedSomething = false;
            Map<String, List<Unit>> groups = groupByTypeAndRarity(working);
            for (List<Unit> group : groups.values()) {
                if (group.size() >= MERGE_GROUP_SIZE) {
                    Unit sample = group.get(0);
                    if (!sample.getRarity().hasNext()) {
                        continue; // LEGENDARY is the ceiling; excess copies just stay in the roster
                    }
                    List<Unit> consumed = group.subList(0, MERGE_GROUP_SIZE);
                    working.removeAll(consumed);
                    working.add(Unit.rolled(sample.getType(), sample.getRarity().next()));
                    mergedSomething = true;
                    break; // restart grouping after every merge - counts/keys shifted
                }
            }
        }
        return working;
    }

    private static Map<String, List<Unit>> groupByTypeAndRarity(List<Unit> units) {
        Map<String, List<Unit>> groups = new HashMap<>();
        for (Unit unit : units) {
            String key = unit.getType() + ":" + unit.getRarity();
            groups.computeIfAbsent(key, k -> new ArrayList<>()).add(unit);
        }
        return groups;
    }
}
