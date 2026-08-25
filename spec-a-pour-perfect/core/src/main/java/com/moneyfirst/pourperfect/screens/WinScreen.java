package com.moneyfirst.pourperfect.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector3;
import com.moneyfirst.pourperfect.PourPerfectGame;

/** Brief "level complete" celebration screen with a Continue button back to level select. */
public final class WinScreen implements Screen {

    private static final float CONTINUE_BUTTON_WIDTH = 220f;
    private static final float CONTINUE_BUTTON_HEIGHT = 80f;

    private final PourPerfectGame game;
    private final OrthographicCamera camera = new OrthographicCamera();
    private final Vector3 touchPoint = new Vector3();

    public WinScreen(PourPerfectGame game) {
        this.game = game;
    }

    @Override
    public void show() {
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.06f, 0.20f, 0.12f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        camera.update();
        ShapeRenderer renderer = game.getShapeRenderer();
        renderer.setProjectionMatrix(camera.combined);

        float buttonX = -CONTINUE_BUTTON_WIDTH / 2f;
        float buttonY = -CONTINUE_BUTTON_HEIGHT / 2f;

        renderer.begin(ShapeRenderer.ShapeType.Filled);
        renderer.setColor(Color.LIME);
        renderer.rect(buttonX, buttonY, CONTINUE_BUTTON_WIDTH, CONTINUE_BUTTON_HEIGHT);
        renderer.end();

        if (Gdx.input.justTouched()) {
            touchPoint.set(Gdx.input.getX(), Gdx.input.getY(), 0);
            camera.unproject(touchPoint);
            if (touchPoint.x >= buttonX && touchPoint.x <= buttonX + CONTINUE_BUTTON_WIDTH
                    && touchPoint.y >= buttonY && touchPoint.y <= buttonY + CONTINUE_BUTTON_HEIGHT) {
                game.setScreen(new LevelSelectScreen(game));
            }
        }
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
    public void hide() {
    }

    @Override
    public void dispose() {
    }
}
