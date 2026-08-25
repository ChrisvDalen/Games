package com.moneyfirst.wardrobesort.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.moneyfirst.wardrobesort.WardrobeSortGame;

/**
 * Shown after {@link RoundScreen} finishes (win or timeout), after the
 * per-round interstitial (if any) has already been resolved by
 * {@link WardrobeSortGame#onRoundFinished}. Offers "next round" (win) or
 * "retry" (timeout).
 */
public final class ResultsScreen extends InputAdapter implements Screen {

    private static final float WORLD_WIDTH = 20f;
    private static final float WORLD_HEIGHT = 14f;

    private final WardrobeSortGame game;
    private final boolean won;
    private final int roundNumber;

    private OrthographicCamera camera;
    private Viewport viewport;
    private SpriteBatch batch;
    private BitmapFont font;

    public ResultsScreen(WardrobeSortGame game, boolean won, int roundNumber) {
        this.game = game;
        this.won = won;
        this.roundNumber = roundNumber;
    }

    @Override
    public void show() {
        camera = new OrthographicCamera();
        viewport = new FitViewport(WORLD_WIDTH, WORLD_HEIGHT, camera);
        batch = new SpriteBatch();
        font = new BitmapFont();
        Gdx.input.setInputProcessor(this);
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(won ? 0.10f : 0.18f, won ? 0.16f : 0.10f, 0.12f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        camera.update();
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        font.draw(batch, won ? "Outfit complete!" : "Time's up!", WORLD_WIDTH / 2f - 2.2f, WORLD_HEIGHT / 2f + 1f);
        font.draw(batch, "Round " + roundNumber, WORLD_WIDTH / 2f - 1.3f, WORLD_HEIGHT / 2f);
        font.draw(batch, "Tap to continue", WORLD_WIDTH / 2f - 1.6f, WORLD_HEIGHT / 2f - 1.5f);
        batch.end();
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        if (won) {
            game.advanceToNextRound();
        } else {
            game.startRound();
        }
        return true;
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
    }

    @Override
    public void pause() {
    }

    @Override
    public void resume() {
    }

    @Override
    public void hide() {
    }

    @Override
    public void dispose() {
        batch.dispose();
        font.dispose();
    }
}
