package com.moneyfirst.pourperfect.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector3;
import com.moneyfirst.pourperfect.PourPerfectGame;
import com.moneyfirst.pourperfect.game.GameSession;
import com.moneyfirst.pourperfect.game.PourResult;
import com.moneyfirst.pourperfect.game.UndoResult;
import com.moneyfirst.pourperfect.model.Level;
import com.moneyfirst.pourperfect.model.Move;
import com.moneyfirst.pourperfect.model.Tube;
import com.moneyfirst.pourperfect.render.TubeRenderer;

import java.util.Optional;

/**
 * The core play loop: renders the current {@link Level}'s tubes, turns taps into pours, and on
 * a win applies the ad-pacing rule and progression/cafe rewards before handing off to
 * {@link WinScreen}. Also hosts the three rewarded-video perk buttons (extra tube / hint /
 * undo-all).
 */
public final class GameplayScreen implements Screen {

    private static final float TUBE_WIDTH = 60f;
    private static final float TUBE_HEIGHT = 220f;
    private static final float TUBE_SPACING = 24f;
    private static final float PERK_BUTTON_SIZE = 64f;
    private static final float PERK_BUTTON_SPACING = 16f;
    private static final int CUPS_PER_SOLVE = 5;

    private final PourPerfectGame game;
    private final GameSession session;
    private final OrthographicCamera camera = new OrthographicCamera();
    private final Vector3 touchPoint = new Vector3();

    private int selectedTubeIndex = -1;
    private boolean solvedHandled;

    public GameplayScreen(PourPerfectGame game) {
        this.game = game;
        Level level = game.getLevelRepository().currentLevel();
        this.session = new GameSession(
                level,
                game.getProgressionState().getHintsRemaining(),
                game.getProgressionState().getUndosRemaining());
    }

    @Override
    public void show() {
        game.getAdsService().hideBanner();
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.09f, 0.10f, 0.14f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        camera.update();
        ShapeRenderer renderer = game.getShapeRenderer();
        renderer.setProjectionMatrix(camera.combined);

        TubeRenderer.Layout layout = currentLayout();
        TubeRenderer.render(renderer, session.getLevel(), layout, selectedTubeIndex);
        renderPerkButtons(renderer);

        if (!session.isWon()) {
            handleInput(layout);
        }

        if (session.isWon() && !solvedHandled) {
            handleSolved();
        }
    }

    private TubeRenderer.Layout currentLayout() {
        int tubeCount = session.getLevel().tubeCount();
        float totalWidth = tubeCount * TUBE_WIDTH + (tubeCount - 1) * TUBE_SPACING;
        float originX = -totalWidth / 2f;
        float originY = -TUBE_HEIGHT / 2f;
        return new TubeRenderer.Layout(originX, originY, TUBE_WIDTH, TUBE_HEIGHT, TUBE_SPACING);
    }

    private void handleInput(TubeRenderer.Layout layout) {
        if (!Gdx.input.justTouched()) {
            return;
        }
        touchPoint.set(Gdx.input.getX(), Gdx.input.getY(), 0);
        camera.unproject(touchPoint);

        if (tryHandlePerkButton(touchPoint.x, touchPoint.y)) {
            return;
        }

        int tapped = TubeRenderer.tubeIndexAt(session.getLevel(), layout, touchPoint.x, touchPoint.y);
        if (tapped < 0) {
            return;
        }
        if (selectedTubeIndex < 0) {
            selectedTubeIndex = tapped;
            return;
        }
        if (selectedTubeIndex == tapped) {
            selectedTubeIndex = -1;
            return;
        }
        PourResult result = session.pour(selectedTubeIndex, tapped);
        if (result == PourResult.POURED) {
            selectedTubeIndex = -1;
        } else {
            // Illegal target: treat the tap as re-selecting a new source tube instead.
            selectedTubeIndex = tapped;
        }
    }

