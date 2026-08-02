package com.keplersharvest.configuration;

import com.keplersharvest.testing.TestContent;
import com.keplersharvest.world.MapObjectKind;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Guards the shipped content: it must parse, and every cross-reference must resolve. */
class ContentLoadingTest {

    @Test
    @DisplayName("all content files load and cross-validate")
    void loadsEverything() {
        GameContent content = TestContent.load();

        assertEquals(4, content.crops().size(), "the vertical slice ships four alien crops");
        assertEquals(3, content.modules().size(), "three repairable modules");
        assertEquals(3, content.colonists().size(), "three colonists");
        assertEquals(5, content.crewLogs().size(), "five discoverable crew logs");
        assertEquals(3, content.maps().maps().size(), "colony, terrace and biome");
        assertFalse(content.recipes().isEmpty());
        assertFalse(content.quests().isEmpty());
    }

    @Test
    @DisplayName("every crew log is actually placed in the world")
    void everyCrewLogIsReachable() {
        GameContent content = TestContent.load();
        long placed = content.maps().maps().stream()
                .flatMap(map -> map.objectsOfKind(MapObjectKind.CREW_LOG).stream())
                .map(object -> object.requireProperty("log"))
                .distinct()
                .count();
        assertEquals(content.crewLogs().size(), placed, "a log nobody can find is a soft-locked mystery");
    }

    @Test
    @DisplayName("the reveal is reachable from the logs alone")
    void revealIsReachable() {
        GameContent content = TestContent.load();
        var granted = content.crewLogs().values().stream()
                .flatMap(log -> log.grantsEvidence().stream())
                .toList();
        content.reveal().requiredEvidence().forEach(id ->
                assertTrue(granted.contains(id), "no crew log grants required evidence " + id));
    }

    @Test
    @DisplayName("the starting kit covers the first farming loop")
    void startingKitIsPlayable() {
        GameContent content = TestContent.load();
        var kit = content.settings().startingItems().stream()
                .map(GameSettings.StartingItem::itemId)
                .toList();
        assertTrue(kit.contains("soil_blade"), "cannot till without a tiller");
        assertTrue(kit.contains("mist_canister"), "cannot water without an irrigator");
        assertTrue(kit.stream().anyMatch(id -> content.requireItem(id).plantsCropId().isPresent()),
                "the player must start with something to plant");
    }
}
