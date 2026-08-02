package com.keplersharvest.ui;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;

/**
 * A full-screen modal panel: inventory, quest log, journal, dialogue and so on.
 *
 * <p>Each overlay owns its own layout and rebuilds it when shown, so it always reflects current
 * state without needing to observe the session.
 */
public abstract class Overlay {

    protected final Skin skin;

    private final Table root = new Table();
    private final Table content = new Table();
    private final Label titleLabel;
    private final Label footerLabel;
    private boolean visible;

    protected Overlay(Skin skin, String title, float width, float height) {
        this.skin = skin;

        root.setFillParent(true);
        root.setBackground(skin.getDrawable("scrim"));
        root.setVisible(false);

        Table panel = new Table(skin);
        panel.setBackground(skin.getDrawable("panel"));
        panel.pad(10f);

        titleLabel = new Label(title, skin, "heading");
        panel.add(titleLabel).left().expandX().fillX().row();
        panel.add(new Image(skin.getDrawable("divider"))).height(1f).fillX().padTop(4f).padBottom(6f).row();
        panel.add(content).grow().top().left().row();

        footerLabel = new Label("", skin, "muted");
        panel.add(footerLabel).left().padTop(6f);

        root.add(panel).width(width).height(height);
    }

    public Actor actor() {
        return root;
    }

    protected Table content() {
        return content;
    }

    protected void setTitle(String title) {
        titleLabel.setText(title);
    }

    protected void setFooter(String footer) {
        footerLabel.setText(footer);
    }

    public boolean visible() {
        return visible;
    }

    public void show() {
        visible = true;
        root.setVisible(true);
        rebuild();
    }

    public void hide() {
        visible = false;
        root.setVisible(false);
    }

    public void toggle() {
        if (visible) {
            hide();
        } else {
            show();
        }
    }

    /** Rebuilds the panel contents from current game state. Called every time it is shown. */
    protected abstract void rebuild();

    /**
     * Handles a key press while this overlay is open.
     *
     * @return true when the key was consumed
     */
    public boolean keyDown(int keycode) {
        return false;
    }

    /** Whether the in-game clock stops while this overlay is up. */
    public boolean pausesTime() {
        return true;
    }
}
