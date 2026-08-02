package com.keplersharvest.testing;

import com.keplersharvest.game.GameSession;
import com.keplersharvest.world.Direction;
import com.keplersharvest.world.GridPoint;
import com.keplersharvest.world.WorldPosition;

/** Test helpers for putting the player where a test needs them. */
public final class TestPlayer {

    private TestPlayer() {
    }

    /** Stands the player on {@code from} facing {@code target}, as if they had walked there. */
    public static void face(GameSession session, String mapId, GridPoint target, Direction facing) {
        GridPoint standing = target.step(facing.opposite());
        session.player().moveTo(mapId, WorldPosition.centreOf(standing));
        session.player().setFacing(facing);
    }

    /** Faces a tile from whichever adjacent side is walkable. */
    public static boolean faceFromAnySide(GameSession session, String mapId, GridPoint target) {
        var map = session.content().maps().require(mapId);
        for (Direction direction : Direction.values()) {
            GridPoint standing = target.step(direction.opposite());
            if (map.inBounds(standing) && !map.blocked(standing)) {
                session.player().moveTo(mapId, WorldPosition.centreOf(standing));
                session.player().setFacing(direction);
                return true;
            }
        }
        return false;
    }

    /** Selects the toolbar slot holding {@code itemId}. */
    public static boolean equip(GameSession session, String itemId) {
        int slot = session.inventory().findSlot(itemId);
        if (slot < 0 || slot >= session.toolbar().size()) {
            return false;
        }
        session.toolbar().select(slot);
        return true;
    }
}
