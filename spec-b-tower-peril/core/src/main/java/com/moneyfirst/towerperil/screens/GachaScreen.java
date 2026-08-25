package com.moneyfirst.towerperil.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector3;
import com.moneyfirst.towerperil.battle.Unit;

import java.util.List;

/**
 * Gem gacha rolling + merge screen. Spending gems rolls a weighted-rarity
 * unit ({@link com.moneyfirst.towerperil.gacha.GachaMergeSystem#roll()}) and
 * then immediately auto-merges the roster so any completed triples upgrade.
 */
public final class GachaScreen extends InputAdapter implements Screen {
    private static final float VIRTUAL_WIDTH = 480f;
    private static final float VIRTUAL_HEIGHT = 640f;
    private static final long ROLL_COST_GEMS = 100L;

    private final TowerPerilGame game;
    private final OrthographicCamera camera = new OrthographicCamera();
    private final Vector3 touchPoint = new Vector3();
    private final Rectangle rollButton = new Rectangle(90, 260, 300, 80);
    private final Rectangle backButton = new Rectangle(90, 160, 300, 60);

    private Unit lastRolled;
    private String message = "Tap ROLL to spend " + ROLL_COST_GEMS + " gems";

    public GachaScreen(TowerPerilGame game) {
        this.game = game;
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
        Gdx.gl.glClearColor(0.12f, 0.08f, 0.02f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        camera.update();
        ShapeRenderer shapes = game.getShapeRenderer();
        shapes.setProjectionMatrix(camera.combined);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(Color.GOLD);
        shapes.rect(rollButton.x, rollButton.y, rollButton.width, rollButton.height);
        shapes.setColor(Color.GRAY);
        shapes.rect(backButton.x, backButton.y, backButton.width, backButton.height);
        shapes.end();

        game.getSpriteBatch().setProjectionMatrix(camera.combined);
        game.getSpriteBatch().begin();
        game.getFont().draw(game.getSpriteBatch(), "Gacha / Merge", 170, 560);
        game.getFont().draw(game.getSpriteBatch(), "Gems: " + game.getEconomy().getGems(), 50, 500);
        game.getFont().draw(game.getSpriteBatch(), "Roster: " + game.getEconomy().getRoster().size(), 50, 470);
        game.getFont().draw(game.getSpriteBatch(), "ROLL (" + ROLL_COST_GEMS + " gems)",
                rollButton.x + 20, rollButton.y + rollButton.height / 2f + 8);
        game.getFont().draw(game.getSpriteBatch(), "Back to Hub",
                backButton.x + 40, backButton.y + backButton.height / 2f + 8);
        game.getFont().draw(game.getSpriteBatch(), message, 50, 100);
        if (lastRolled != null) {
            game.getFont().draw(game.getSpriteBatch(),
                    "Rolled: " + lastRolled.getRarity() + " " + lastRolled.getType(), 50, 70);
        }
        game.getSpriteBatch().end();
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        touchPoint.set(screenX, screenY, 0);
        camera.unproject(touchPoint);
        if (rollButton.contains(touchPoint.x, touchPoint.y)) {
            roll();
            return true;
        }
        if (backButton.contains(touchPoint.x, touchPoint.y)) {
            game.setScreen(new HubScreen(game));
            return true;
        }
        return false;
    }

    private void roll() {
        if (!game.getEconomy().canAfford(ROLL_COST_GEMS)) {
            message = "Not enough gems!";
            return;
        }
        game.getEconomy().spendGems(ROLL_COST_GEMS);
        lastRolled = game.getGachaMergeSystem().roll();
        game.getEconomy().addUnit(lastRolled);
        List<Unit> merged = game.getGachaMergeSystem().mergeAll(game.getEconomy().getRoster());
        game.getEconomy().setRoster(merged);
        message = "Rolled a new unit!";
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
