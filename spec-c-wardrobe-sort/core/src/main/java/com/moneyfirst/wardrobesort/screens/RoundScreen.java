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
import com.moneyfirst.wardrobesort.AvatarCompositor;
import com.moneyfirst.wardrobesort.AvatarState;
import com.moneyfirst.wardrobesort.Garment;
import com.moneyfirst.wardrobesort.GarmentSlot;
import com.moneyfirst.wardrobesort.HintReveal;
import com.moneyfirst.wardrobesort.Round;
import com.moneyfirst.wardrobesort.RoundTimer;
import com.moneyfirst.wardrobesort.WardrobeSortGame;
import com.moneyfirst.wardrobesort.input.DragDropController;
import com.moneyfirst.wardrobesort.render.AvatarRenderer;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * The core gameplay screen: target outfit display, draggable tray, timer
 * bar, and the avatar being dressed. Touch input is handled directly (this
 * class is both the {@link Screen} and its {@link com.badlogic.gdx.InputProcessor}),
 * delegating pickup/drop resolution to {@link DragDropController} and
 * {@link AvatarCompositor}.
 */
public final class RoundScreen extends InputAdapter implements Screen {

    private static final float WORLD_WIDTH = 20f;
    private static final float WORLD_HEIGHT = 14f;
    private static final float AVATAR_SCALE = 0.55f;
    private static final float AVATAR_CENTER_X = 14.5f;
    private static final float AVATAR_BASE_Y = 3.2f;
    private static final float TRAY_ITEM_SIZE = 1.6f;
    private static final float TRAY_MARGIN = 0.3f;

    private final WardrobeSortGame game;
    private final Round round;
    private final RoundTimer timer;
    private final AvatarState avatarState;
    private final DragDropController dragDropController;

    private OrthographicCamera camera;
    private Viewport viewport;
    private ShapeRenderer shapeRenderer;
    private SpriteBatch batch;
    private BitmapFont font;

    private Map<Garment, Rectangle> trayLayout = new LinkedHashMap<>();
    private final Vector2 touchWorld = new Vector2();

    private boolean roundEnded = false;
    private String statusMessage = "";

    public RoundScreen(WardrobeSortGame game, Round round, RoundTimer timer, AvatarState avatarState) {
        this.game = game;
        this.round = round;
        this.timer = timer;
        this.avatarState = avatarState;
        this.dragDropController = new DragDropController(avatarState, new DragDropController.Listener() {
            @Override
            public void onDropAccepted(Garment garment, GarmentSlot slot) {
                statusMessage = "";
                if (AvatarCompositor.isRoundWon(avatarState, round.target())) {
                    finishRound(true);
                }
            }

            @Override
            public void onDropRejected(Garment garment, GarmentSlot slot, AvatarCompositor.DropOutcome outcome) {
                statusMessage = outcome == AvatarCompositor.DropOutcome.REJECTED_WRONG_SLOT_TYPE
                    ? "That doesn't go there!"
                    : "Not quite the right item.";
            }
        });
        this.dragDropController.setTarget(round.target());
    }

    @Override
    public void show() {
        camera = new OrthographicCamera();
        viewport = new FitViewport(WORLD_WIDTH, WORLD_HEIGHT, camera);
        shapeRenderer = new ShapeRenderer();
        batch = new SpriteBatch();
        font = new BitmapFont();
        Gdx.input.setInputProcessor(this);
        layoutTray();
        dragDropController.updateAvatarSlotLayout(
            AvatarRenderer.slotBounds(AVATAR_CENTER_X, AVATAR_BASE_Y, AVATAR_SCALE));
    }

    private void layoutTray() {
        trayLayout = new LinkedHashMap<>();
        float x = TRAY_MARGIN;
        float y = TRAY_MARGIN;
        int columns = 6;
        int i = 0;
        for (Garment garment : round.tray()) {
            int col = i % columns;
            int row = i / columns;
            float itemX = TRAY_MARGIN + col * (TRAY_ITEM_SIZE + TRAY_MARGIN);
            float itemY = y + row * (TRAY_ITEM_SIZE + TRAY_MARGIN);
            trayLayout.put(garment, new Rectangle(itemX, itemY, TRAY_ITEM_SIZE, TRAY_ITEM_SIZE));
            i++;
        }
        dragDropController.updateTrayLayout(trayLayout);
    }

