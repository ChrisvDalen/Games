package com.keplersharvest.ui;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;

/** Pause menu: resume, save, or leave to the title screen. */
public final class PauseOverlay extends Overlay {

    private final Runnable onResume;
    private final Runnable onSave;
    private final Runnable onQuitToTitle;
    private final Runnable onExit;
    private String status = "";

    public PauseOverlay(Skin skin, Runnable onResume, Runnable onSave, Runnable onQuitToTitle, Runnable onExit) {
        super(skin, "Paused", 300f, 200f);
        this.onResume = onResume;
        this.onSave = onSave;
        this.onQuitToTitle = onQuitToTitle;
        this.onExit = onExit;
        setFooter("Esc to resume");
    }

    /** Shown under the buttons after a save attempt. */
    public void setStatus(String status) {
        this.status = status == null ? "" : status;
        if (visible()) {
            rebuild();
        }
    }

    @Override
    protected void rebuild() {
        content().clear();
        content().add(button("Resume", onResume)).fillX().padBottom(4f).row();
        content().add(button("Save game", onSave)).fillX().padBottom(4f).row();
        content().add(button("Save and quit to title", onQuitToTitle)).fillX().padBottom(4f).row();
        content().add(button("Quit to desktop", onExit)).fillX().padBottom(4f).row();
        content().add(new Label(status, skin, "accent")).left().padTop(3f);
    }

    private TextButton button(String text, Runnable action) {
        TextButton button = new TextButton(text, skin);
        button.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                action.run();
            }
        });
        return button;
    }
}
