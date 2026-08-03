package com.keplersharvest.game;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorldRandomTest {

    private static List<Integer> roll(WorldRandom random, int times) {
        List<Integer> values = new ArrayList<>();
        for (int i = 0; i < times; i++) {
            values.add(random.nextInt(100));
        }
        return values;
    }

    @Test
    @DisplayName("the same seed replays the same sequence")
    void sameSeedSameSequence() {
        assertEquals(roll(new WorldRandom(4242L), 20), roll(new WorldRandom(4242L), 20));
    }

    @Test
    @DisplayName("different seeds diverge")
    void differentSeedsDiverge() {
        assertNotEquals(roll(new WorldRandom(1L), 20), roll(new WorldRandom(2L), 20));
    }

    @Test
    @DisplayName("restoring the captured state resumes the stream where it left off")
    void restoreResumesTheStream() {
        WorldRandom original = new WorldRandom(99L);
        roll(original, 13);
        long captured = original.state();
        List<Integer> continuation = roll(original, 10);

        WorldRandom resumed = new WorldRandom(0L);
        resumed.restore(captured);

        assertEquals(continuation, roll(resumed, 10),
                "a generator restored mid-stream must not rewind to the seed");
    }

    @Test
    @DisplayName("the state advances with every draw")
    void stateAdvances() {
        WorldRandom random = new WorldRandom(7L);
        long before = random.state();
        random.nextInt(10);

        assertNotEquals(before, random.state());
    }

    @Test
    @DisplayName("bounded draws stay inside their bound")
    void boundedDrawsStayInRange() {
        WorldRandom random = new WorldRandom(-1L);
        for (int i = 0; i < 5_000; i++) {
            int value = random.nextInt(6);
            assertTrue(value >= 0 && value < 6, "out of range: " + value);
        }
    }
}
