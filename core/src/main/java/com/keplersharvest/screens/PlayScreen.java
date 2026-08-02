package com.keplersharvest.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.keplersharvest.KeplersHarvestGame;
import com.keplersharvest.game.GameSession;
import com.keplersharvest.game.InteractionResult;
import com.keplersharvest.game.InteractionTarget;
import com.keplersharvest.game.PlayerActions;
import com.keplersharvest.player.PlayerState;
import com.keplersharvest.render.WorldRenderer;
import com.keplersharvest.save.SaveGameService;
import com.keplersharvest.ui.CraftingOverlay;
import com.keplersharvest.ui.DialogueOverlay;
import com.keplersharvest.ui.Hud;
import com.keplersharvest.ui.InventoryOverlay;
import com.keplersharvest.ui.JournalOverlay;
import com.keplersharvest.ui.Overlay;
import com.keplersharvest.ui.PauseOverlay;
import com.keplersharvest.ui.QuestLogOverlay;
import com.keplersharvest.ui.ReaderOverlay;
import com.keplersharvest.ui.UiSkinFactory;
import com.keplersharvest.world.Direction;
import com.keplersharvest.world.GridPoint;
import com.keplersharvest.world.Movement;
import com.keplersharvest.world.WorldPosition;

import java.util.List;
import java.util.Optional;

/**
 * The playable screen: movement, interaction, rendering and the overlay stack.
 *
 * <p>All rules live in {@link GameSession} and {@link PlayerActions}; this class only translates
 * input into calls and state into pixels.
 */
public final class PlayScreen extends ScreenAdapter {

    private final KeplersHarvestGame game;
    private final GameSession session;
    private final PlayerActions actions;
    private final WorldRenderer renderer;
    private final Stage stage;
    private final Hud hud;

    private final InventoryOverlay inventory;
    private final QuestLogOverlay questLog;
    private final JournalOverlay journal;
    private final ReaderOverlay reader;
    private final CraftingOverlay crafting;
    private final DialogueOverlay dialogue;
    private final PauseOverlay pause;
    private final List<Overlay> overlays;

    private Optional<InteractionTarget> target = Optional.empty();
    private float animationTime;

    public PlayScreen(KeplersHarvestGame game, GameSession session) {
        this.game = game;
        this.session = session;
        this.actions = new PlayerActions(session);
        this.renderer = new WorldRenderer(game.art());
        this.stage = new Stage(new FitViewport(UiSkinFactory.UI_WIDTH, UiSkinFactory.UI_HEIGHT));
        this.hud = new Hud(game.skin(), session);

        this.reader = new ReaderOverlay(game.skin());
        this.inventory = new InventoryOverlay(game.skin(), session);
        this.questLog = new QuestLogOverlay(game.skin(), session);
        this.journal = new JournalOverlay(game.skin(), session, reader);
        this.crafting = new CraftingOverlay(game.skin(), session);
        this.dialogue = new DialogueOverlay(game.skin());
        this.pause = new PauseOverlay(game.skin(),
                this::closeOverlays,
                this::saveGame,
                this::saveAndQuitToTitle,
                () -> Gdx.app.exit());
        this.overlays = List.of(dialogue, reader, crafting, inventory, questLog, journal, pause);

        stage.addActor(hud.actor());
        overlays.forEach(overlay -> stage.addActor(overlay.actor()));
    }

    @Override
    public void show() {
        InputMultiplexer input = new InputMultiplexer();
        input.addProcessor(stage);
        input.addProcessor(new KeyHandler());
        Gdx.input.setInputProcessor(input);
    }

