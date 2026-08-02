package com.keplersharvest.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.NinePatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable;

/**
 * Builds the interface skin in code: a chunky 16-bit look with hand-drawn framed panels, a warm
 * limited palette and a crisp pixel font.
 *
 * <p>Everything here is generated at start-up from the glyph table and the colours below, so the
 * whole interface style can be changed from one file and the project still ships no third-party
 * artwork. Panels are nine-patches so they scale to any panel size without smearing.
 */
public final class UiSkinFactory {

    /**
     * The interface is laid out against this virtual resolution and scaled to fit the window.
     *
     * <p>640x360 is exactly half of 720p and a third of 1080p, so at common window sizes every
     * pixel of the font and the panel frames lands on a whole number of screen pixels.
     */
    public static final float UI_WIDTH = 640f;
    public static final float UI_HEIGHT = 360f;

    // --- Palette: warm, limited, and deliberately small. -----------------------------------

    /** Dark brown body text, for use on parchment. */
    public static final Color INK = rgb(0x3a2a1c);
    /** Secondary text: hints, sources, empty slots. */
    public static final Color MUTED = rgb(0x7d6a52);
    /** Positive highlight: completed objectives, names, ready crops. */
    public static final Color ACCENT = rgb(0x2e6b3a);
    /** Attention: missing materials, warnings, interaction prompts. */
    public static final Color WARN = rgb(0xa4432a);
    /** Lettering on wood, and the title. */
    public static final Color GOLD = rgb(0xf0c860);
    /** Cream lettering for use on dark wood. */
    public static final Color CREAM = rgb(0xf3e6c8);

    private static final Color OUTLINE = rgb(0x241811);
    private static final Color WOOD_LIGHT = rgb(0xb5793f);
    private static final Color WOOD_MID = rgb(0x8a5a2e);
    private static final Color WOOD_DARK = rgb(0x5c3a1c);
    private static final Color PARCHMENT = rgb(0xefe0bd);
    private static final Color PARCHMENT_SHADE = rgb(0xddc9a0);
    private static final Color SLOT_FILL = rgb(0xd8c194);
    private static final Color SLOT_SELECTED = rgb(0xf6e6b0);
    private static final Color SCRIM = new Color(0.09f, 0.06f, 0.04f, 0.68f);

    private UiSkinFactory() {
    }

    public static Skin create() {
        Skin skin = new Skin();

        Texture white = solidTexture();
        skin.add("white", white, Texture.class);

        // Flat fills for bars, dividers and the modal dimmer.
        skin.add("scrim", skin.newDrawable("white", SCRIM), Drawable.class);
        skin.add("divider", skin.newDrawable("white", OUTLINE), Drawable.class);
        skin.add("bar-track", skin.newDrawable("white", rgb(0x5c3a1c)), Drawable.class);
        skin.add("bar-fill", skin.newDrawable("white", rgb(0x4fa04a)), Drawable.class);
        skin.add("bar-low", skin.newDrawable("white", rgb(0xc04a32)), Drawable.class);

        // Framed panels: outline, raised bevel, wood, inner shadow, parchment.
        skin.add("panel", framedPanel(skin, "panel-tex", 16, PARCHMENT), Drawable.class);
        skin.add("panel-soft", insetPanel(skin, "panel-soft-tex", 10, PARCHMENT_SHADE), Drawable.class);
        skin.add("slot", insetPanel(skin, "slot-tex", 10, SLOT_FILL), Drawable.class);
        skin.add("slot-selected", selectedSlot(skin, "slot-sel-tex", 10), Drawable.class);

        NinePatchDrawable buttonUp = raisedWood(skin, "button-tex", 12, WOOD_MID);
        NinePatchDrawable buttonOver = raisedWood(skin, "button-over-tex", 12, WOOD_LIGHT);
        NinePatchDrawable buttonDown = insetPanel(skin, "button-down-tex", 12, WOOD_DARK);
        skin.add("button", buttonUp, Drawable.class);
        skin.add("button-over", buttonOver, Drawable.class);
        skin.add("button-down", buttonDown, Drawable.class);

        BitmapFont body = pixelFont(1);
        BitmapFont heading = pixelFont(2);
        BitmapFont title = pixelFont(3);
        skin.add("body", body);
        skin.add("heading-font", heading);
        skin.add("title-font", title);

        skin.add("default", new Label.LabelStyle(body, INK));
        skin.add("muted", new Label.LabelStyle(body, MUTED));
        skin.add("accent", new Label.LabelStyle(body, ACCENT));
        skin.add("warn", new Label.LabelStyle(body, WARN));
        skin.add("cream", new Label.LabelStyle(body, CREAM));
        skin.add("heading", new Label.LabelStyle(heading, INK));
        skin.add("title", new Label.LabelStyle(title, GOLD));

        TextButton.TextButtonStyle button = new TextButton.TextButtonStyle();
        button.font = body;
        button.fontColor = CREAM;
        button.overFontColor = Color.WHITE;
        button.downFontColor = GOLD;
        button.disabledFontColor = rgb(0x9a8a72);
        button.up = buttonUp;
        button.over = buttonOver;
        button.down = buttonDown;
        skin.add("default", button);

        TextButton.TextButtonStyle menuButton = new TextButton.TextButtonStyle(button);
        menuButton.font = heading;
        skin.add("menu", menuButton);

        ScrollPane.ScrollPaneStyle scroll = new ScrollPane.ScrollPaneStyle();
        scroll.vScrollKnob = skin.newDrawable("white", WOOD_MID);
        scroll.vScroll = skin.newDrawable("white", new Color(0f, 0f, 0f, 0.12f));
        skin.add("default", scroll);

        return skin;
    }

