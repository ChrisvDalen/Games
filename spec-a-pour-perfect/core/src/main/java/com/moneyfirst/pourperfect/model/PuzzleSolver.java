package com.moneyfirst.pourperfect.model;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Breadth-first search over the tube-pouring state graph. Used two ways:
 * <ul>
 *   <li>by {@link LevelGenerator}, to verify a freshly scrambled puzzle is actually solvable
 *       before handing it to a player;</li>
 *   <li>by {@code GameSession}'s hint feature, to find (and reveal) one correct next move from
 *       whatever state the player is currently in.</li>
 * </ul>
 * BFS guarantees the shortest solution is found first, and - because states are deduplicated via
 * a visited set - never revisits the same configuration twice. Search is bounded by
 * {@code maxStatesExplored} so a pathological puzzle can't hang the caller; hitting the bound is
 * reported as "no solution found" (not proof of unsolvability), which is exactly the conservative
 * behaviour both callers want.
 */
public final class PuzzleSolver {

    private PuzzleSolver() {
    }

    /** Is this exact list of tubes (in this exact order) solvable via legal pours? */
    public static boolean isSolvable(List<Tube> tubes, int maxStatesExplored) {
        return solve(tubes, maxStatesExplored).isPresent();
    }

    /**
     * Finds a shortest sequence of legal pours that wins from the given tube state.
     *
     * @return the move sequence if one was found within the search bound; empty if the search
     *         bound was exhausted first (or the state has no legal moves at all)
     */
    public static Optional<List<Move>> solve(List<Tube> tubes, int maxStatesExplored) {
        List<Tube> start = copyOf(tubes);
        if (isWon(start)) {
            return Optional.of(List.of());
        }

        Map<String, Node> visited = new HashMap<>();
        String startKey = keyOf(start);
        visited.put(startKey, new Node(null, null));

        ArrayDeque<List<Tube>> frontier = new ArrayDeque<>();
        frontier.add(start);

        int statesExplored = 1;

        while (!frontier.isEmpty()) {
            if (statesExplored > maxStatesExplored) {
                return Optional.empty();
            }
            List<Tube> current = frontier.poll();
            String currentKey = keyOf(current);

            int tubeCount = current.size();
            for (int from = 0; from < tubeCount; from++) {
                Tube source = current.get(from);
                if (source.isEmpty() || source.isComplete()) {
                    continue;
                }
                for (int to = 0; to < tubeCount; to++) {
                    if (from == to) {
                        continue;
                    }
                    Tube target = current.get(to);
                    if (!source.canPourInto(target)) {
                        continue;
                    }

                    List<Tube> next = copyOf(current);
                    next.get(from).pourInto(next.get(to));
                    String nextKey = keyOf(next);

                    if (visited.containsKey(nextKey)) {
                        continue;
                    }
                    visited.put(nextKey, new Node(currentKey, new Move(from, to)));
                    statesExplored++;

                    if (isWon(next)) {
                        return Optional.of(reconstructPath(visited, nextKey));
                    }
                    frontier.add(next);

                    if (statesExplored > maxStatesExplored) {
                        return Optional.empty();
                    }
                }
            }
        }
        return Optional.empty();
    }

    private static List<Move> reconstructPath(Map<String, Node> visited, String goalKey) {
        List<Move> path = new ArrayList<>();
        String key = goalKey;
        while (key != null) {
            Node node = visited.get(key);
            if (node.move == null) {
                break;
            }
            path.add(node.move);
            key = node.parentKey;
        }
        Collections.reverse(path);
        return path;
    }

    private static boolean isWon(List<Tube> tubes) {
        for (Tube tube : tubes) {
            if (!tube.isSolvedOrEmpty()) {
                return false;
            }
        }
        return true;
    }

    private static List<Tube> copyOf(List<Tube> tubes) {
        List<Tube> copy = new ArrayList<>(tubes.size());
        for (Tube tube : tubes) {
            copy.add(tube.copy());
        }
        return copy;
    }

    /** Stable string key for a tube-state, used for the BFS visited-set. */
    private static String keyOf(List<Tube> tubes) {
        StringBuilder sb = new StringBuilder();
        for (Tube tube : tubes) {
            for (LiquidColor c : tube.getContents()) {
                sb.append(c.ordinal()).append(',');
            }
            sb.append(';');
        }
        return sb.toString();
    }

    private static final class Node {
        final String parentKey;
        final Move move;

        Node(String parentKey, Move move) {
            this.parentKey = parentKey;
            this.move = move;
        }
    }
}
