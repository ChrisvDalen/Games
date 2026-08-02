package com.keplersharvest;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.keplersharvest.configuration.ContentLoader;
import com.keplersharvest.configuration.GameContent;
import com.keplersharvest.configuration.GdxResourceReader;
import com.keplersharvest.render.Placeholders;
import com.keplersharvest.save.SaveGameService;
import com.keplersharvest.screens.PlayScreen;
import com.keplersharvest.screens.TitleScreen;
import com.keplersharvest.ui.UiSkinFactory;

/**
 * Application entry point: loads content once, owns the shared resources, and hands control to the
 * title screen.
 */
public final class KeplersHarvestGame extends Game {

    public static final String TITLE = "Kepler's Harvest";

    private final boolean skipTitle;

    private GameContent content;
    private Placeholders art;
    private Skin skin;
    private SaveGameService saves;
    private String startupError;

    public KeplersHarvestGame() {
        this(false);
    }

    /**
     * @param skipTitle jump straight into a new game; used by {@code --smoke-test} so a build
     *                  machine exercises the world renderer, not just the menu
     */
    public KeplersHarvestGame(boolean skipTitle) {
        this.skipTitle = skipTitle;
    }

    @Override
    public void create() {
        art = new Placeholders();
        skin = UiSkinFactory.create();
        saves = new SaveGameService();
        try {
            content = ContentLoader.loadDefault(new GdxResourceReader());
        } catch (RuntimeException e) {
            // A content error must be visible rather than a black window.
            startupError = e.getMessage();
            Gdx.app.error(TITLE, "Content failed to load", e);
        }
        if (skipTitle && content != null) {
            setScreen(new PlayScreen(this,
                    com.keplersharvest.game.GameSession.newGame(content, System.currentTimeMillis())));
        } else {
            setScreen(new TitleScreen(this));
        }
    }

    public GameContent content() {
        return content;
    }

    public Placeholders art() {
        return art;
    }

    public Skin skin() {
        return skin;
    }

    public SaveGameService saves() {
        return saves;
    }

    /** Non-null when content could not be loaded; the title screen shows it instead of a menu. */
    public String startupError() {
        return startupError;
    }

    @Override
    public void dispose() {
        if (getScreen() != null) {
            getScreen().dispose();
        }
        if (skin != null) {
            skin.dispose();
        }
        if (art != null) {
            art.dispose();
        }
    }
}
