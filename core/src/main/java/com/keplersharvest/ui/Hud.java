package com.keplersharvest.ui;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Cell;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.keplersharvest.game.GameSession;
import com.keplersharvest.game.InteractionTarget;
import com.keplersharvest.inventory.ItemStack;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * The always-on interface: clock, energy, current task, interaction prompt, notices and toolbar.
 *
 * <p>Every readable element sits on a framed panel so it stays legible over the world at any time of
 * day. Panels with nothing to say hide themselves rather than leaving empty frames on screen.
 *
 * <p>Rebuilt from session state each frame rather than being pushed to, which makes it impossible
 * for the HUD to drift out of sync with the simulation.
 */
public final class Hud {

    private static final float BAR_WIDTH = 104f;
    private static final int TOOLBAR_NAME_LIMIT = 11;

    private final Skin skin;
    private final GameSession session;
    private final Table root = new Table();

    private final Label clockLabel;
    private final Label placeLabel;
    private final Label energyLabel;
    private final Image energyFill;
    private final Table energyTrack;
    private final Cell<Image> energyCell;

    private final Table objectivePanel;
    private final Label objectiveLabel;
    private final Table promptPanel;
    private final Label promptLabel;
    private final Table noticePanel;
    private final Label noticeLabel;
    private final Table toolbarRow = new Table();

    public Hud(Skin skin, GameSession session) {
        this.skin = skin;
        this.session = session;

        root.setFillParent(true);
        root.top().left().pad(6f);

        Table status = panel();
        clockLabel = new Label("", skin, "heading");
        placeLabel = new Label("", skin, "muted");
        status.add(clockLabel).left().row();
        status.add(placeLabel).left().padTop(1f).row();

        energyTrack = new Table(skin);
        energyTrack.setBackground(skin.getDrawable("bar-track"));
        energyTrack.left();
        energyFill = new Image(skin.getDrawable("bar-fill"));
        energyCell = energyTrack.add(energyFill).height(6f).width(BAR_WIDTH).left();

        energyLabel = new Label("", skin, "default");
        Table energyRow = new Table();
        energyRow.add(energyTrack).left().width(BAR_WIDTH).height(6f);
        energyRow.add(energyLabel).left().padLeft(5f);
        status.add(energyRow).left().padTop(4f);

        objectivePanel = panel();
        objectiveLabel = new Label("", skin, "accent");
        objectivePanel.add(objectiveLabel).left();

        promptPanel = panel();
        promptLabel = new Label("", skin, "warn");
        promptPanel.add(promptLabel).left();

        noticePanel = panel();
        noticeLabel = new Label("", skin, "default");
        noticePanel.add(noticeLabel).left();

        root.add(status).left().top().row();
        root.add(objectivePanel).left().top().padTop(4f).row();
        root.add().expand().fill().row();
        root.add(promptPanel).left().padBottom(3f).row();
        root.add(noticePanel).left().padBottom(3f).row();
        root.add(toolbarRow).left().bottom();
    }

    private Table panel() {
        Table table = new Table(skin);
        table.setBackground(skin.getDrawable("panel"));
        table.pad(5f);
        return table;
    }

    public Actor actor() {
        return root;
    }

    /** Refreshes every element from the session. Cheap enough to call each frame. */
    public void update(Optional<InteractionTarget> target) {
        clockLabel.setText("DAY " + session.clock().day() + "  " + session.clock().now().clockText());
        placeLabel.setText(session.clock().phase().label() + " - " + session.currentMap().displayName());

        int energy = session.player().energy().current();
        int max = session.player().energy().max();
        float fraction = max == 0 ? 0f : (float) energy / max;
        energyLabel.setText(energy + "/" + max);
        energyFill.setDrawable(skin.getDrawable(fraction < 0.25f ? "bar-low" : "bar-fill"));
        energyCell.width(Math.max(1f, BAR_WIDTH * fraction));
        energyTrack.invalidate();

        setPanelText(objectivePanel, objectiveLabel, currentObjectiveText());
        setPanelText(promptPanel, promptLabel, target.map(hit -> "[E] " + hit.prompt()).orElse(""));

        List<String> notices = session.notices();
        setPanelText(noticePanel, noticeLabel, notices.isEmpty() ? "" : notices.get(notices.size() - 1));

        rebuildToolbar();
    }

    /** An empty panel would just be a stray frame, so blank text hides the whole thing. */
    private void setPanelText(Table panel, Label label, String text) {
        label.setText(text);
        boolean visible = !text.isBlank();
        if (panel.isVisible() != visible) {
            panel.setVisible(visible);
            root.getCell(panel).height(visible ? -1f : 0f).pad(visible ? 0f : 0f);
            root.invalidateHierarchy();
        }
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
                            return objectives.get(i).description() + counter;
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
            cell.pad(3f);
            Optional<ItemStack> stack = session.toolbar().slot(index);
            cell.add(new Label(String.valueOf(index + 1), skin, "muted")).left().row();
            cell.add(new Label(stack.map(held -> shorten(held.item().name())).orElse("-"),
                    skin, "default")).left().row();
            Optional<String> count = stack.filter(held -> held.item().stackable())
                    .map(held -> "x" + held.count());
            cell.add(new Label(count.orElseGet(() -> stack
                            .map(held -> held.item().category().name().toLowerCase(Locale.ROOT))
                            .orElse(" ")),
                    skin, count.isPresent() ? "accent" : "muted")).left();
            toolbarRow.add(cell).width(72f).height(40f).padRight(2f);
        }
    }

    private String shorten(String name) {
        if (name.length() <= TOOLBAR_NAME_LIMIT) {
            return name;
        }
        return name.substring(0, TOOLBAR_NAME_LIMIT - 1).stripTrailing() + ".";
    }
}
