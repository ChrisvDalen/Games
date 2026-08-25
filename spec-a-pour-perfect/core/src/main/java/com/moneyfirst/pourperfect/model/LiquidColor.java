package com.moneyfirst.pourperfect.model;

import com.badlogic.gdx.graphics.Color;

/**
 * The finite palette of liquid colors a tube can hold. Puzzles never use more than
 * {@link #values()}.length colors; {@link LevelGenerator} picks a contiguous prefix of this
 * enum (in declaration order) sized to the requested difficulty.
 *
 * <p>Each constant carries the libGDX {@link Color} used to render it. Constructing a
 * {@link Color} does not touch the GL context, so this class is safe to use from plain JUnit
 * tests with no libGDX application/backend running.
 */
public enum LiquidColor {
    RED(Color.RED),
    ORANGE(Color.ORANGE),
    YELLOW(Color.YELLOW),
    LIME(Color.LIME),
    GREEN(Color.GREEN),
    TEAL(Color.TEAL),
    CYAN(Color.CYAN),
    BLUE(Color.ROYAL),
    PURPLE(Color.PURPLE),
    MAGENTA(Color.MAGENTA),
    PINK(Color.PINK),
    BROWN(Color.BROWN);

    private final Color renderColor;

    LiquidColor(Color renderColor) {
        this.renderColor = renderColor;
    }

    /** The libGDX color to draw this liquid with. Callers must not mutate the returned instance. */
    public Color getRenderColor() {
        return renderColor;
    }
}
