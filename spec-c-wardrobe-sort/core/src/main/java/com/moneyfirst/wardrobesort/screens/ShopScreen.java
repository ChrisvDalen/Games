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
import com.moneyfirst.wardrobesort.WardrobeIapCatalog;
import com.moneyfirst.wardrobesort.WardrobeSortGame;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Cosmetic pack (and remove-ads / avatar-personalization) storefront. Lists
 * every {@link WardrobeIapCatalog} entry with an owned/buy indicator, wired
 * straight to {@link com.moneyfirst.wardrobesort.IapService#purchase}.
 */
public final class ShopScreen extends InputAdapter implements Screen {

    private static final float WORLD_WIDTH = 20f;
    private static final float WORLD_HEIGHT = 14f;
    private static final float ROW_HEIGHT = 1.6f;

    private final WardrobeSortGame game;

    private OrthographicCamera camera;
    private Viewport viewport;
    private ShapeRenderer shapeRenderer;
    private SpriteBatch batch;
    private BitmapFont font;

    private final Map<WardrobeIapCatalog, Rectangle> rowBounds = new LinkedHashMap<>();
    private final Vector2 touchWorld = new Vector2();
    private String statusMessage = "";

    public ShopScreen(WardrobeSortGame game) {
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

        rowBounds.clear();
        float y = WORLD_HEIGHT - 1.6f;
        for (WardrobeIapCatalog offer : WardrobeIapCatalog.values()) {
            rowBounds.put(offer, new Rectangle(1f, y, WORLD_WIDTH - 2f, ROW_HEIGHT - 0.2f));
            y -= ROW_HEIGHT;
        }
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.09f, 0.10f, 0.14f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        camera.update();
        shapeRenderer.setProjectionMatrix(camera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        for (Map.Entry<WardrobeIapCatalog, Rectangle> entry : rowBounds.entrySet()) {
            boolean owned = game.iapService().isOwned(entry.getKey());
            shapeRenderer.setColor(owned ? 0.20f : 0.28f, owned ? 0.55f : 0.28f, owned ? 0.30f : 0.34f, 1f);
            Rectangle r = entry.getValue();
            shapeRenderer.rect(r.x, r.y, r.width, r.height);
        }
        shapeRenderer.end();

        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        font.draw(batch, "Shop", 0.4f, WORLD_HEIGHT - 0.15f);
        for (Map.Entry<WardrobeIapCatalog, Rectangle> entry : rowBounds.entrySet()) {
            WardrobeIapCatalog offer = entry.getKey();
            Rectangle r = entry.getValue();
            boolean owned = game.iapService().isOwned(offer);
            String label = offer.displayName() + "  -  " + (owned ? "OWNED" : "€" + offer.referencePriceEur());
            font.draw(batch, label, r.x + 0.2f, r.y + r.height - 0.2f);
        }
        font.draw(batch, statusMessage, 0.4f, 0.6f);
        batch.end();
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        viewport.unproject(touchWorld.set(screenX, screenY));
        for (Map.Entry<WardrobeIapCatalog, Rectangle> entry : rowBounds.entrySet()) {
            if (entry.getValue().contains(touchWorld.x, touchWorld.y)) {
                WardrobeIapCatalog offer = entry.getKey();
                if (game.iapService().isOwned(offer)) {
                    return true;
                }
                statusMessage = "Purchasing " + offer.displayName() + "...";
                game.iapService().purchase(offer,
                    () -> statusMessage = offer.displayName() + " unlocked!",
                    () -> statusMessage = "Purchase failed or canceled.");
                return true;
            }
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