    @Override
    public void render(float delta) {
        animationTime += delta;
        boolean paused = anyOverlayPausesTime();
        session.clock().setPaused(paused);

        if (!paused) {
            handleMovement(delta);
            session.update(delta);
        } else {
            session.player().setMoving(false);
        }

        target = actions.currentTarget();
        hud.update(target);

        Gdx.gl.glClearColor(0.03f, 0.04f, 0.06f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        renderer.render(session, target, animationTime);

        stage.getViewport().apply();
        stage.act(delta);
        stage.draw();
    }

    private boolean anyOverlayPausesTime() {
        return overlays.stream().anyMatch(overlay -> overlay.visible() && overlay.pausesTime());
    }

    private Optional<Overlay> activeOverlay() {
        return overlays.stream().filter(Overlay::visible).findFirst();
    }

    private void handleMovement(float delta) {
        float dx = 0f;
        float dy = 0f;
        if (Gdx.input.isKeyPressed(Input.Keys.A) || Gdx.input.isKeyPressed(Input.Keys.LEFT)) {
            dx -= 1f;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.D) || Gdx.input.isKeyPressed(Input.Keys.RIGHT)) {
            dx += 1f;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.S) || Gdx.input.isKeyPressed(Input.Keys.DOWN)) {
            dy -= 1f;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.W) || Gdx.input.isKeyPressed(Input.Keys.UP)) {
            dy += 1f;
        }

        PlayerState player = session.player();
        player.setMoving(dx != 0f || dy != 0f);
        if (!player.moving()) {
            return;
        }

        Direction facing = Direction.fromVector(dx, dy);
        if (facing != null) {
            player.setFacing(facing);
        }

        float length = (float) Math.sqrt(dx * dx + dy * dy);
        float speed = session.settings().playerTilesPerSecond() * delta;
        GridPoint before = player.tile();
        WorldPosition moved = Movement.resolve(session.currentMap(), player.position(),
                dx / length * speed, dy / length * speed, PlayerState.HALF_SIZE);
        player.setPosition(moved);

        GridPoint after = player.tile();
        if (!after.equals(before)) {
            if (!actions.checkPortal()) {
                session.discoverLandmarksUnder(after);
            }
        }
    }

    private void interact() {
        InteractionResult result = actions.interact();
        switch (result) {
            case InteractionResult.StartDialogue started ->
                    dialogue.begin(started.colonistName(), started.runner(), this::refreshOpenOverlays);
            case InteractionResult.OpenCrafting station ->
                    crafting.openFor(station.stationId(), station.stationName());
            case InteractionResult.ReadCrewLog log -> reader.showCrewLog(log.log());
            case InteractionResult.OfferSleep ignored -> sleep();
            case InteractionResult.Handled handled -> {
                if (!handled.message().isBlank()) {
                    session.notice(handled.message());
                }
            }
            case InteractionResult.Nothing nothing -> session.notice(nothing.reason());
        }
    }

    private void useTool() {
        InteractionResult result = actions.useTool();
        if (result instanceof InteractionResult.Nothing nothing) {
            session.notice(nothing.reason());
        }
    }

    private void sleep() {
        GameSession.SleepReport report = session.sleep();
        if (report.harvestableCrops() > 0) {
            session.notice(report.harvestableCrops() + " crop"
                    + (report.harvestableCrops() == 1 ? " is" : "s are") + " ready to pick.");
        }
        // Autosave on sleep, which is the game's natural checkpoint.
        SaveGameService.SaveOutcome outcome = game.saves().save(session);
        session.notice(outcome.succeeded() ? "Progress saved." : "Autosave failed.");
    }

    private void saveGame() {
        SaveGameService.SaveOutcome outcome = game.saves().save(session);
        pause.setStatus(switch (outcome) {
            case SaveGameService.SaveOutcome.Saved saved -> "Saved to " + saved.file();
            case SaveGameService.SaveOutcome.Failed failed -> failed.reason();
        });
    }

    private void saveAndQuitToTitle() {
        game.saves().save(session);
        game.setScreen(new TitleScreen(game));
        dispose();
    }

    private void closeOverlays() {
        overlays.forEach(Overlay::hide);
    }

    /** Dialogue can change quests and the journal, so any panel left open is stale. */
    private void refreshOpenOverlays() {
        overlays.stream().filter(Overlay::visible).forEach(Overlay::show);
    }

    @Override
    public void resize(int width, int height) {
        renderer.resize(width, height);
        stage.getViewport().update(width, height, true);
    }

    @Override
    public void dispose() {
        renderer.dispose();
        stage.dispose();
    }

    /** Keyboard handling that Scene2D did not already consume. */
    private final class KeyHandler extends InputAdapter {

        @Override
        public boolean keyDown(int keycode) {
            Optional<Overlay> active = activeOverlay();
            if (active.isPresent()) {
                Overlay overlay = active.get();
                if (overlay.keyDown(keycode)) {
                    return true;
                }
                if (keycode == Input.Keys.ESCAPE || keycode == Input.Keys.ENTER
                        || isToggleFor(overlay, keycode)) {
                    overlay.hide();
                    return true;
                }
                return false;
            }

            switch (keycode) {
                case Input.Keys.E -> interact();
                case Input.Keys.F -> useTool();
                case Input.Keys.I -> inventory.show();
                case Input.Keys.Q -> questLog.show();
                case Input.Keys.J -> journal.show();
                case Input.Keys.ESCAPE -> {
                    pause.setStatus("");
                    pause.show();
                }
                case Input.Keys.F5 -> {
                    SaveGameService.SaveOutcome outcome = game.saves().save(session);
                    session.notice(outcome.succeeded() ? "Progress saved." : "Save failed.");
                }
                default -> {
                    if (keycode >= Input.Keys.NUM_1 && keycode <= Input.Keys.NUM_8) {
                        int slot = keycode - Input.Keys.NUM_1;
                        if (slot < session.toolbar().size()) {
                            session.toolbar().select(slot);
                        }
                        return true;
                    }
                    return false;
                }
            }
            return true;
        }

        /** The key that opened a panel also closes it. */
        private boolean isToggleFor(Overlay overlay, int keycode) {
            if (overlay == inventory) {
                return keycode == Input.Keys.I;
            }
            if (overlay == questLog) {
                return keycode == Input.Keys.Q;
            }
            if (overlay == journal) {
                return keycode == Input.Keys.J;
            }
            return false;
        }
    }
}