    /** Loads the generated pixel font at an integer scale so it never blurs. */
    private static BitmapFont pixelFont(int scale) {
        BitmapFont font = new BitmapFont(Gdx.files.internal("ui/pixel-font.fnt"), false);
        font.getRegion().getTexture().setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        font.getData().setScale(scale);
        font.setUseIntegerPositions(true);
        font.getData().markupEnabled = true;
        return font;
    }

    // --- Nine-patch construction -----------------------------------------------------------

    /** A raised parchment panel inside a wooden frame. */
    private static NinePatchDrawable framedPanel(Skin skin, String name, int size, Color fill) {
        Pixmap pixmap = base(size, fill);
        ring(pixmap, size, 0, OUTLINE);
        bevelRing(pixmap, size, 1, WOOD_LIGHT, WOOD_DARK);
        ring(pixmap, size, 2, WOOD_MID);
        bevelRing(pixmap, size, 3, WOOD_DARK, WOOD_LIGHT);
        ring(pixmap, size, 4, OUTLINE);
        return register(skin, name, pixmap, 5);
    }

    /** A recessed area: dark on the top and left, light on the bottom and right. */
    private static NinePatchDrawable insetPanel(Skin skin, String name, int size, Color fill) {
        Pixmap pixmap = base(size, fill);
        ring(pixmap, size, 0, OUTLINE);
        bevelRing(pixmap, size, 1, WOOD_DARK, WOOD_LIGHT);
        return register(skin, name, pixmap, 3);
    }

    /** A recessed slot ringed in gold, for the selected toolbar entry. */
    private static NinePatchDrawable selectedSlot(Skin skin, String name, int size) {
        Pixmap pixmap = base(size, SLOT_SELECTED);
        ring(pixmap, size, 0, OUTLINE);
        ring(pixmap, size, 1, GOLD);
        return register(skin, name, pixmap, 3);
    }

    /** A raised wooden block, for buttons. */
    private static NinePatchDrawable raisedWood(Skin skin, String name, int size, Color fill) {
        Pixmap pixmap = base(size, fill);
        ring(pixmap, size, 0, OUTLINE);
        bevelRing(pixmap, size, 1, WOOD_LIGHT, WOOD_DARK);
        return register(skin, name, pixmap, 3);
    }

    private static Pixmap base(int size, Color fill) {
        Pixmap pixmap = new Pixmap(size, size, Pixmap.Format.RGBA8888);
        pixmap.setBlending(Pixmap.Blending.None);
        pixmap.setColor(fill);
        pixmap.fill();
        return pixmap;
    }

    /** Draws a one-pixel rectangle outline {@code inset} pixels in from the edge. */
    private static void ring(Pixmap pixmap, int size, int inset, Color colour) {
        pixmap.setColor(colour);
        pixmap.drawRectangle(inset, inset, size - inset * 2, size - inset * 2);
    }

    /** A ring lit from the top left: {@code light} on the top and left, {@code dark} elsewhere. */
    private static void bevelRing(Pixmap pixmap, int size, int inset, Color light, Color dark) {
        int span = size - inset * 2;
        pixmap.setColor(light);
        pixmap.drawLine(inset, inset, inset + span - 1, inset);
        pixmap.drawLine(inset, inset, inset, inset + span - 1);
        pixmap.setColor(dark);
        pixmap.drawLine(inset, inset + span - 1, inset + span - 1, inset + span - 1);
        pixmap.drawLine(inset + span - 1, inset, inset + span - 1, inset + span - 1);
    }

    private static NinePatchDrawable register(Skin skin, String name, Pixmap pixmap, int border) {
        Texture texture = new Texture(pixmap);
        texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        pixmap.dispose();
        // Handing the texture to the skin means the skin disposes it with everything else.
        skin.add(name, texture, Texture.class);
        return new NinePatchDrawable(new NinePatch(new TextureRegion(texture), border, border, border, border));
    }

    private static Texture solidTexture() {
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.WHITE);
        pixmap.fill();
        Texture texture = new Texture(pixmap);
        texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        pixmap.dispose();
        return texture;
    }

    private static Color rgb(int hex) {
        return new Color(
                ((hex >> 16) & 0xFF) / 255f,
                ((hex >> 8) & 0xFF) / 255f,
                (hex & 0xFF) / 255f,
                1f);
    }
}
