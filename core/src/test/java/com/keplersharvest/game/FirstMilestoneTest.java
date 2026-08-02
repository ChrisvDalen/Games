package com.keplersharvest.game;

import com.keplersharvest.configuration.GameContent;
import com.keplersharvest.farming.FarmLand;
import com.keplersharvest.save.SaveGameService;
import com.keplersharvest.testing.TestContent;
import com.keplersharvest.testing.TestPlayer;
import com.keplersharvest.world.GridPoint;
import com.keplersharvest.world.MapObject;
import com.keplersharvest.world.MapObjectKind;
import com.keplersharvest.world.WorldMap;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Drives the whole first-milestone loop through the real systems, with no window open.
 *
 * <p>This is the test that says the game is playable: gather, till, plant, water, sleep, harvest,
 * repair, talk, read a log, save and reload.
 */
class FirstMilestoneTest {

    private static final GridPoint BED = new GridPoint(8, 8);

    @Test
    @DisplayName("the full farming and repair loop can be completed and saved")
    void playsTheFirstMilestone(@TempDir Path saveDirectory) {
        // 1-2. Start a new game.
        GameSession session = TestContent.newSession();
        GameContent content = session.content();
        PlayerActions actions = new PlayerActions(session);

        assertEquals("colony", session.player().mapId());
        assertTrue(session.questLog().isActive("arrival"), "the opening task starts by itself");

        // 3-4. Walk the colony and cut salvage free.
        gatherEverythingOn(session, actions, "colony");
        assertTrue(session.inventory().count("scrap_alloy") >= 4,
                "the colony wrecks should yield enough plate, got " + session.inventory().count("scrap_alloy"));

        // 5-7. Out to the terrace: break a bed, plant, water.
        session.travelTo("terrace", "from_colony");
        gatherEverythingOn(session, actions, "terrace");

        assertTrue(TestPlayer.equip(session, "soil_blade"));
        TestPlayer.faceFromAnySide(session, "terrace", BED);
        assertInstanceOf(InteractionResult.Handled.class, actions.useTool());
        FarmLand terrace = session.farmLand("terrace");
        assertEquals(com.keplersharvest.farming.SoilState.TILLED,
                terrace.plotAt(BED).orElseThrow().soil());

        assertTrue(TestPlayer.equip(session, "lumen_pod_seed"));
        int seedsBefore = session.inventory().count("lumen_pod_seed");
        assertInstanceOf(InteractionResult.Handled.class, actions.useTool());
        assertEquals(seedsBefore - 1, session.inventory().count("lumen_pod_seed"), "planting spends a seed");
        assertEquals("lumen_pod", terrace.plotAt(BED).orElseThrow().cropId().orElseThrow());

        assertTrue(TestPlayer.equip(session, "mist_canister"));
        assertInstanceOf(InteractionResult.Handled.class, actions.useTool());
        assertTrue(terrace.plotAt(BED).orElseThrow().watered());

        // 8. Sleep through the crop's growth, watering each morning and topping up materials.
        int daysToMaturity = content.crop("lumen_pod").orElseThrow().daysToMaturity();
        int startingDay = session.clock().day();
        for (int day = 0; day < daysToMaturity; day++) {
            session.sleep();
            gatherEverythingOn(session, actions, "terrace");
            TestPlayer.faceFromAnySide(session, "terrace", BED);
            TestPlayer.equip(session, "mist_canister");
            actions.useTool();
        }
        assertEquals(startingDay + daysToMaturity, session.clock().day(), "each sleep advances one day");

        // 9. Harvest.
        TestPlayer.faceFromAnySide(session, "terrace", BED);
        assertTrue(terrace.readyToHarvest(BED), "three watered days should ripen a Lumen Pod");
        assertInstanceOf(InteractionResult.Handled.class, actions.interact());
        assertTrue(session.inventory().count("lumen_pod") >= 1, "the harvest reaches the pack");
        assertTrue(session.questLog().isCompleted("breaking_ground"), "the farming task should close");

        // 10. Repair the oxygen recycler, one stage at a time.
        session.travelTo("colony", "from_terrace");
        gatherEverythingOn(session, actions, "colony");
        MapObject recycler = moduleObject(session, "oxygen_recycler");
        int stages = content.module("oxygen_recycler").orElseThrow().stageCount();
        for (int stage = 0; stage < stages; stage++) {
            TestPlayer.faceFromAnySide(session, "colony", recycler.position());
            actions.interact();
        }
        assertTrue(session.colony().isOnline("oxygen_recycler"),
                "the recycler should be running; still missing " + session.latestNotice().orElse(""));
        assertTrue(session.player().energy().max() > content.settings().baseMaxEnergy(),
                "a working recycler raises the energy ceiling");

        // 11. Talk to a colonist.
        session.travelTo("terrace", "from_colony");
        var sefa = session.colonist("sefa").orElseThrow();
        sefa.applySchedule(session.clock().phase(), session.colony()::isOnline);
        session.player().moveTo(sefa.mapId(), com.keplersharvest.world.WorldPosition.centreOf(
                com.keplersharvest.world.Movement.nearestFreeTile(
                        content.maps().require(sefa.mapId()), sefa.position())));
        TestPlayer.faceFromAnySide(session, sefa.mapId(), sefa.position());
        InteractionResult conversation = actions.interact();
        assertInstanceOf(InteractionResult.StartDialogue.class, conversation);
        assertTrue(session.questLog().isCompleted("arrival"), "meeting Sefa closes the opening task");

        // 12. Find the first crew log.
        session.travelTo("colony", "from_terrace");
        MapObject slate = firstOfKind(content.maps().require("colony"), MapObjectKind.CREW_LOG);
        TestPlayer.faceFromAnySide(session, "colony", slate.position());
        InteractionResult reading = actions.interact();
        var read = assertInstanceOf(InteractionResult.ReadCrewLog.class, reading);
        assertEquals(slate.requireProperty("log"), read.log().id());
        assertEquals(1, session.journal().foundLogCount());
        assertTrue(session.journal().hasEvidence("weave_makes_the_air"), "the log files its own clue");

        // 13. Save and reload; everything above must survive.
        SaveGameService saves = new SaveGameService(saveDirectory);
        assertTrue(saves.save(session).succeeded());

        GameSession reloaded = assertInstanceOf(SaveGameService.LoadOutcome.Loaded.class,
                saves.load(content)).session();

        assertEquals(session.clock().day(), reloaded.clock().day());
        assertTrue(reloaded.colony().isOnline("oxygen_recycler"));
        assertTrue(reloaded.journal().hasLog(slate.requireProperty("log")));
        assertTrue(reloaded.questLog().isCompleted("arrival"));
        assertTrue(reloaded.questLog().isCompleted("breaking_ground"));
        assertTrue(reloaded.relationshipPoints("sefa") > 0);
        assertEquals(session.inventory().count("lumen_pod"), reloaded.inventory().count("lumen_pod"));
    }

