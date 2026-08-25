package com.moneyfirst.wardrobesort.input;

import com.badlogic.gdx.math.Rectangle;
import com.moneyfirst.wardrobesort.AvatarCompositor;
import com.moneyfirst.wardrobesort.AvatarState;
import com.moneyfirst.wardrobesort.Garment;
import com.moneyfirst.wardrobesort.GarmentSlot;
import com.moneyfirst.wardrobesort.OutfitTarget;

import java.util.Map;
import java.util.Objects;

/**
 * Translates raw touch/mouse coordinates (in the round screen's world/viewport
 * space) into tray-item pickup and avatar-slot drop events, delegating the
 * actual accept/reject decision to {@link AvatarCompositor}. Screens own the
 * libGDX {@code InputProcessor} plumbing and layout math; this class only
 * needs to be told where things currently are on screen each frame/resize via
 * {@link #updateTrayLayout} / {@link #updateAvatarSlotLayout}.
 */
public final class DragDropController {

    /** Notified when a drop is resolved by {@link AvatarCompositor}. */
    public interface Listener {
        void onDropAccepted(Garment garment, GarmentSlot slot);

        void onDropRejected(Garment garment, GarmentSlot slot, AvatarCompositor.DropOutcome outcome);
    }

    private final AvatarState avatarState;
    private final Listener listener;

    private Map<Garment, Rectangle> trayItemBounds = Map.of();
    private Map<GarmentSlot, Rectangle> avatarSlotBounds = Map.of();
    private OutfitTarget target;

    private Garment draggingGarment;
    private float dragX;
    private float dragY;

    public DragDropController(AvatarState avatarState, Listener listener) {
        this.avatarState = Objects.requireNonNull(avatarState, "avatarState");
        this.listener = Objects.requireNonNull(listener, "listener");
    }

    /** Call once per round (and whenever the target changes) before accepting input. */
    public void setTarget(OutfitTarget target) {
        this.target = Objects.requireNonNull(target, "target");
    }

    /** Call after (re)laying out the tray, e.g. on show()/resize(). */
    public void updateTrayLayout(Map<Garment, Rectangle> trayItemBounds) {
        this.trayItemBounds = Objects.requireNonNull(trayItemBounds, "trayItemBounds");
    }

    /** Call after (re)laying out the avatar, e.g. on show()/resize(). */
    public void updateAvatarSlotLayout(Map<GarmentSlot, Rectangle> avatarSlotBounds) {
        this.avatarSlotBounds = Objects.requireNonNull(avatarSlotBounds, "avatarSlotBounds");
    }

    /** @return true if a tray item was under (x, y) and pickup started. */
    public boolean touchDown(float x, float y) {
        for (Map.Entry<Garment, Rectangle> entry : trayItemBounds.entrySet()) {
            if (entry.getValue().contains(x, y)) {
                draggingGarment = entry.getKey();
                dragX = x;
                dragY = y;
                return true;
            }
        }
        return false;
    }

    public void touchDragged(float x, float y) {
        if (draggingGarment != null) {
            dragX = x;
            dragY = y;
        }
    }

    /** @return true if a garment was being dragged and this touchUp resolved a drop attempt. */
    public boolean touchUp(float x, float y) {
        if (draggingGarment == null) {
            return false;
        }
        Garment garment = draggingGarment;
        draggingGarment = null;

        GarmentSlot droppedOnSlot = findSlotAt(x, y);
        if (droppedOnSlot == null || target == null) {
            // Released outside any avatar slot: no-op drop, garment returns to tray.
            return true;
        }

        AvatarCompositor.DropOutcome outcome = AvatarCompositor.attemptDrop(avatarState, target, garment, droppedOnSlot);
        if (outcome == AvatarCompositor.DropOutcome.ACCEPTED_CORRECT_MATCH) {
            listener.onDropAccepted(garment, droppedOnSlot);
        } else {
            listener.onDropRejected(garment, droppedOnSlot, outcome);
        }
        return true;
    }

    public boolean isDragging() {
        return draggingGarment != null;
    }

    public Garment draggingGarment() {
        return draggingGarment;
    }

    public float dragX() {
        return dragX;
    }

    public float dragY() {
        return dragY;
    }

    private GarmentSlot findSlotAt(float x, float y) {
        for (Map.Entry<GarmentSlot, Rectangle> entry : avatarSlotBounds.entrySet()) {
            if (entry.getValue().contains(x, y)) {
                return entry.getKey();
            }
        }
        return null;
    }
}
