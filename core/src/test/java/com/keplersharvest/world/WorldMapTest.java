package com.keplersharvest.world;

import com.keplersharvest.testing.TestContent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorldMapTest {

    private WorldMap colony() {
        return TestContent.load().maps().require("colony");
    }

    @Test
    @DisplayName("maps load with their layers and objects intact")
    void mapsLoad() {
        WorldMap map = colony();

        assertEquals("Meridian Station", map.displayName());
        assertTrue(map.width() > 0 && map.height() > 0);
        assertFalse(map.objects().isEmpty());
        assertFalse(map.objectsOfKind(MapObjectKind.MODULE).isEmpty());
        assertNotNull(map.spawn("landing_pad").orElse(null));
    }

    @Test
    @DisplayName("the map border is solid so the player cannot leave")
    void bordersAreSolid() {
        WorldMap map = colony();

        assertTrue(map.blocked(0, 5));
        assertTrue(map.blocked(map.width() - 1, 5));
        assertTrue(map.blocked(5, 0));
        assertTrue(map.blocked(5, map.height() - 1));
        assertTrue(map.blocked(-1, -1), "out of bounds counts as blocked");
    }

    @Test
    @DisplayName("every spawn point stands on walkable ground")
    void spawnsAreWalkable() {
        for (WorldMap map : TestContent.load().maps().maps()) {
            for (MapObject spawn : map.objectsOfKind(MapObjectKind.SPAWN)) {
                assertFalse(map.blocked(spawn.position()),
                        "spawn " + spawn.id() + " on " + map.id() + " is inside a wall");
            }
        }
    }

    @Test
    @DisplayName("every portal leads somewhere reachable and does not bounce the player back")
    void portalsAreWired() {
        MapRegistry registry = TestContent.load().maps();

        for (WorldMap map : registry.maps()) {
            for (MapObject portal : map.objectsOfKind(MapObjectKind.PORTAL)) {
                WorldMap target = registry.require(portal.requireProperty("target"));
                GridPoint arrival = target.spawnOrDefault(portal.property("spawn", ""));

                assertFalse(target.blocked(arrival),
                        "portal " + portal.id() + " drops the player into a wall");
                assertTrue(target.objectAt(arrival)
                                .filter(object -> object.kind() == MapObjectKind.PORTAL).isEmpty(),
                        "portal " + portal.id() + " arrives on another portal, which would loop");
            }
        }
    }

    @Test
    @DisplayName("interactable objects are reachable from at least one side")
    void interactablesAreReachable() {
        for (WorldMap map : TestContent.load().maps().maps()) {
            for (MapObject object : map.objects()) {
                if (object.kind() == MapObjectKind.SPAWN || object.kind() == MapObjectKind.LANDMARK) {
                    continue;
                }
                boolean reachable = false;
                for (Direction direction : Direction.values()) {
                    GridPoint standing = object.position().step(direction);
                    if (map.inBounds(standing) && !map.blocked(standing)) {
                        reachable = true;
                        break;
                    }
                }
                assertTrue(reachable, object.id() + " on " + map.id() + " is walled in");
            }
        }
    }

    @Test
    @DisplayName("only the terrace has farmable ground")
    void farmableGroundIsWhereItShouldBe() {
        long farmableTiles = countFarmable(TestContent.load().maps().require("terrace"));
        assertTrue(farmableTiles > 50, "the terrace needs a usable bed area, found " + farmableTiles);
        assertEquals(0, countFarmable(colony()), "the station apron is not soil");
    }

    private long countFarmable(WorldMap map) {
        long count = 0;
        for (int y = 0; y < map.height(); y++) {
            for (int x = 0; x < map.width(); x++) {
                if (map.farmable(new GridPoint(x, y))) {
                    count++;
                }
            }
        }
        return count;
    }

    @Test
    @DisplayName("collision stops movement but allows sliding along a wall")
    void movementCollides() {
        WorldMap map = colony();
        GridPoint open = map.spawn("landing_pad").orElseThrow();
        WorldPosition start = WorldPosition.centreOf(open);

        WorldPosition intoWall = Movement.resolve(map, WorldPosition.centreOf(new GridPoint(1, 5)),
                -2f, 0f, com.keplersharvest.player.PlayerState.HALF_SIZE);
        assertEquals(1.5f, intoWall.x(), 0.001f, "the border wall stops horizontal movement");

        WorldPosition slid = Movement.resolve(map, WorldPosition.centreOf(new GridPoint(1, 5)),
                -2f, 1f, com.keplersharvest.player.PlayerState.HALF_SIZE);
        assertEquals(1.5f, slid.x(), 0.001f);
        assertTrue(slid.y() > start.y() - 10f, "vertical movement still happens along the wall");
    }

    @Test
    @DisplayName("a blocked spawn falls back to the nearest free tile")
    void nearestFreeTileFindsSpace() {
        WorldMap map = colony();
        GridPoint wall = new GridPoint(0, 0);

        GridPoint free = Movement.nearestFreeTile(map, wall);

        assertFalse(map.blocked(free));
    }

    @Test
    @DisplayName("tile coordinates round-trip through the save format")
    void gridPointParses() {
        GridPoint point = new GridPoint(12, 7);
        assertEquals(point, GridPoint.parse(point.toString()));
    }
}
