package com.moneyfirst.towerperil.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector3;
import com.moneyfirst.towerperil.battle.EnemyWave;
import com.moneyfirst.towerperil.battle.Rarity;
import com.moneyfirst.towerperil.battle.Unit;
import com.moneyfirst.towerperil.battle.UnitType;
import com.moneyfirst.towerperil.rescue.Character;
import com.moneyfirst.towerperil.rescue.Grid;
import com.moneyfirst.towerperil.rescue.GridEntity;
import com.moneyfirst.towerperil.rescue.Hazard;
import com.moneyfirst.towerperil.rescue.PhysicsResolver;
import com.moneyfirst.towerperil.rescue.Pin;
import com.moneyfirst.towerperil.rescue.PinPullLevel;
import com.moneyfirst.towerperil.rescue.PinPullLevelGenerator;
import com.moneyfirst.towerperil.rescue.PullOutcome;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * The pin-pull rescue phase: renders the room grid and lets the player tap a
 * pin to pull it. Once every pin is spent or every character is resolved
 * (rescued or lost), the level completes: an interstitial fires (see
 * {@link com.moneyfirst.towerperil.ads.AdPacingPolicy}) and, if anyone was
 * rescued, play moves on to the auto-battle phase with the newly rescued
 * units.
 */
public final class PinPullScreen extends InputAdapter implements Screen {
    private static final float CELL_SIZE = 44f;

    private final TowerPerilGame game;
    private final PinPullLevel level;
    private final Grid grid;
    private final OrthographicCamera camera = new OrthographicCamera();
    private final Vector3 touchPoint = new Vector3();
    private final List<Unit> rescuedUnits = new ArrayList<>();
    private boolean levelComplete = false;

    public PinPullScreen(TowerPerilGame game, long seed) {
        this.game = game;
        this.level = PinPullLevelGenerator.generate(seed);
        this.grid = level.getGrid();
        float width = grid.getWidth() * CELL_SIZE;
        float height = grid.getHeight() * CELL_SIZE + 80f;
        camera.setToOrtho(false, width, height);
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
        Gdx.gl.glClearColor(0.05f, 0.06f, 0.1f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        camera.update();
        ShapeRenderer shapes = game.getShapeRenderer();
        shapes.setProjectionMatrix(camera.combined);
        shapes.begin(ShapeRenderer.ShapeType.Filled);

        shapes.setColor(Color.DARK_GRAY);
        shapes.rect(0, 80, grid.getWidth() * CELL_SIZE, grid.getHeight() * CELL_SIZE);

        for (int col = 0; col < grid.getWidth(); col++) {
            for (int row = 0; row < grid.getHeight(); row++) {
                GridEntity occupant = grid.occupantAt(col, row);
                float x = col * CELL_SIZE;
                float y = 80f + row * CELL_SIZE;
                if (occupant instanceof Pin) {
                    shapes.setColor(Color.ORANGE);
                    shapes.rect(x + 4, y + 4, CELL_SIZE - 8, CELL_SIZE - 8);
                } else if (occupant instanceof Character) {
                    shapes.setColor(colorFor(((Character) occupant).getType()));
                    shapes.circle(x + CELL_SIZE / 2f, y + CELL_SIZE / 2f, CELL_SIZE / 2.5f);
                } else if (occupant instanceof Hazard) {
                    shapes.setColor(Color.RED);
                    shapes.triangle(
                            x + CELL_SIZE / 2f, y + CELL_SIZE - 6,
                            x + 6, y + 6,
                            x + CELL_SIZE - 6, y + 6);
                }
            }
        }
        shapes.end();

        game.getSpriteBatch().setProjectionMatrix(camera.combined);
        game.getSpriteBatch().begin();
        game.getFont().draw(game.getSpriteBatch(),
                "Level " + (game.getLevelIndex()) + " - tap a pin (orange) to pull it", 10, 40);
        game.getFont().draw(game.getSpriteBatch(),
                "Rescued: " + rescuedUnits.size() + " / " + level.getTotalCharacters(), 10, 20);
        game.getSpriteBatch().end();
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        if (levelComplete) {
            return false;
        }
        touchPoint.set(screenX, screenY, 0);
        camera.unproject(touchPoint);
        int col = (int) (touchPoint.x / CELL_SIZE);
        int row = (int) ((touchPoint.y - 80f) / CELL_SIZE);
        GridEntity occupant = grid.occupantAt(col, row);
        if (occupant instanceof Pin) {
            pull((Pin) occupant);
            return true;
        }
        return false;
    }

    private void pull(Pin pin) {
        PullOutcome outcome = PhysicsResolver.applyPull(grid, pin);
        for (Character rescued : outcome.getRescued()) {
            Unit unit = Unit.rolled(rescued.getType(), Rarity.COMMON);
            rescuedUnits.add(unit);
            game.getEconomy().addUnit(unit);
        }
        if (!outcome.getLost().isEmpty()) {
            game.getAdPacingPolicy().offerRevive(
                    () -> { /* platform UI would re-spawn the character; core just logs the hook fired */ },
                    () -> { /* declined or no rewarded ad available - character stays lost */ });
        }
        if (grid.getPins().isEmpty() || grid.getCharacters().isEmpty()) {
            completeLevel();
        }
    }

    private void completeLevel() {
        levelComplete = true;
        game.getAdPacingPolicy().onPinPullLevelComplete();
        if (!rescuedUnits.isEmpty()) {
            EnemyWave wave = generateWave();
            game.setScreen(new AutoBattleScreen(game, rescuedUnits, wave));
        } else {
            game.setScreen(new HubScreen(game));
        }
    }

    private EnemyWave generateWave() {
        Random rng = new Random(level.getSeed() * 31L + 7L);
        UnitType[] types = UnitType.values();
        UnitType dominant = types[rng.nextInt(types.length)];
        double power = 12.0 + game.getLevelIndex() * 6.0 + rng.nextInt(10);
        int loot = 20 + game.getLevelIndex() * 5;
        return new EnemyWave("wave-" + level.getSeed(), power, dominant, loot);
    }

    private static Color colorFor(UnitType type) {
        switch (type) {
            case WARRIOR:
                return Color.SKY;
            case ARCHER:
                return Color.LIME;
            case MAGE:
                return Color.MAGENTA;
            case TANK:
            default:
                return Color.TAN;
        }
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
