package com.keplersharvest.ui;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.keplersharvest.game.GameSession;
import com.keplersharvest.game.InteractionTarget;
import com.keplersharvest.inventory.ItemStack;

import java.util.List;
import java.util.Optional;

/**
 * The always-on interface: clock, energy, toolbar, interaction prompt and recent notices.
 *
 * <p>Rebuilt from session state each frame's update rather than being pushed to, which keeps it
 * impossible for the HUD to drift out of sync with the simulation.
 */
public final class Hud {

    private static final float BAR_WIDTH = 190f;

    private final Skin skin;
    private final GameSession session;
    private final Table root = new Table();

    private final Label clockLabel;
    private final Label locationLabel;
    private final Label energyLabel;
    private final Image energyFill;
    private final Table energyTrack;
    private final com.badlogic.gdx.scenes.scene2d.ui.Cell<Image> energyCell;
    private final Table toolbarRow = new Table();
    private final Label promptLabel;
    private final Label noticeLabel;
    private final Label objectiveLabel;

    public Hud(Skin skin, GameSession session) {
        this.skin = skin;
        this.session = session;

        root.setFillParent(true);
        root.top().left().pad(12f);

        Table status = new Table(skin);
        status.setBackground(skin.getDrawable("panel"));
        status.pad(10f);
        clockLabel = new Label("", skin, "heading");
        locationLabel = new Label("", skin, "muted");
        status.add(clockLabel).left().row();
        status.add(locationLabel).left().padTop(2f).row();

        Table energyRow = new Table();
        energyTrack = new Table(skin);
        energyTrack.setBackground(skin.getDrawable("bar-track"));
        energyTrack.left();
        energyFill = new Image(skin.getDrawable("bar-fill"));
        energyCell = energyTrack.add(energyFill).height(10f).width(BAR_WIDTH).left();
        energyLabel = new Label("", skin, "default");
        energyRow.add(new Label("Energy", skin, "muted")).left().padRight(8f);
        energyRow.add(energyTrack).left().width(BAR_WIDTH).height(10f);
        energyRow.add(energyLabel).left().padLeft(8f);
        status.add(energyRow).left().padTop(6f);

        objectiveLabel = new Label("", skin, "accent");

        root.add(status).left().top();
        root.row();
        root.add(objectiveLabel).left().padTop(8f).row();

        promptLabel = new Label("", skin, "warn");
        noticeLabel = new Label("", skin, "default");

        Table bottom = new Table();
        bottom.add(promptLabel).left().row();
        bottom.add(noticeLabel).left().padTop(2f).row();
        bottom.add(toolbarRow).left().padTop(6f);

        root.add().expand().fill().row();
        root.add(bottom).left().bottom();
    }

    public Actor actor() {
        return root;
    }

    /** Refreshes every element from the session. Cheap enough to call each frame. */
    public void update(Optional<InteractionTarget> target) {
        clockLabel.setText("Day " + session.clock().day() + "   " + session.clock().now().clockText()
                + "   " + session.clock().phase().label());
        locationLabel.setText(session.currentMap().displayName());

        int energy = session.player().energy().current();
        int max = session.player().energy().max();
        float fraction = max == 0 ? 0f : (float) energy / max;
        energyLabel.setText(energy + " / " + max);
        energyFill.setDrawable(skin.getDrawable(fraction < 0.25f ? "bar-low" : "bar-fill"));
        energyCell.width(Math.max(2f, BAR_WIDTH * fraction));
        energyTrack.invalidate();

        objectiveLabel.setText(currentObjectiveText());
        promptLabel.setText(target.map(hit -> "[E] " + hit.prompt()).orElse(""));

        List<String> notices = session.notices();
        noticeLabel.setText(notices.isEmpty() ? "" : notices.get(notices.size() - 1));

        rebuildToolbar();
    }

    private String currentObjectiveText() {
        return session.questLog().active().stream()
                .findFirst()
                .map(state -> {
                    var objectives = state.definition().objectives();
                    for (int i = 0; i < objectives.size(); i++) {
                        if (!state.objectiveComplete(i, session)) {
                            int progress = state.progress(i, session);
                            int target = objectives.get(i).target();
                            String counter = target > 1 ? "  (" + progress + "/" + target + ")" : "";
                            return state.definition().title() + ": " + objectives.get(i).description() + counter;
                        }
                    }
                    return state.definition().title();
                })
                .orElse("");
    }

    private void rebuildToolbar() {
        toolbarRow.clear();
        for (int index = 0; index < session.toolbar().size(); index++) {
            Table cell = new Table(skin);
            boolean selected = index == session.toolbar().selectedIndex();
            cell.setBackground(skin.getDrawable(selected ? "slot-selected" : "slot"));
            cell.pad(4f);
            Optional<ItemStack> stack = session.toolbar().slot(index);
            cell.add(new Label(String.valueOf(index + 1), skin, "muted")).left().row();
            String name = stack.map(held -> shorten(held.item().name())).orElse("-");
            cell.add(new Label(name, skin, "default")).left().row();
            String count = stack.filter(held -> held.item().stackable())
                    .map(held -> "x" + held.count()).orElse(" ");
            cell.add(new Label(count, skin, "accent")).left();
            toolbarRow.add(cell).width(118f).height(64f).padRight(4f);
        }
    }

    private String shorten(String name) {
        return name.length() <= 14 ? name : name.substring(0, 13) + ".";
    }
}
