package com.moneyfirst.pourperfect.render;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.moneyfirst.pourperfect.model.LiquidColor;
import com.moneyfirst.pourperfect.model.Level;
import com.moneyfirst.pourperfect.model.Tube;

import java.util.List;

/**
 * Draws a {@link Level}'s tubes purely with {@link ShapeRenderer} filled rectangles - no texture
 * atlas, no art assets. Also does the inverse: given a screen/world tap point, tells the caller
 * which tube (if any) was tapped, so {@code GameplayScreen} can drive selection off the same
 * layout math used to draw it.
 */
public final class TubeRenderer {

    private static final float OUTLINE_INSET = 2f;
    private static final float UNIT_INSET = 2f;

    private TubeRenderer() {
    }

    /** Layout of one row of tubes, reused by both {@link #render} and {@link #tubeIndexAt}. */
    public record Layout(float originX, float originY, float tubeWidth, float tubeHeight, float spacing) {
    }

    public static void render(ShapeRenderer renderer, Level level, Layout layout, int selectedTubeIndex) {
        List<Tube> tubes = level.getTubes();
        renderer.begin(ShapeRenderer.ShapeType.Filled);
        for (int i = 0; i < tubes.size(); i++) {
            drawTube(renderer, tubes.get(i), xFor(layout, i), layout.originY(), layout, i == selectedTubeIndex);
        }
        renderer.end();
    }

    /** Returns the index of the tube containing world point (x, y), or -1 if none does. */
    public static int tubeIndexAt(Level level, Layout layout, float worldX, float worldY) {
        int tubeCount = level.tubeCount();
        for (int i = 0; i < tubeCount; i++) {
            float x = xFor(layout, i);
            if (worldX >= x && worldX <= x + layout.tubeWidth()
                    && worldY >= layout.originY() && worldY <= layout.originY() + layout.tubeHeight()) {
                return i;
            }
        }
        return -1;
    }

    private static float xFor(Layout layout, int tubeIndex) {
        return layout.originX() + tubeIndex * (layout.tubeWidth() + layout.spacing());
    }

    private static void drawTube(ShapeRenderer renderer, Tube tube, float x, float y, Layout layout, boolean selected) {
        renderer.setColor(selected ? Color.WHITE : Color.DARK_GRAY);
        renderer.rect(x, y, layout.tubeWidth(), layout.tubeHeight());

        float innerX = x + OUTLINE_INSET;
        float innerY = y + OUTLINE_INSET;
        float innerWidth = layout.tubeWidth() - 2 * OUTLINE_INSET;
        float innerHeight = layout.tubeHeight() - 2 * OUTLINE_INSET;
        renderer.setColor(Color.BLACK);
        renderer.rect(innerX, innerY, innerWidth, innerHeight);

        int capacity = tube.getCapacity();
        float unitHeight = innerHeight / capacity;
        List<LiquidColor> contents = tube.getContents();
        for (int u = 0; u < contents.size(); u++) {
            LiquidColor color = contents.get(u);
            renderer.setColor(color.getRenderColor());
            float unitY = innerY + u * unitHeight;
            renderer.rect(innerX + UNIT_INSET, unitY + UNIT_INSET / 2f,
                    innerWidth - 2 * UNIT_INSET, unitHeight - UNIT_INSET);
        }
    }
}
