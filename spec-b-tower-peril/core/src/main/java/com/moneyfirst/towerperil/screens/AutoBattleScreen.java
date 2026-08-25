package com.moneyfirst.towerperil.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.moneyfirst.towerperil.battle.AutoBattleResolver;
import com.moneyfirst.towerperil.battle.BattleOutcome;
import com.moneyfirst.towerperil.battle.EnemyWave;
import com.moneyfirst.towerperil.battle.Unit;

import java.util.List;

/**
 * Auto-battle phase: the freshly rescued roster fights the level's enemy
 * wave automatically. Resolution is deterministic ({@link AutoBattleResolver})
 * given the wave's own seed; this screen just plays a short fill-bar
 * animation up to the already-known result and offers a rewarded "double
 * loot" video afterward.
 */
public final class AutoBattleScreen extends InputAdapter implements Screen {
    private static final float VIRTUAL_WIDTH = 480f;
    private static final float VIRTUAL_HEIGHT = 640f;
    private static final float ANIMATION_SECONDS = 1.2f;

    private final TowerPerilGame game;
    private final List<Unit> roster;
    private final EnemyWave wave;
    private final BattleOutcome outcome;
    private final OrthographicCamera camera = new OrthographicCamera();

    private float elapsed = 0f;
    private boolean resolved = false;
    private boolean lootOffered = false;

    public AutoBattleScreen(TowerPerilGame game, List<Unit> roster, EnemyWave wave) {
        this.game = game;
        this.roster = roster;
        this.wave = wave;
        long battleSeed = wave.getId().hashCode();
        this.outcome = AutoBattleResolver.resolve(roster, wave, battleSeed);
        camera.setToOrtho(false, VIRTUAL_WIDTH, VIRTUAL_HEIGHT);
    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(this);
    }

    @Override
    public void hide() {
    }

    @Override
    public void render(float delta) {
        elapsed += delta;
        Gdx.gl.glClearColor(0.1f, 0.05f, 0.08f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        float progress = Math.min(1f, elapsed / ANIMATION_SECONDS);
        if (progress >= 1f && !resolved) {
            resolved = true;
            onResolved();
        }

        camera.update();
        ShapeRenderer shapes = game.getShapeRenderer();
        shapes.setProjectionMatrix(camera.combined);
        shapes.begin(ShapeRenderer.ShapeType.Filled);

        float maxPower = (float) Math.max(outcome.getRosterEffectivePower(), outcome.getWaveEffectivePower()) * 1.1f + 1f;
        float barWidth = 380f;

        shapes.setColor(Color.SKY);
        shapes.rect(50, 420, barWidth * progress * (float) (outcome.getRosterEffectivePower() / maxPower), 40);
        shapes.setColor(Color.SCARLET);
        shapes.rect(50, 340, barWidth * progress * (float) (outcome.getWaveEffectivePower() / maxPower), 40);

        shapes.end();

        game.getSpriteBatch().setProjectionMatrix(camera.combined);
        game.getSpriteBatch().begin();
        game.getFont().draw(game.getSpriteBatch(), "Auto-Battle", 190, 560);
        game.getFont().draw(game.getSpriteBatch(), "Roster power", 50, 480);
        game.getFont().draw(game.getSpriteBatch(), "Wave power", 50, 400);
        if (resolved) {
            game.getFont().draw(game.getSpriteBatch(),
                    outcome.isWin() ? "VICTORY! Loot: " + outcome.getLootGems() + " gems (tap to continue)"
                            : "Defeat. Consolation loot: " + outcome.getLootGems() + " gems (tap to continue)",
                    50, 280);
        }
        game.getSpriteBatch().end();
    }

    private void onResolved() {
        game.getEconomy().addGems(outcome.getLootGems());
        List<Unit> merged = game.getGachaMergeSystem().mergeAll(game.getEconomy().getRoster());
        game.getEconomy().setRoster(merged);
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        if (!resolved) {
            return false;
        }
        if (!lootOffered) {
            lootOffered = true;
            game.getAdPacingPolicy().offerDoubleLootAfterBattle(
                    () -> game.getEconomy().addGems(outcome.getLootGems()),
                    () -> { /* declined or unavailable - keep the single payout already granted */ });
            return true;
        }
        game.setScreen(new HubScreen(game));
        return true;
    }

    @Override
    public void resize(int width, int height) {
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
