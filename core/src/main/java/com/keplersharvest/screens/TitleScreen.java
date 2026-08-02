package com.keplersharvest.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.keplersharvest.KeplersHarvestGame;
import com.keplersharvest.game.GameSession;
import com.keplersharvest.save.SaveGameService;
import com.keplersharvest.ui.UiSkinFactory;

/** Title screen: new game, continue, quit. */
public final class TitleScreen extends ScreenAdapter {

    private final KeplersHarvestGame game;
    private final Stage stage;
    private final Label statusLabel;

    public TitleScreen(KeplersHarvestGame game) {
        this.game = game;
        this.stage = new Stage(new FitViewport(UiSkinFactory.UI_WIDTH, UiSkinFactory.UI_HEIGHT));
        this.statusLabel = new Label("", game.skin(), "warn");

        Table root = new Table();
        root.setFillParent(true);
        root.center();

        root.add(new Label("KEPLER'S HARVEST", game.skin(), "title")).padBottom(3f).row();
        root.add(new Label("Meridian Station, Ilyra   -   colony status: abandoned",
                game.skin(), "cream")).padBottom(14f).row();

        if (game.startupError() != null) {
            Table panel = framedPanel();
            Label error = new Label("Content failed to load:\n" + game.startupError(), game.skin(), "warn");
            error.setWrap(true);
            panel.add(error).width(400f).padBottom(8f).row();
            panel.add(quitButton()).width(180f).row();
            root.add(panel).row();
        } else {
            Table menu = framedPanel();
            menu.add(newGameButton()).width(190f).padBottom(4f).row();
            menu.add(continueButton()).width(190f).padBottom(4f).row();
            menu.add(quitButton()).width(190f).row();
            root.add(menu).padBottom(8f).row();

            root.add(statusLabel).width(420f).row();
            root.add(new Label("WASD move   E interact   F tool   1-8 toolbar", game.skin(), "cream"))
                    .padTop(12f).row();
            root.add(new Label("I pack   Q tasks   J journal   Esc pause", game.skin(), "cream"))
                    .padTop(2f).row();
        }

        stage.addActor(root);
    }

    /** Wooden frame shared by the menu and the content-error panel. */
    private Table framedPanel() {
        Table panel = new Table(game.skin());
        panel.setBackground(game.skin().getDrawable("panel"));
        panel.pad(9f);
        return panel;
    }

    private TextButton newGameButton() {
        TextButton button = new TextButton("New game", game.skin(), "menu");
        button.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                GameSession session = GameSession.newGame(game.content(), System.currentTimeMillis());
                game.setScreen(new PlayScreen(game, session));
                dispose();
            }
        });
        return button;
    }

    private TextButton continueButton() {
        TextButton button = new TextButton("Continue", game.skin(), "menu");
        boolean hasSave = game.saves().hasSave();
        button.setDisabled(!hasSave);
        if (!hasSave) {
            statusLabel.setText("No save found yet - start a new game.");
        }
        button.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (button.isDisabled()) {
                    return;
                }
                loadGame();
            }
        });
        return button;
    }

    private void loadGame() {
        SaveGameService.LoadOutcome outcome = game.saves().load(game.content());
        switch (outcome) {
            case SaveGameService.LoadOutcome.Loaded loaded -> {
                game.setScreen(new PlayScreen(game, loaded.session()));
                dispose();
            }
            case SaveGameService.LoadOutcome.NoSave ignored ->
                    statusLabel.setText("No save found yet - start a new game.");
            case SaveGameService.LoadOutcome.Unreadable unreadable ->
                    statusLabel.setText("Could not load: " + unreadable.reason());
            case SaveGameService.LoadOutcome.Invalid invalid ->
                    statusLabel.setText("Save file is damaged: " + String.join("; ", invalid.problems()));
        }
    }

    private TextButton quitButton() {
        TextButton button = new TextButton("Quit", game.skin(), "menu");
        button.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                Gdx.app.exit();
            }
        });
        return button;
    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(stage);
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.114f, 0.086f, 0.063f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        stage.act(delta);
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    @Override
    public void dispose() {
        stage.dispose();
    }
}
