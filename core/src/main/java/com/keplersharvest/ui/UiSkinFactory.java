package com.keplersharvest.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;

/**
 * Builds the Scene2D skin in code.
 *
 * <p>libGDX's built-in bitmap font plus flat tinted rectangles gives a coherent interface with no
 * downloaded skin files, which keeps the project free of third-party assets. Swapping in a real skin
 * later means replacing this one class.
 */
public final class UiSkinFactory {

    /** The interface is laid out against this virtual resolution and scaled to the window. */
    public static final float UI_WIDTH = 1024f;
    public static final float UI_HEIGHT = 576f;

    public static final Color INK = new Color(0.90f, 0.93f, 0.96f, 1f);
    public static final Color MUTED = new Color(0.62f, 0.68f, 0.75f, 1f);
    public static final Color ACCENT = new Color(0.55f, 0.86f, 0.80f, 1f);
    public static final Color WARN = new Color(0.95f, 0.72f, 0.42f, 1f);
    public static final Color PANEL = new Color(0.07f, 0.09f, 0.12f, 0.94f);
    public static final Color PANEL_SOFT = new Color(0.12f, 0.15f, 0.19f, 0.96f);
    public static final Color SCRIM = new Color(0.02f, 0.03f, 0.05f, 0.72f);

    private UiSkinFactory() {
    }

    public static Skin create() {
        Skin skin = new Skin();

        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.WHITE);
        pixmap.fill();
        Texture white = new Texture(pixmap);
        pixmap.dispose();
        skin.add("white", white);

        skin.add("panel", skin.newDrawable("white", PANEL), Drawable.class);
        skin.add("panel-soft", skin.newDrawable("white", PANEL_SOFT), Drawable.class);
        skin.add("scrim", skin.newDrawable("white", SCRIM), Drawable.class);
        skin.add("slot", skin.newDrawable("white", new Color(0.16f, 0.19f, 0.24f, 0.95f)), Drawable.class);
        skin.add("slot-selected", skin.newDrawable("white", new Color(0.33f, 0.58f, 0.55f, 1f)), Drawable.class);
        skin.add("bar-track", skin.newDrawable("white", new Color(0.14f, 0.16f, 0.2f, 0.95f)), Drawable.class);
        skin.add("bar-fill", skin.newDrawable("white", new Color(0.45f, 0.82f, 0.62f, 1f)), Drawable.class);
        skin.add("bar-low", skin.newDrawable("white", new Color(0.87f, 0.45f, 0.36f, 1f)), Drawable.class);
        skin.add("divider", skin.newDrawable("white", new Color(1f, 1f, 1f, 0.10f)), Drawable.class);
        skin.add("button", skin.newDrawable("white", new Color(0.17f, 0.22f, 0.27f, 1f)), Drawable.class);
        skin.add("button-over", skin.newDrawable("white", new Color(0.24f, 0.32f, 0.38f, 1f)), Drawable.class);
        skin.add("button-down", skin.newDrawable("white", new Color(0.30f, 0.48f, 0.46f, 1f)), Drawable.class);

        BitmapFont body = new BitmapFont();
        body.getData().markupEnabled = true;
        skin.add("body", body);

        BitmapFont title = new BitmapFont();
        title.getData().markupEnabled = true;
        title.getData().setScale(2.0f);
        skin.add("title-font", title);

        BitmapFont heading = new BitmapFont();
        heading.getData().markupEnabled = true;
        heading.getData().setScale(1.35f);
        skin.add("heading-font", heading);

        skin.add("default", new Label.LabelStyle(body, INK));
        skin.add("muted", new Label.LabelStyle(body, MUTED));
        skin.add("accent", new Label.LabelStyle(body, ACCENT));
        skin.add("warn", new Label.LabelStyle(body, WARN));
        skin.add("heading", new Label.LabelStyle(heading, INK));
        skin.add("title", new Label.LabelStyle(title, ACCENT));

        TextButton.TextButtonStyle button = new TextButton.TextButtonStyle();
        button.font = body;
        button.fontColor = INK;
        button.overFontColor = Color.WHITE;
        button.downFontColor = Color.WHITE;
        button.disabledFontColor = MUTED;
        button.up = skin.getDrawable("button");
        button.over = skin.getDrawable("button-over");
        button.down = skin.getDrawable("button-down");
        skin.add("default", button);

        TextButton.TextButtonStyle menuButton = new TextButton.TextButtonStyle(button);
        menuButton.font = heading;
        skin.add("menu", menuButton);

        ScrollPane.ScrollPaneStyle scroll = new ScrollPane.ScrollPaneStyle();
        scroll.vScrollKnob = skin.newDrawable("white", new Color(0.35f, 0.42f, 0.5f, 0.9f));
        scroll.vScroll = skin.newDrawable("white", new Color(1f, 1f, 1f, 0.06f));
        skin.add("default", scroll);

        return skin;
    }
}
