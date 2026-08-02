package com.keplersharvest.npc;

import com.keplersharvest.testing.TestContent;
import com.keplersharvest.time.DayPhase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RelationshipTest {

    @Test
    @DisplayName("levels follow point thresholds")
    void levelsFollowPoints() {
        assertEquals(RelationshipLevel.STRANGER, RelationshipLevel.forPoints(0));
        assertEquals(RelationshipLevel.STRANGER, RelationshipLevel.forPoints(19));
        assertEquals(RelationshipLevel.ACQUAINTANCE, RelationshipLevel.forPoints(20));
        assertEquals(RelationshipLevel.COLLEAGUE, RelationshipLevel.forPoints(45));
        assertEquals(RelationshipLevel.FRIEND, RelationshipLevel.forPoints(75));
        assertEquals(RelationshipLevel.CONFIDANT, RelationshipLevel.forPoints(110));
    }

    @Test
    @DisplayName("crossing a threshold is reported once")
    void promotionIsReportedOnce() {
        RelationshipBook book = new RelationshipBook();

        assertEquals(Optional.empty(), book.add("sefa", 10), "still a stranger");
        assertEquals(Optional.of(RelationshipLevel.ACQUAINTANCE), book.add("sefa", 10));
        assertEquals(Optional.empty(), book.add("sefa", 5), "no new tier crossed");
        assertEquals(Optional.of(RelationshipLevel.COLLEAGUE), book.add("sefa", 25));
    }

    @Test
    @DisplayName("points are clamped at both ends")
    void pointsAreClamped() {
        RelationshipBook book = new RelationshipBook();

        book.add("rike", -50);
        assertEquals(0, book.points("rike"), "relationships do not go negative");

        book.add("rike", 10_000);
        assertEquals(RelationshipLevel.MAX_POINTS, book.points("rike"));
        assertEquals(RelationshipLevel.CONFIDANT, book.level("rike"));
        assertEquals(0, RelationshipLevel.pointsToNext(RelationshipLevel.MAX_POINTS));
    }

    @Test
    @DisplayName("an unknown colonist reads as a stranger rather than failing")
    void unknownColonistIsAStranger() {
        RelationshipBook book = new RelationshipBook();

        assertEquals(0, book.points("nobody"));
        assertEquals(RelationshipLevel.STRANGER, book.level("nobody"));
    }

    @Test
    @DisplayName("relationship state survives a snapshot and restore")
    void snapshotRoundTrips() {
        RelationshipBook book = new RelationshipBook();
        book.add("sefa", 40);
        book.add("nnenna", 80);

        RelationshipBook restored = new RelationshipBook();
        restored.restore(book.snapshot());

        assertEquals(40, restored.points("sefa"));
        assertEquals(80, restored.points("nnenna"));
    }

    @Test
    @DisplayName("colonists stand somewhere different in each phase of the day")
    void schedulesMoveColonists() {
        ColonistDefinition sefa = TestContent.load().colonist("sefa").orElseThrow();

        ScheduleEntry morning = sefa.locationDuring(DayPhase.MORNING, module -> false);
        ScheduleEntry evening = sefa.locationDuring(DayPhase.EVENING, module -> false);

        assertNotEquals(morning.position(), evening.position());
        assertTrue(TestContent.load().maps().find(morning.mapId()).isPresent());
    }

    @Test
    @DisplayName("a repaired module moves a colonist to a new post")
    void repairsChangeSchedules() {
        ColonistDefinition nnenna = TestContent.load().colonist("nnenna").orElseThrow();

        ScheduleEntry before = nnenna.locationDuring(DayPhase.MORNING, module -> false);
        ScheduleEntry after = nnenna.locationDuring(DayPhase.MORNING, "oxygen_recycler"::equals);

        assertNotEquals(before.position(), after.position(),
                "Nnenna should go and look at the recycler once it runs");
    }

    @Test
    @DisplayName("a colonist always has somewhere to be")
    void scheduleAlwaysResolves() {
        for (ColonistDefinition colonist : TestContent.load().colonists().values()) {
            for (DayPhase phase : DayPhase.values()) {
                ScheduleEntry entry = colonist.locationDuring(phase, module -> false);
                assertTrue(TestContent.load().maps().find(entry.mapId()).isPresent(),
                        colonist.id() + " is scheduled onto a map that does not exist");
            }
        }
    }
}
