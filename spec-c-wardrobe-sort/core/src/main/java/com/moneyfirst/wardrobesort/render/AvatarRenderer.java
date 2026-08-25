package com.moneyfirst.wardrobesort.render;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.moneyfirst.wardrobesort.AvatarState;
import com.moneyfirst.wardrobesort.Garment;
import com.moneyfirst.wardrobesort.GarmentCatalog;
import com.moneyfirst.wardrobesort.GarmentSlot;

import java.util.EnumMap;
import java.util.Map;

/**
 * Draws the avatar as procedurally-generated flat shapes via
 * {@link ShapeRenderer} - a simple layered silhouette (head circle + torso/
 * limb rectangles) plus one colored shape per equipped {@link GarmentSlot} -
 * with no external art assets. Purely a rendering helper: it never mutates
 * game state, only reads {@link AvatarState}.
 */
public final class AvatarRenderer {

    private static final Color SKIN = new Color(0.94f, 0.80f, 0.65f, 1f);
    private static final Color SILHOUETTE_OUTLINE = new Color(0.35f, 0.30f, 0.28f, 1f);
    private static final Color EMPTY_SLOT_HINT = new Color(0.55f, 0.55f, 0.55f, 0.35f);

    private AvatarRenderer() {
    }

    /**
     * Renders the avatar silhouette plus any equipped garments, centered
     * horizontally on {@code centerX} with feet at {@code baseY}, scaled by
     * {@code scale} (roughly the desired total avatar height in world units
     * divided by 10).
     */
    public static void render(ShapeRenderer sr, AvatarState state, float centerX, float baseY, float scale) {
        Map<GarmentSlot, Rectangle> slotBounds = slotBounds(centerX, baseY, scale);

        sr.setColor(SKIN);
        // Head.
        float headRadius = 1.1f * scale;
        float headCenterY = baseY + 8.6f * scale;
        sr.circle(centerX, headCenterY, headRadius, 24);
        // Torso.
        sr.rect(centerX - 1.1f * scale, baseY + 4.2f * scale, 2.2f * scale, 4.0f * scale);
        // Legs.
        sr.rect(centerX - 1.0f * scale, baseY + 0.4f * scale, 0.85f * scale, 3.9f * scale);
        sr.rect(centerX + 0.15f * scale, baseY + 0.4f * scale, 0.85f * scale, 3.9f * scale);
        // Feet.
        sr.rect(centerX - 1.05f * scale, baseY, 0.95f * scale, 0.5f * scale);
        sr.rect(centerX + 0.10f * scale, baseY, 0.95f * scale, 0.5f * scale);

        for (GarmentSlot slot : GarmentSlot.values()) {
            Rectangle bounds = slotBounds.get(slot);
            String equippedId = state.equippedGarmentId(slot);
            if (equippedId == null) {
                sr.setColor(EMPTY_SLOT_HINT);
                sr.rect(bounds.x, bounds.y, bounds.width, bounds.height);
                continue;
            }
            Garment garment = GarmentCatalog.findById(equippedId);
            if (garment != null) {
                drawGarmentShape(sr, garment, bounds);
            }
        }
    }

    /** Draws one tray/palette entry for {@code garment} inside {@code bounds} - used by the tray and shop screens. */
    public static void renderTrayItem(ShapeRenderer sr, Garment garment, Rectangle bounds) {
        drawGarmentShape(sr, garment, bounds);
    }

    private static void drawGarmentShape(ShapeRenderer sr, Garment garment, Rectangle bounds) {
        sr.setColor(garment.r(), garment.g(), garment.b(), garment.a());
        float cx = bounds.x + bounds.width / 2f;
        float cy = bounds.y + bounds.height / 2f;
        switch (Math.floorMod(garment.shapeId(), 3)) {
            case 0:
                sr.rect(bounds.x, bounds.y, bounds.width, bounds.height);
                break;
            case 1:
                sr.circle(cx, cy, Math.min(bounds.width, bounds.height) / 2f, 20);
                break;
            default:
                sr.triangle(
                    bounds.x, bounds.y,
                    bounds.x + bounds.width, bounds.y,
                    cx, bounds.y + bounds.height);
                break;
        }
    }

    /** Approximate on-avatar rectangle for each slot's garment, in the same coordinate space as {@link #render}. */
    public static Map<GarmentSlot, Rectangle> slotBounds(float centerX, float baseY, float scale) {
        Map<GarmentSlot, Rectangle> bounds = new EnumMap<>(GarmentSlot.class);
        bounds.put(GarmentSlot.HEAD, new Rectangle(centerX - 1.1f * scale, baseY + 7.6f * scale, 2.2f * scale, 2.0f * scale));
        bounds.put(GarmentSlot.TOP, new Rectangle(centerX - 1.3f * scale, baseY + 4.2f * scale, 2.6f * scale, 4.0f * scale));
        bounds.put(GarmentSlot.BOTTOM, new Rectangle(centerX - 1.1f * scale, baseY + 0.9f * scale, 2.2f * scale, 3.4f * scale));
        bounds.put(GarmentSlot.SHOES, new Rectangle(centerX - 1.1f * scale, baseY, 2.2f * scale, 0.6f * scale));
        bounds.put(GarmentSlot.ACCESSORY, new Rectangle(centerX + 1.15f * scale, baseY + 5.5f * scale, 1.0f * scale, 1.0f * scale));
        return bounds;
    }
}