    private void renderPerkButtons(ShapeRenderer renderer) {
        float x = -(3 * PERK_BUTTON_SIZE + 2 * PERK_BUTTON_SPACING) / 2f;
        float y = TUBE_HEIGHT / 2f + 40f;
        renderer.begin(ShapeRenderer.ShapeType.Filled);
        renderer.setColor(Color.TEAL);
        renderer.rect(x, y, PERK_BUTTON_SIZE, PERK_BUTTON_SIZE); // extra tube
        renderer.setColor(Color.GOLD);
        renderer.rect(x + PERK_BUTTON_SIZE + PERK_BUTTON_SPACING, y, PERK_BUTTON_SIZE, PERK_BUTTON_SIZE); // hint
        renderer.setColor(Color.SCARLET);
        renderer.rect(x + 2 * (PERK_BUTTON_SIZE + PERK_BUTTON_SPACING), y, PERK_BUTTON_SIZE, PERK_BUTTON_SIZE); // undo-all
        renderer.end();
    }

    private boolean tryHandlePerkButton(float worldX, float worldY) {
        float x = -(3 * PERK_BUTTON_SIZE + 2 * PERK_BUTTON_SPACING) / 2f;
        float y = TUBE_HEIGHT / 2f + 40f;

        if (inside(worldX, worldY, x, y)) {
            offerRewardedExtraTube();
            return true;
        }
        x += PERK_BUTTON_SIZE + PERK_BUTTON_SPACING;
        if (inside(worldX, worldY, x, y)) {
            offerRewardedHint();
            return true;
        }
        x += PERK_BUTTON_SIZE + PERK_BUTTON_SPACING;
        if (inside(worldX, worldY, x, y)) {
            offerRewardedUndoAll();
            return true;
        }
        return false;
    }

    private boolean inside(float px, float py, float x, float y) {
        return px >= x && px <= x + PERK_BUTTON_SIZE && py >= y && py <= y + PERK_BUTTON_SIZE;
    }

    private void offerRewardedExtraTube() {
        game.getAdsService().showRewardedIfLoaded(
                () -> session.addEmptyTube(4),
                () -> { /* not loaded yet - no-op; a real UI would show a "try again shortly" toast */ });
    }

    private void offerRewardedHint() {
        game.getAdsService().showRewardedIfLoaded(
                () -> {
                    session.grantHints(1); // rewarded watch always earns a bonus hint credit first
                    applyHint();
                },
                this::applyHint); // no ad available - fall back to the player's existing hint pool
    }

    private void applyHint() {
        Optional<Move> hint = session.requestHint();
        hint.ifPresent(move -> selectedTubeIndex = move.from());
    }

    private void offerRewardedUndoAll() {
        game.getAdsService().showRewardedIfLoaded(
                () -> session.undoAll(),
                () -> {
                    UndoResult result = session.undo();
                    if (result == UndoResult.NOTHING_TO_UNDO) {
                        selectedTubeIndex = -1;
                    }
                });
    }

    private void handleSolved() {
        solvedHandled = true;

        game.getProgressionState().onLevelSolved(CUPS_PER_SOLVE);
        game.getProgressionState().save();
        game.getCafeProgress().earnCupsForSolve();
        game.getCafeProgress().refreshEligibility(
                game.getIapService().isOwned(com.moneyfirst.pourperfect.iap.PourPerfectIapCatalog.CAFE_EXPANSION),
                game.getProgressionState().getCurrentLevelIndex());
        game.getCafeProgress().save();

        boolean showInterstitial = game.getAdPacingPolicy().onLevelSolved();
        Runnable goToWinScreen = () -> game.setScreen(new WinScreen(game));

        if (showInterstitial) {
            game.getAdsService().showInterstitialIfLoaded(() -> {
                game.getAdsService().loadInterstitial();
                goToWinScreen.run();
            });
        } else {
            goToWinScreen.run();
        }
    }

    @Override
    public void resize(int width, int height) {
        camera.setToOrtho(false, width, height);
        camera.position.set(0, 0, 0);
    }

    @Override
    public void hide() {
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
