package com.keplersharvest.game;

import com.keplersharvest.configuration.GameSettings;
import com.keplersharvest.testing.TestContent;
import com.keplersharvest.time.DayPhase;
import com.keplersharvest.world.GridPoint;
import com.keplersharvest.world.MapObject;
import com.keplersharvest.world.MapObjectKind;
import com.keplersharvest.world.WorldMap;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The night hazard is decided solely by the objects on the map the player is standing on.
 *
 * <p>These exist because the check used to consider only landmarks, and then fall back to "the
 * first map anywhere that declares a hazard". That happened to be right for the one hazardous map
 * that ships, and would have gone quietly wrong the moment a second one was added or a hazard was
 * put on something other than a landmark.
 */
class NightHazardTest {

    /** Minutes from waking up to the first night hour, which is the first that drains energy. */
    private static int minutesToFirstNightHour(GameSession session) {
        return DayPhase.NIGHT.startMinute() - session.settings().wakeMinute();
    }

    private static int energyLostOvernightOn(String mapId, String spawn) {
        GameSession session = TestContent.newSession();
        if (!mapId.equals(session.player().mapId())) {
            session.travelTo(mapId, spawn);
        }
        session.player().energy().fill();
        int before = session.player().energy().current();

        session.clock().advanceMinutes(minutesToFirstNightHour(session));

        return before - session.player().energy().current();
    }

    @Test
    @DisplayName("exactly one shipped map declares a night hazard")
    void oneMapIsHazardous() {
        List<String> hazardous = TestContent.load().maps().maps().stream()
                .filter(map -> map.objects().stream().anyMatch(NightHazardTest::declaresHazard))
                .map(WorldMap::id)
                .toList();

        assertEquals(List.of("biome"), hazardous,
                "if this changes, the per-map hazard checks below need revisiting");
    }

    @Test
    @DisplayName("a safe map costs only the ordinary night drain")
    void safeMapDrainsNormally() {
        GameSettings settings = TestContent.load().settings();

        assertEquals(settings.nightEnergyDrainPerHour(), energyLostOvernightOn("colony", "landing_pad"));
    }

    @Test
    @DisplayName("standing in the hazardous biome after dark costs extra")
    void hazardousMapDrainsMore() {
        GameSettings settings = TestContent.load().settings();
        int expected = settings.nightEnergyDrainPerHour() + settings.hazardEnergyDrainPerHour();

        assertEquals(expected, energyLostOvernightOn("biome", "from_terrace"));
        assertTrue(settings.hazardEnergyDrainPerHour() > 0, "the hazard should actually cost something");
    }

    @Test
    @DisplayName("a hazard counts whatever kind of object carries it")
    void hazardIsNotLandmarkOnly() {
        GridPoint anywhere = new GridPoint(1, 1);

        assertTrue(GameSession.declaresNightHazard(List.of(
                new MapObject("marker", MapObjectKind.LANDMARK, anywhere, Map.of("nightHazard", "true")))));
        assertTrue(GameSession.declaresNightHazard(List.of(
                new MapObject("node", MapObjectKind.RESOURCE_NODE, anywhere, Map.of("nightHazard", "true")))),
                "a hazard on a non-landmark object must count too");
    }

    @Test
    @DisplayName("a map with no hazard object is safe")
    void mapsWithoutHazardsAreSafe() {
        GridPoint anywhere = new GridPoint(1, 1);

        assertFalse(GameSession.declaresNightHazard(List.of()));
        assertFalse(GameSession.declaresNightHazard(List.of(
                new MapObject("marker", MapObjectKind.LANDMARK, anywhere, Map.of("title", "Somewhere")))));
        assertFalse(GameSession.declaresNightHazard(List.of(
                new MapObject("marker", MapObjectKind.LANDMARK, anywhere, Map.of("nightHazard", "false")))));
    }

    private static boolean declaresHazard(MapObject object) {
        return object.boolProperty("nightHazard", false);
    }
}
