package com.moneyfirst.towerperil.rescue;

import com.moneyfirst.towerperil.battle.UnitType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PhysicsResolverTest {

    @Test
    void pullingARowPinRemovesItFromTheGrid() {
        Grid grid = new Grid(5, 5);
        Pin pin = new Pin("p1", Orientation.ROW, 2, Direction.RIGHT, 4, 2);
        grid.place(pin);

        assertEquals(pin, grid.findPin(Orientation.ROW, 2));
        PhysicsResolver.applyPull(grid, pin);
        assertNull(grid.findPin(Orientation.ROW, 2));
        assertNull(grid.occupantAt(4, 2));
    }

    @Test
    void characterSlidesAllTheWayToExitAndIsRescued() {
        Grid grid = new Grid(5, 5);
        Pin pin = new Pin("p1", Orientation.ROW, 2, Direction.RIGHT, 4, 2);
        grid.place(pin);
        Character character = new Character("c1", UnitType.WARRIOR, 0, 2);
        grid.place(character);

        PullOutcome outcome = PhysicsResolver.applyPull(grid, pin);

        assertEquals(1, outcome.getRescued().size());
        assertEquals("c1", outcome.getRescued().get(0).getId());
        assertTrue(outcome.getLost().isEmpty());
        assertNull(grid.occupantAt(0, 2));
        assertTrue(grid.getCharacters().isEmpty());
    }

    @Test
    void characterCompactsAgainstAPinFromTheOtherOrientationAndIsNeitherRescuedNorLost() {
        Grid grid = new Grid(5, 5);
        // Row pin pulls right along row 0; a column pin also anchored at row 0 (its UP edge)
        // sits at (3,0), physically blocking the row-0 path partway to the exit.
        Pin rowPin = new Pin("rowPin", Orientation.ROW, 0, Direction.RIGHT, 4, 0);
        Pin columnPin = new Pin("colPin", Orientation.COLUMN, 3, Direction.UP, 3, 0);
        grid.place(rowPin);
        grid.place(columnPin);
        Character character = new Character("c1", UnitType.ARCHER, 0, 0);
        grid.place(character);

        PullOutcome outcome = PhysicsResolver.applyPull(grid, rowPin);

        assertTrue(outcome.getRescued().isEmpty());
        assertTrue(outcome.getLost().isEmpty());
        // Character should have compacted to column 2 (immediately left of the blocking column pin at col 3).
        assertEquals(2, character.getCol());
        assertEquals(0, character.getRow());
        assertEquals(character, grid.occupantAt(2, 0));
        // The blocking column pin itself is untouched by this pull.
        assertEquals(columnPin, grid.findPin(Orientation.COLUMN, 3));
    }

    @Test
    void hazardNearerTheExitCatchesACharacterBehindIt() {
        Grid grid = new Grid(6, 5);
        Pin pin = new Pin("p1", Orientation.ROW, 2, Direction.RIGHT, 5, 2);
        grid.place(pin);
        Hazard hazard = new Hazard("h1", 3, 2);
        Character character = new Character("c1", UnitType.MAGE, 0, 2);
        grid.place(hazard);
        grid.place(character);

        PullOutcome outcome = PhysicsResolver.applyPull(grid, pin);

        assertTrue(outcome.getRescued().isEmpty());
        assertEquals(1, outcome.getLost().size());
        assertEquals("c1", outcome.getLost().get(0).getId());
        // The character should have been removed from the grid (caught).
        assertTrue(grid.getCharacters().isEmpty());
        // The hazard survives, resting at the exit-most slot.
        assertEquals(1, grid.getHazards().size());
        assertEquals(5, hazard.getCol());
    }

    @Test
    void characterNearerTheExitThanAHazardEscapesSafely() {
        Grid grid = new Grid(6, 5);
        Pin pin = new Pin("p1", Orientation.ROW, 2, Direction.RIGHT, 5, 2);
        grid.place(pin);
        Character character = new Character("c1", UnitType.TANK, 3, 2);
        Hazard hazard = new Hazard("h1", 0, 2);
        grid.place(character);
        grid.place(hazard);

        PullOutcome outcome = PhysicsResolver.applyPull(grid, pin);

        assertEquals(1, outcome.getRescued().size());
        assertTrue(outcome.getLost().isEmpty());
        assertFalse(grid.getCharacters().contains(character));
    }

    @Test
    void multipleCharactersAheadOfAHazardAllEscape() {
        Grid grid = new Grid(8, 3);
        Pin pin = new Pin("p1", Orientation.ROW, 1, Direction.RIGHT, 7, 1);
        grid.place(pin);
        Character c1 = new Character("c1", UnitType.WARRIOR, 5, 1);
        Character c2 = new Character("c2", UnitType.ARCHER, 6, 1);
        Hazard hazard = new Hazard("h1", 2, 1);
        Character trapped = new Character("c3", UnitType.MAGE, 0, 1);
        grid.place(c1);
        grid.place(c2);
        grid.place(hazard);
        grid.place(trapped);

        PullOutcome outcome = PhysicsResolver.applyPull(grid, pin);

        assertEquals(2, outcome.getRescued().size());
        assertEquals(1, outcome.getLost().size());
        assertEquals("c3", outcome.getLost().get(0).getId());
    }

    @Test
    void pullingAPinNotOnTheGridThrows() {
        Grid grid = new Grid(4, 4);
        Pin pin = new Pin("ghost", Orientation.ROW, 1, Direction.RIGHT, 3, 1);
        assertThrows(IllegalArgumentException.class, () -> PhysicsResolver.applyPull(grid, pin));
    }

    @Test
    void columnPinPullingUpRescuesCharacterAtTheTop() {
        Grid grid = new Grid(4, 6);
        Pin pin = new Pin("colPin", Orientation.COLUMN, 1, Direction.UP, 1, 0);
        grid.place(pin);
        Character character = new Character("c1", UnitType.TANK, 1, 5);
        grid.place(character);

        PullOutcome outcome = PhysicsResolver.applyPull(grid, pin);

        assertEquals(1, outcome.getRescued().size());
        assertTrue(grid.getCharacters().isEmpty());
    }
}
