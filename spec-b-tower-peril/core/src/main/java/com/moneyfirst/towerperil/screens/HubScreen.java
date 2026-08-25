package com.moneyfirst.towerperil.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector3;

/**
 * Level-select / hub. Shows current gems + roster size, a "Play" button that
 * starts the next pin-pull level, and a "Gacha" button that opens the
 * gem-gacha/merge screen. The banner ad is shown here per the shared
 * placement rule (banner only on the level-select screen).
 */
public final class HubScreen extends InputAdapter implements Screen {
    private static final float VIRTUAL_WIDTH = 480f;
    private static final float VIRTUAL_HEIGHT = 640f;

    private final TowerPerilGame game;
    private final OrthographicCamera camera = new OrthographicCamera();
    private final Vector3 touchPoint = new Vector3();

    private final com.badlogic.gdx.math.Rectangle playButton =
            new com.badlogic.gdx.math.Rectangle(90, 320, 300, 80);
    private final com.badlogic.gdx.math.Rectangle gachaButton =
            new com.badlogic.gdx.math.Rectangle(90, 200, 300, 80);

    public HubScreen(TowerPerilGame game) {
        this.game = game;
        camera.setToOrtho(false, VIRTUAL_WIDTH, VIRTUAL_HEIGHT);
    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(this);
        game.getAdsService().showBanner();
    }

    @Override
    public void hide() {
        game.getAdsService().hideBanner();
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.08f, 0.09f, 0.14f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        camera.update();
        ShapeRenderer shapes = game.getShapeRenderer();
        shapes.setProjectionMatrix(camera.combined);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(Color.FOREST);
        shapes.rect(playButton.x, playButton.y, playButton.width, playButton.height);
        shapes.setColor(Color.GOLD);
        shapes.rect(gachaButton.x, gachaButton.y, gachaButton.width, gachaButton.height);
        shapes.end();

        game.getSpriteBatch().setProjectionMatrix(camera.combined);
        game.getSpriteBatch().begin();
        game.getFont().draw(game.getSpriteBatch(), "Tower Peril", 170, 560);
        game.getFont().draw(game.getSpriteBatch(),
                "Gems: " + game.getEconomy().getGems(), 170, 520);
        game.getFont().draw(game.getSpriteBatch(),
                "Roster: " + game.getEconomy().getRoster().size(), 170, 495);
        game.getFont().draw(game.getSpriteBatch(), "Play Level " + (game.getLevelIndex() + 1),
                playButton.x + 20, playButton.y + playButton.height / 2f + 8);
        game.getFont().draw(game.getSpriteBatch(), "Gacha / Merge",
                gachaButton.x + 20, gachaButton.y + gachaButton.height / 2f + 8);
        game.getSpriteBatch().end();
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        touchPoint.set(screenX, screenY, 0);
        camera.unproject(touchPoint);
        if (playButton.contains(touchPoint.x, touchPoint.y)) {
            game.startNextPinPullLevel();
            return true;
        }
        if (gachaButton.contains(touchPoint.x, touchPoint.y)) {
            game.setScreen(new GachaScreen(game));
            return true;
        }
        return false;
    }

    @Override
    public void resize(int width, int height) {
        camera.setToOrtho(false, VIRTUAL_WIDTH, VIRTUAL_HEIGHT);
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
