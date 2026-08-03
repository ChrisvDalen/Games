package com.keplersharvest.save;

import com.keplersharvest.BuildInfo;
import com.keplersharvest.configuration.GameContent;
import com.keplersharvest.game.GameSession;
import com.keplersharvest.testing.TestContent;
import com.keplersharvest.testing.TestPlayer;
import com.keplersharvest.world.Direction;
import com.keplersharvest.world.GridPoint;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SaveGameTest {

    /** Plays a short session so there is real state worth persisting. */
    private GameSession playedSession() {
        GameSession session = TestContent.newSession();
        GameContent content = session.content();

        session.inventory().add(content.requireItem("scrap_alloy"), 7);
        session.inventory().add(content.requireItem("resin_fibre"), 2);
        session.repair("oxygen_recycler");
        session.changeRelationship("sefa", 32);
        session.recoverCrewLog("log_intake");
        session.setFlag("met_sefa");
        session.discoverLandmark("south_terrace", "South Terrace");

        session.travelTo("terrace", "from_colony");
        GridPoint bed = new GridPoint(8, 8);
        TestPlayer.face(session, "terrace", bed, Direction.UP);
        session.farmLand("terrace").till(bed);
        session.farmLand("terrace").plant(bed, "glassroot");
        session.farmLand("terrace").water(bed);

        session.clock().advanceMinutes(5 * 60);
        session.player().energy().set(61);
        return session;
    }

    @Test
    @DisplayName("a played session survives a save and load unchanged")
    void roundTripsFullState(@TempDir Path directory) {
        SaveGameService service = new SaveGameService(directory);
        GameSession original = playedSession();

        assertTrue(service.save(original).succeeded());
        assertTrue(service.hasSave());

        SaveGameService.LoadOutcome outcome = service.load(original.content());
        GameSession restored = assertInstanceOf(SaveGameService.LoadOutcome.Loaded.class, outcome).session();

        assertEquals(original.clock().day(), restored.clock().day());
        assertEquals(original.clock().now().minuteOfDay(), restored.clock().now().minuteOfDay());
        assertEquals(original.player().mapId(), restored.player().mapId());
        assertEquals(original.player().position(), restored.player().position());
        assertEquals(original.player().facing(), restored.player().facing());
        assertEquals(original.player().energy().current(), restored.player().energy().current());

        assertEquals(original.inventory().count("scrap_alloy"), restored.inventory().count("scrap_alloy"));
        assertEquals(original.inventory().count("resin_fibre"), restored.inventory().count("resin_fibre"));

        assertEquals(original.colony().module("oxygen_recycler").orElseThrow().completedStages(),
                restored.colony().module("oxygen_recycler").orElseThrow().completedStages());
        assertEquals(32, restored.relationshipPoints("sefa"));
        assertTrue(restored.journal().hasLog("log_intake"));
        assertTrue(restored.journal().hasEvidence("weave_makes_the_air"));
        assertTrue(restored.flagSet("met_sefa"));
        assertTrue(restored.landmarkDiscovered("south_terrace"));

        var plot = restored.farmLand("terrace").plotAt(new GridPoint(8, 8)).orElseThrow();
        assertEquals("glassroot", plot.cropId().orElseThrow());
        assertTrue(plot.watered());
    }

    @Test
    @DisplayName("quest progress and status come back")
    void questsRoundTrip(@TempDir Path directory) {
        SaveGameService service = new SaveGameService(directory);
        GameSession original = TestContent.newSession();
        original.startQuest("arrival");
        original.publish(new com.keplersharvest.game.GameEvent.ColonistGreeted("sefa"));
        assertTrue(original.questCompleted("arrival"), "greeting Sefa finishes the opening task");

        service.save(original);
        GameSession restored = assertInstanceOf(SaveGameService.LoadOutcome.Loaded.class,
                service.load(original.content())).session();

        assertTrue(restored.questCompleted("arrival"));
    }

    @Test
    @DisplayName("crops keep growing across a save and load")
    void cropGrowthContinuesAfterLoad(@TempDir Path directory) {
        SaveGameService service = new SaveGameService(directory);
        GameSession original = TestContent.newSession();
        GridPoint bed = new GridPoint(8, 8);
        original.travelTo("terrace", "from_colony");
        original.farmLand("terrace").till(bed);
        original.farmLand("terrace").plant(bed, "lumen_pod");
        original.farmLand("terrace").water(bed);
        original.sleep();

        service.save(original);
        GameSession restored = assertInstanceOf(SaveGameService.LoadOutcome.Loaded.class,
                service.load(original.content())).session();

        assertEquals(1, restored.farmLand("terrace").plotAt(bed).orElseThrow().grownDays());

        restored.farmLand("terrace").water(bed);
        restored.sleep();
        restored.farmLand("terrace").water(bed);
        restored.sleep();

        assertTrue(restored.farmLand("terrace").readyToHarvest(bed));
    }

    @Test
    @DisplayName("the save file is human-readable JSON with a format version")
    void savesAreReadableJson(@TempDir Path directory) throws Exception {
        SaveGameService service = new SaveGameService(directory);
        service.save(TestContent.newSession());

        String text = Files.readString(service.slotFile());

        assertTrue(text.contains("\"version\""), "a save must record its format version");
        assertTrue(text.contains("\n"), "saves are pretty-printed so they can be inspected by hand");
        assertTrue(text.contains("\"mapId\""));
    }

    @Test
    @DisplayName("a missing save is reported rather than crashing")
    void missingSaveIsHandled(@TempDir Path directory) {
        SaveGameService service = new SaveGameService(directory.resolve("nothing-here"));

        assertFalse(service.hasSave());
        assertInstanceOf(SaveGameService.LoadOutcome.NoSave.class, service.load(TestContent.load()));
    }

    @Test
    @DisplayName("a corrupted save is reported rather than crashing")
    void corruptedSaveIsHandled(@TempDir Path directory) throws Exception {
        SaveGameService service = new SaveGameService(directory);
        Files.createDirectories(directory);
        Files.writeString(service.slotFile(), "{ this is not json at all ");

        SaveGameService.LoadOutcome outcome = service.load(TestContent.load());

        assertInstanceOf(SaveGameService.LoadOutcome.Unreadable.class, outcome);
    }

    @Test
    @DisplayName("a structurally impossible save is rejected by validation")
    void invalidSaveIsRejected(@TempDir Path directory) {
        SaveGameService service = new SaveGameService(directory);

        SaveGameService.LoadOutcome outcome = service.parse(TestContent.load(), """
                { "version": 1, "day": 0, "minuteOfDay": 5000, "mapId": "", "energy": -4 }
                """);

        var invalid = assertInstanceOf(SaveGameService.LoadOutcome.Invalid.class, outcome);
        assertEquals(4, invalid.problems().size(), "day, time, map and energy are all wrong");
    }

    @Test
    @DisplayName("a save from a newer format is refused with an explanation")
    void futureFormatIsRefused() {
        SaveGameService service = new SaveGameService(Path.of("unused"));

        SaveGameService.LoadOutcome outcome = service.parse(TestContent.load(), """
                { "version": 999, "day": 1, "minuteOfDay": 360, "mapId": "colony", "energy": 100 }
                """);

        var invalid = assertInstanceOf(SaveGameService.LoadOutcome.Invalid.class, outcome);
        assertTrue(invalid.problems().get(0).contains("newer version"));
    }

    @Test
    @DisplayName("an unversioned save is migrated up to the current format")
    void unversionedSaveIsMigrated() {
        SaveGameService service = new SaveGameService(Path.of("unused"));

        SaveGameService.LoadOutcome outcome = service.parse(TestContent.load(), """
                { "day": 3, "minuteOfDay": 480, "mapId": "colony", "playerX": 5.5, "playerY": 5.5,
                  "facing": "DOWN", "energy": 80 }
                """);

        var loaded = assertInstanceOf(SaveGameService.LoadOutcome.Loaded.class, outcome);
        assertFalse(loaded.migrationsApplied().isEmpty(), "the migration should be recorded");
        assertEquals(3, loaded.session().clock().day());
    }

    @Test
    @DisplayName("reloading resumes the random stream instead of rewinding it")
    void randomStreamSurvivesTheRoundTrip(@TempDir Path directory) {
        SaveGameService service = new SaveGameService(directory);
        GameSession original = TestContent.newSession();
        for (int i = 0; i < 9; i++) {
            original.random().nextInt(1000);
        }

        assertTrue(service.save(original).succeeded());
        int[] afterSaving = {original.random().nextInt(1000), original.random().nextInt(1000)};

        GameSession restored = assertInstanceOf(SaveGameService.LoadOutcome.Loaded.class,
                service.load(original.content())).session();

        assertEquals(afterSaving[0], restored.random().nextInt(1000));
        assertEquals(afterSaving[1], restored.random().nextInt(1000));
    }

    @Test
    @DisplayName("a format-1 save starts its random stream from the recorded seed")
    void formatOneSaveMigratesTheRandomState() {
        SaveGameService service = new SaveGameService(Path.of("unused"));

        SaveGameService.LoadOutcome outcome = service.parse(TestContent.load(), """
                { "version": 1, "seed": 12345, "day": 2, "minuteOfDay": 400, "mapId": "colony",
                  "playerX": 5.5, "playerY": 5.5, "facing": "UP", "energy": 50 }
                """);

        var loaded = assertInstanceOf(SaveGameService.LoadOutcome.Loaded.class, outcome);
        assertFalse(loaded.migrationsApplied().isEmpty(), "the migration should be recorded");
        assertEquals(12345L, loaded.session().randomState(),
                "an old save has no stream position, so it resumes from its seed");
    }

    @Test
    @DisplayName("the save stamps the version the build was produced from")
    void saveStampsTheBuildVersion() {
        SaveData data = SaveMapper.capture(TestContent.newSession());

        assertEquals(BuildInfo.VERSION, data.gameVersion);
        assertEquals(System.getProperty("keplersharvest.expectedVersion"), data.gameVersion,
                "BuildInfo.VERSION has drifted from the version Gradle is building");
    }

    @Test
    @DisplayName("content that no longer exists is dropped instead of failing the load")
    void unknownContentIsDropped() {
        SaveGameService service = new SaveGameService(Path.of("unused"));

        SaveGameService.LoadOutcome outcome = service.parse(TestContent.load(), """
                { "version": 1, "day": 2, "minuteOfDay": 400, "mapId": "colony",
                  "playerX": 5.5, "playerY": 5.5, "facing": "UP", "energy": 50,
                  "inventory": [ { "slot": 0, "item": "scrap_alloy", "count": 3 },
                                 { "slot": 1, "item": "removed_item", "count": 9 } ],
                  "quests": [ { "id": "deleted_quest", "status": "ACTIVE", "counters": [1] } ],
                  "modules": [ { "id": "deleted_module", "completedStages": 4 } ] }
                """);

        GameSession session = assertInstanceOf(SaveGameService.LoadOutcome.Loaded.class, outcome).session();
        assertEquals(3, session.inventory().count("scrap_alloy"));
        assertEquals(0, session.inventory().count("removed_item"));
    }

    @Test
    @DisplayName("validation catches nonsense inventory entries")
    void validationChecksInventoryEntries() {
        SaveData data = new SaveData();
        data.day = 1;
        data.minuteOfDay = 360;
        data.mapId = "colony";
        data.inventory.add(new SaveData.SlotEntry(-1, "", 0));

        var problems = SaveValidation.problems(data);

        assertEquals(3, problems.size(), "negative slot, zero count and blank id");
        assertFalse(SaveValidation.isValid(data));
    }

    @Test
    @DisplayName("deleting the slot removes the file")
    void deleteRemovesTheSlot(@TempDir Path directory) {
        SaveGameService service = new SaveGameService(directory);
        service.save(TestContent.newSession());
        assertTrue(service.hasSave());

        service.delete();

        assertFalse(service.hasSave());
    }
}