    @Override
    public void render(float delta) {
        if (!roundEnded) {
            timer.update(delta);
            if (timer.isExpired()) {
                finishRound(false);
            }
        }

        Gdx.gl.glClearColor(0.10f, 0.11f, 0.16f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        camera.update();
        shapeRenderer.setProjectionMatrix(camera.combined);

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        // Timer bar.
        float timerFrac = Math.max(0f, timer.remainingSeconds() / timer.totalSeconds());
        shapeRenderer.setColor(0.25f, 0.25f, 0.30f, 1f);
        shapeRenderer.rect(0.3f, WORLD_HEIGHT - 0.6f, WORLD_WIDTH - 0.6f, 0.35f);
        shapeRenderer.setColor(timerFrac > 0.3f ? 0.30f : 0.80f, timerFrac > 0.3f ? 0.75f : 0.20f, 0.35f, 1f);
        shapeRenderer.rect(0.3f, WORLD_HEIGHT - 0.6f, (WORLD_WIDTH - 0.6f) * timerFrac, 0.35f);

        // Target outfit preview swatches (top-right).
        float previewX = WORLD_WIDTH - 3.6f;
        float previewY = WORLD_HEIGHT - 2.6f;
        int slotIndex = 0;
        for (GarmentSlot slot : GarmentSlot.values()) {
            Garment required = round.target().required(slot);
            if (required != null) {
                Rectangle swatch = new Rectangle(previewX + (slotIndex % 5) * 0.7f, previewY, 0.55f, 0.55f);
                AvatarRenderer.renderTrayItem(shapeRenderer, required, swatch);
            }
            slotIndex++;
        }

        // Avatar.
        AvatarRenderer.render(shapeRenderer, avatarState, AVATAR_CENTER_X, AVATAR_BASE_Y, AVATAR_SCALE);

        // Tray.
        for (Map.Entry<Garment, Rectangle> entry : trayLayout.entrySet()) {
            if (entry.getKey().equals(dragDropController.draggingGarment())) {
                continue;
            }
            AvatarRenderer.renderTrayItem(shapeRenderer, entry.getKey(), entry.getValue());
        }

        // Ghost of the item currently being dragged, following the touch point.
        if (dragDropController.isDragging()) {
            Rectangle ghost = new Rectangle(
                dragDropController.dragX() - TRAY_ITEM_SIZE / 2f,
                dragDropController.dragY() - TRAY_ITEM_SIZE / 2f,
                TRAY_ITEM_SIZE, TRAY_ITEM_SIZE);
            AvatarRenderer.renderTrayItem(shapeRenderer, dragDropController.draggingGarment(), ghost);
        }

        shapeRenderer.end();

        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        font.draw(batch, "Round " + round.roundNumber(), 0.4f, WORLD_HEIGHT - 0.15f);
        font.draw(batch, statusMessage, 0.4f, 1.2f);
        font.draw(batch, "Hints: " + game.hintSystem().hintsRemaining(), WORLD_WIDTH - 2.2f, 0.6f);
        batch.end();
    }

    private void finishRound(boolean won) {
        if (roundEnded) {
            return;
        }
        roundEnded = true;
        game.onRoundFinished(won);
    }

    // -- Input handling: screen pixel coordinates are unprojected to this screen's world space. --

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        viewport.unproject(touchWorld.set(screenX, screenY));
        return dragDropController.touchDown(touchWorld.x, touchWorld.y);
    }

    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) {
        viewport.unproject(touchWorld.set(screenX, screenY));
        dragDropController.touchDragged(touchWorld.x, touchWorld.y);
        return true;
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        viewport.unproject(touchWorld.set(screenX, screenY));
        return dragDropController.touchUp(touchWorld.x, touchWorld.y);
    }

    /** Wired to a "watch ad for +10s" button by the platform UI layer. */
    public void requestExtraTime() {
        game.requestRewardedExtraTime(() -> statusMessage = "+10 seconds!");
    }

    /** Wired to a "watch ad for a hint" / "use hint" button by the platform UI layer. */
    public void useHint() {
        Optional<HintReveal> reveal = game.hintSystem().useHint(round.target(), round.tray());
        if (reveal.isPresent()) {
            HintReveal r = reveal.get();
            statusMessage = "Hint: check slot " + (trayIndexDisplay(r.trayIndex()));
        } else {
            statusMessage = "No hints left - watch a video for one!";
            game.requestRewardedHint(this::useHint);
        }
    }

    private static int trayIndexDisplay(int index) {
        return index + 1;
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
