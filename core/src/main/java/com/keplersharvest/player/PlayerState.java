package com.keplersharvest.player;

import com.keplersharvest.world.Direction;
import com.keplersharvest.world.GridPoint;
import com.keplersharvest.world.WorldPosition;

import java.util.Objects;

/** Where the player is, which way they face, and how tired they are. */
public final class PlayerState {

    /** Half-width of the player's collision box, in tile units. */
    public static final float HALF_SIZE = 0.32f;

    private String mapId;
    private WorldPosition position;
    private Direction facing = Direction.DOWN;
    private final EnergyMeter energy;
    private boolean moving;

    public PlayerState(String mapId, WorldPosition position, int baseMaxEnergy) {
        this.mapId = Objects.requireNonNull(mapId, "mapId");
        this.position = Objects.requireNonNull(position, "position");
        this.energy = new EnergyMeter(baseMaxEnergy);
    }

    public String mapId() {
        return mapId;
    }

    public WorldPosition position() {
        return position;
    }

    public GridPoint tile() {
        return position.tile();
    }

    /** The tile the player would act on: the one they are facing. */
    public GridPoint facingTile() {
        return tile().step(facing);
    }

    public Direction facing() {
        return facing;
    }

    public EnergyMeter energy() {
        return energy;
    }

    public boolean moving() {
        return moving;
    }

    public void setMoving(boolean moving) {
        this.moving = moving;
    }

    public void setFacing(Direction facing) {
        this.facing = Objects.requireNonNull(facing, "facing");
    }

    public void setPosition(WorldPosition position) {
        this.position = Objects.requireNonNull(position, "position");
    }

    public void moveTo(String mapId, WorldPosition position) {
        this.mapId = Objects.requireNonNull(mapId, "mapId");
        this.position = Objects.requireNonNull(position, "position");
    }
}