    @Test
    @DisplayName("a resource node runs dry and comes back after its respawn window")
    void resourceNodesRespawn() {
        GameSession session = TestContent.newSession();
        PlayerActions actions = new PlayerActions(session);
        WorldMap colony = session.content().maps().require("colony");
        MapObject node = firstOfKind(colony, MapObjectKind.RESOURCE_NODE);
        int respawnDays = node.intProperty("respawnDays", 0);
        assertTrue(respawnDays > 0, "this test needs a respawning node");

        TestPlayer.faceFromAnySide(session, "colony", node.position());
        assertInstanceOf(InteractionResult.Handled.class, actions.gather(node.position()));
        assertInstanceOf(InteractionResult.Nothing.class, actions.gather(node.position()));

        for (int day = 0; day < respawnDays - 1; day++) {
            session.sleep();
        }
        assertInstanceOf(InteractionResult.Nothing.class, actions.gather(node.position()));

        session.sleep();
        TestPlayer.faceFromAnySide(session, "colony", node.position());
        assertInstanceOf(InteractionResult.Handled.class, actions.gather(node.position()));
    }

    @Test
    @DisplayName("work costs energy and stops when the player is spent")
    void energyLimitsWork() {
        GameSession session = TestContent.newSession();
        PlayerActions actions = new PlayerActions(session);
        session.travelTo("terrace", "from_colony");
        TestPlayer.equip(session, "soil_blade");

        int before = session.player().energy().current();
        TestPlayer.faceFromAnySide(session, "terrace", BED);
        actions.useTool();
        int cost = session.settings().energyCost("till");
        assertEquals(before - cost, session.player().energy().current());

        session.player().energy().set(1);
        assertInstanceOf(InteractionResult.Nothing.class,
                actions.useTool(), "too tired to break more ground");

        session.sleep();
        assertEquals(session.player().energy().max(), session.player().energy().current(),
                "sleeping restores the meter");
    }

    /** Cuts free every available node on a map, walking up to each in turn. */
    private void gatherEverythingOn(GameSession session, PlayerActions actions, String mapId) {
        WorldMap map = session.content().maps().require(mapId);
        session.player().moveTo(mapId, session.player().position());
        for (MapObject node : map.objectsOfKind(MapObjectKind.RESOURCE_NODE)) {
            if (!node.property("tool", "").isBlank()) {
                TestPlayer.equip(session, "cutting_torch");
            }
            if (TestPlayer.faceFromAnySide(session, mapId, node.position())) {
                actions.gather(node.position());
            }
        }
    }

    private MapObject moduleObject(GameSession session, String moduleId) {
        return session.content().maps().require("colony").objectsOfKind(MapObjectKind.MODULE).stream()
                .filter(object -> object.property("module", "").equals(moduleId))
                .findFirst()
                .orElseThrow();
    }

    private MapObject firstOfKind(WorldMap map, MapObjectKind kind) {
        return map.objectsOfKind(kind).stream().findFirst().orElseThrow();
    }
}
