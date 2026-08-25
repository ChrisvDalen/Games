package com.moneyfirst.wardrobesort.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.moneyfirst.wardrobesort.WardrobeSortGame;

/**
 * Level/round-select hub: shows the current round number, a "play" button,
 * a shop entry point, and reserves the banner ad zone at the bottom of the
 * screen (the banner ad view itself is platform-native UI composited behind/
 * below this libGDX view by the Android/iOS launcher; this screen only
 * leaves room for it and calls {@link com.moneyfirst.wardrobesort.AdsService#showBanner()}
 * via {@link WardrobeSortGame#goToHub()}, per the "banner only on the hub"
 * placement rule in {@link com.moneyfirst.wardrobesort.AdPacingPolicy}).
 */
public final class HubScreen extends InputAdapter implements Screen {

    private static final float WORLD_WIDTH = 20f;
    private static final float WORLD_HEIGHT = 14f;
    private static final float BANNER_ZONE_HEIGHT = 1.5f;

    private final WardrobeSortGame game;

    private OrthographicCamera camera;
    private Viewport viewport;
    private ShapeRenderer shapeRenderer;
    private SpriteBatch batch;
    private BitmapFont font;

    private Rectangle playButton;
    private Rectangle shopButton;
    private final Vector2 touchWorld = new Vector2();

    public HubScreen(WardrobeSortGame game) {
        this.game = game;
    }

    @Override
    public void show() {
        camera = new OrthographicCamera();
        viewport = new FitViewport(WORLD_WIDTH, WORLD_HEIGHT, camera);
        shapeRenderer = new ShapeRenderer();
        batch = new SpriteBatch();
        font = new BitmapFont();
        Gdx.input.setInputProcessor(this);

        playButton = new Rectangle(WORLD_WIDTH / 2f - 2.5f, 6.5f, 5f, 1.6f);
        shopButton = new Rectangle(WORLD_WIDTH / 2f - 2.5f, 4.5f, 5f, 1.6f);
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.11f, 0.12f, 0.17f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        camera.update();
        shapeRenderer.setProjectionMatrix(camera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0.25f, 0.65f, 0.35f, 1f);
        shapeRenderer.rect(playButton.x, playButton.y, playButton.width, playButton.height);
        shapeRenderer.setColor(0.35f, 0.40f, 0.65f, 1f);
        shapeRenderer.rect(shopButton.x, shopButton.y, shopButton.width, shopButton.height);
        // Reserved banner zone outline at the bottom of the screen.
        shapeRenderer.setColor(0.18f, 0.18f, 0.22f, 1f);
        shapeRenderer.rect(0f, 0f, WORLD_WIDTH, BANNER_ZONE_HEIGHT);
        shapeRenderer.end();

        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        font.draw(batch, "Wardrobe Sort", WORLD_WIDTH / 2f - 2.2f, WORLD_HEIGHT - 0.6f);
        font.draw(batch, "Round " + game.roundNumber(), WORLD_WIDTH / 2f - 1.3f, WORLD_HEIGHT - 1.6f);
        font.draw(batch, "Play", playButton.x + playButton.width / 2f - 0.4f, playButton.y + playButton.height / 2f + 0.15f);
        font.draw(batch, "Shop", shopButton.x + shopButton.width / 2f - 0.4f, shopButton.y + shopButton.height / 2f + 0.15f);
        batch.end();
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        viewport.unproject(touchWorld.set(screenX, screenY));
        if (playButton.contains(touchWorld.x, touchWorld.y)) {
            game.startRound();
            return true;
        }
        if (shopButton.contains(touchWorld.x, touchWorld.y)) {
            game.goToShop();
            return true;
        }
        return false;
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
        shapeRenderer.dispose();
        batch.dispose();
        font.dispose();
    }
}
