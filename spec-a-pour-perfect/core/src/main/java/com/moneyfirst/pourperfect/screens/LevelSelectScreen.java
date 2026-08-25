package com.moneyfirst.pourperfect.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector3;
import com.moneyfirst.pourperfect.PourPerfectGame;
import com.moneyfirst.pourperfect.state.LevelProgressionState;
import com.moneyfirst.pourperfect.state.StreakTracker;

/**
 * The hub screen: shows current level number, solved count, streak, cups, and a Play button.
 * Per the architecture doc's placement rule, this is the only screen that shows the banner ad.
 */
public final class LevelSelectScreen implements Screen {

    private static final float PLAY_BUTTON_WIDTH = 220f;
    private static final float PLAY_BUTTON_HEIGHT = 80f;

    private final PourPerfectGame game;
    private final OrthographicCamera camera = new OrthographicCamera();
    private final Vector3 touchPoint = new Vector3();

    public LevelSelectScreen(PourPerfectGame game) {
        this.game = game;
    }

    @Override
    public void show() {
        game.getAdsService().showBanner();
    }

    @Override
    public void hide() {
        game.getAdsService().hideBanner();
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.09f, 0.10f, 0.14f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        camera.update();
        ShapeRenderer renderer = game.getShapeRenderer();
        renderer.setProjectionMatrix(camera.combined);

        float buttonX = -PLAY_BUTTON_WIDTH / 2f;
        float buttonY = -PLAY_BUTTON_HEIGHT / 2f;

        renderer.begin(ShapeRenderer.ShapeType.Filled);
        renderer.setColor(Color.ROYAL);
        renderer.rect(buttonX, buttonY, PLAY_BUTTON_WIDTH, PLAY_BUTTON_HEIGHT);
        renderer.end();

        if (Gdx.input.justTouched()) {
            touchPoint.set(Gdx.input.getX(), Gdx.input.getY(), 0);
            camera.unproject(touchPoint);
            if (touchPoint.x >= buttonX && touchPoint.x <= buttonX + PLAY_BUTTON_WIDTH
                    && touchPoint.y >= buttonY && touchPoint.y <= buttonY + PLAY_BUTTON_HEIGHT) {
                game.setScreen(new GameplayScreen(game));
            }
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER) || Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            game.setScreen(new GameplayScreen(game));
        }
    }

    /** Human-readable summary for the HUD (also handy for tests that just want a status line). */
    public static String statusLine(LevelProgressionState state, com.badlogic.gdx.Preferences streakPrefs) {
        return "Level " + state.getCurrentLevelIndex()
                + " | Solved " + state.getSolvedCount()
                + " | Streak " + StreakTracker.getCurrentStreak(streakPrefs)
                + " | Cups " + state.getCups();
    }

    @Override
    public void resize(int width, int height) {
        camera.setToOrtho(false, width, height);
        camera.position.set(0, 0, 0);
    }

    @Override
    public void pause() {
    }

    @Override
    public void resume() {
    }

    @Override
    public void dispose() {
    }
}
