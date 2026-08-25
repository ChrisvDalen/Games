package com.moneyfirst.towerperil.rescue;

/** Anything that can occupy a single cell of a {@link Grid}. */
public abstract class GridEntity {
    private final String id;
    int col;
    int row;

    protected GridEntity(String id, int col, int row) {
        this.id = id;
        this.col = col;
        this.row = row;
    }

    public String getId() {
        return id;
    }

    public int getCol() {
        return col;
    }

    public int getRow() {
        return row;
    }
}
