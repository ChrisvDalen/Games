package com.keplersharvest.ui;

import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.keplersharvest.game.GameSession;
import com.keplersharvest.quests.Objective;
import com.keplersharvest.quests.QuestState;

import java.util.List;

/** Active and completed tasks, with live objective progress. */
public final class QuestLogOverlay extends Overlay {

    private final GameSession session;

    public QuestLogOverlay(Skin skin, GameSession session) {
        super(skin, "Tasks", 560f, 300f);
        this.session = session;
        setFooter("Q or Esc to close");
    }

    @Override
    protected void rebuild() {
        content().clear();
        Table list = new Table();

        List<QuestState> active = session.questLog().active();
        if (active.isEmpty()) {
            list.add(new Label("Nothing outstanding. Talk to someone.", skin, "muted")).left().row();
        }
        for (QuestState state : active) {
            list.add(buildQuest(state, false)).left().fillX().padBottom(3f).row();
        }

        List<QuestState> done = session.questLog().completed();
        if (!done.isEmpty()) {
            list.add(new Label("Finished", skin, "accent")).left().padTop(4f).padBottom(2f).row();
            for (QuestState state : done) {
                list.add(buildQuest(state, true)).left().fillX().padBottom(3f).row();
            }
        }

        ScrollPane scroll = new ScrollPane(list, skin);
        scroll.setFadeScrollBars(false);
        scroll.setScrollingDisabled(true, false);
        content().add(scroll).grow().top().left();
    }

    private Table buildQuest(QuestState state, boolean finished) {
        Table entry = new Table(skin);
        entry.setBackground(skin.getDrawable("panel-soft"));
        entry.pad(6f);
        entry.add(new Label(state.definition().title(), skin, finished ? "muted" : "accent")).left().row();

        if (!finished) {
            Label summary = new Label(state.definition().summary(), skin, "default");
            summary.setWrap(true);
            entry.add(summary).left().width(490f).padTop(1f).row();

            List<Objective> objectives = state.definition().objectives();
            for (int i = 0; i < objectives.size(); i++) {
                Objective objective = objectives.get(i);
                int progress = state.progress(i, session);
                boolean complete = progress >= objective.target();
                String tick = complete ? "[x]" : "[ ]";
                String counter = objective.target() > 1 ? "  (" + progress + "/" + objective.target() + ")" : "";
                entry.add(new Label(tick + " " + objective.description() + counter,
                                skin, complete ? "accent" : "default"))
                        .left().padTop(2f).padLeft(4f).row();
            }
        }
        return entry;
    }
}
