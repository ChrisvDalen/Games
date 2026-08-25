package com.moneyfirst.towerperil.rescue;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A rectangular pin-pull room. Cells are addressed (col, row) with col in
 * [0,width) and row in [0,height). At most one entity occupies a cell at a
 * time. At most one pin governs any given row and at most one governs any
 * given column (two pins can never share a line), which keeps pull
 * resolution and the level generator's brute-force search tractable.
 */
public final class Grid {
    private final int width;
    private final int height;
    private final GridEntity[][] cells; // [row][col]
    private final Map<String, Pin> pins = new LinkedHashMap<>();
    private final Map<String, Character> characters = new LinkedHashMap<>();
    private final Map<String, Hazard> hazards = new LinkedHashMap<>();

    public Grid(int width, int height) {
        if (width < 2 || height < 2) {
            throw new IllegalArgumentException("Grid must be at least 2x2");
        }
        this.width = width;
        this.height = height;
        this.cells = new GridEntity[height][width];
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public boolean inBounds(int col, int row) {
        return col >= 0 && col < width && row >= 0 && row < height;
    }

    public GridEntity occupantAt(int col, int row) {
        if (!inBounds(col, row)) {
            return null;
        }
        return cells[row][col];
    }

    public void place(GridEntity entity) {
        if (!inBounds(entity.getCol(), entity.getRow())) {
            throw new IllegalArgumentException("Entity out of bounds: " + entity);
        }
        if (cells[entity.getRow()][entity.getCol()] != null) {
            throw new IllegalStateException("Cell already occupied at (" + entity.getCol() + "," + entity.getRow() + ")");
        }
        if (entity instanceof Pin) {
            Pin pin = (Pin) entity;
            for (Pin existing : pins.values()) {
                if (existing.getOrientation() == pin.getOrientation() && existing.getLineIndex() == pin.getLineIndex()) {
                    throw new IllegalStateException("Line already has a pin: " + pin.getOrientation() + "#" + pin.getLineIndex());
                }
            }
            pins.put(pin.getId(), pin);
        } else if (entity instanceof Character) {
            characters.put(entity.getId(), (Character) entity);
        } else if (entity instanceof Hazard) {
            hazards.put(entity.getId(), (Hazard) entity);
        }
        cells[entity.getRow()][entity.getCol()] = entity;
    }

    /** Removes the entity from its current cell without deleting it from bookkeeping lists. */
    void clearCell(int col, int row) {
        if (inBounds(col, row)) {
            cells[row][col] = null;
        }
    }

    void setCell(GridEntity entity, int col, int row) {
        cells[row][col] = entity;
        entity.col = col;
        entity.row = row;
    }

    public void removePin(Pin pin) {
        clearCell(pin.getCol(), pin.getRow());
        pins.remove(pin.getId());
    }

    public void removeCharacter(Character character) {
        clearCell(character.getCol(), character.getRow());
        characters.remove(character.getId());
    }

    public void removeHazard(Hazard hazard) {
        clearCell(hazard.getCol(), hazard.getRow());
        hazards.remove(hazard.getId());
    }

    public List<Pin> getPins() {
        return new ArrayList<>(pins.values());
    }

    public List<Character> getCharacters() {
        return new ArrayList<>(characters.values());
    }

    public List<Hazard> getHazards() {
        return new ArrayList<>(hazards.values());
    }

    public Pin findPin(Orientation orientation, int lineIndex) {
        for (Pin pin : pins.values()) {
            if (pin.getOrientation() == orientation && pin.getLineIndex() == lineIndex) {
                return pin;
            }
        }
        return null;
    }

    /** Deep copy - used by the generator's brute-force solvability search and by tests. */
    public Grid copy() {
        Grid copy = new Grid(width, height);
        for (Pin p : pins.values()) {
            copy.place(new Pin(p.getId(), p.getOrientation(), p.getLineIndex(), p.getPullDirection(), p.getCol(), p.getRow()));
        }
        for (Character c : characters.values()) {
            copy.place(new Character(c.getId(), c.getType(), c.getCol(), c.getRow()));
        }
        for (Hazard h : hazards.values()) {
            copy.place(new Hazard(h.getId(), h.getCol(), h.getRow()));
        }
        return copy;
    }
}
