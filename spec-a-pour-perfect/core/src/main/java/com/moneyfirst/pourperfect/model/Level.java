package com.moneyfirst.pourperfect.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A single puzzle: a fixed list of {@link Tube}s. Levels are produced by {@link LevelGenerator}
 * and are always solvable at the moment they're generated - a player can still reach a dead end
 * through their own play, which is why {@link #isWon()} is checked after every move rather than
 * assumed.
 */
public final class Level {

    private final long seed;
    private final int levelIndex;
    private final List<Tube> tubes;

    public Level(long seed, int levelIndex, List<Tube> tubes) {
        this.seed = seed;
        this.levelIndex = levelIndex;
        this.tubes = new ArrayList<>(tubes);
    }

    public long getSeed() {
        return seed;
    }

    public int getLevelIndex() {
        return levelIndex;
    }

    public List<Tube> getTubes() {
        return Collections.unmodifiableList(tubes);
    }

    public int tubeCount() {
        return tubes.size();
    }

    public Tube getTube(int index) {
        return tubes.get(index);
    }

    /** Win condition: every tube is either empty or a completed single color. */
    public boolean isWon() {
        for (Tube tube : tubes) {
            if (!tube.isSolvedOrEmpty()) {
                return false;
            }
        }
        return true;
    }

    /** Adds a fresh empty tube to this level - backs the "extra tube" rewarded-video perk. */
    public void addTube(Tube tube) {
        tubes.add(tube);
    }

    /**
     * Replaces this level's tubes wholesale with deep copies of {@code snapshot} - backs
     * {@code GameSession}'s undo/undo-all. Deliberately tolerant of a different tube count (the
     * "extra tube" perk can grow the tube list mid-session; undoing past that point simply
     * restores whatever tube count the snapshot had).
     */
    public void restoreFrom(List<Tube> snapshot) {
        tubes.clear();
        for (Tube tube : snapshot) {
            tubes.add(tube.copy());
        }
    }

    /** Deep copy of the whole level - used for undo snapshots and solver state exploration. */
    public Level copy() {
        List<Tube> copiedTubes = new ArrayList<>(tubes.size());
        for (Tube tube : tubes) {
            copiedTubes.add(tube.copy());
        }
        return new Level(seed, levelIndex, copiedTubes);
    }
}
