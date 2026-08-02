package com.keplersharvest.ui;

import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.keplersharvest.game.GameSession;
import com.keplersharvest.mystery.CrewLogDefinition;
import com.keplersharvest.mystery.EvidenceDefinition;

/** Recovered crew logs, the clues drawn from them, and the conclusion once it is earned. */
public final class JournalOverlay extends Overlay {

    private final GameSession session;
    private final ReaderOverlay reader;

    public JournalOverlay(Skin skin, GameSession session, ReaderOverlay reader) {
        super(skin, "Evidence Journal", 600f, 320f);
        this.session = session;
        this.reader = reader;
        setFooter("J or Esc to close");
    }

    @Override
    protected void rebuild() {
        content().clear();
        Table page = new Table();

        page.add(new Label("Recovered logs  " + session.journal().foundLogCount()
                + " / " + session.journal().totalLogs(), skin, "accent")).left().padBottom(2f).row();

        if (session.journal().foundLogCount() == 0) {
            page.add(new Label("No crew logs recovered yet. Data slates glow faintly in the dark.",
                    skin, "muted")).left().row();
        }
        for (CrewLogDefinition log : session.journal().foundLogsInOrder()) {
            TextButton open = new TextButton(log.title() + "   -   " + log.author(), skin);
            open.getLabel().setAlignment(com.badlogic.gdx.utils.Align.left);
            open.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, com.badlogic.gdx.scenes.scene2d.Actor actor) {
                    hide();
                    reader.showCrewLog(log);
                }
            });
            page.add(open).left().fillX().padTop(2f).row();
        }

        page.add(new Label("What this adds up to", skin, "accent")).left().padTop(8f).padBottom(2f).row();
        if (session.journal().recordedEvidence().isEmpty()) {
            page.add(new Label("Nothing conclusive yet.", skin, "muted")).left().row();
        }
        for (EvidenceDefinition evidence : session.journal().recordedEvidence()) {
            Table entry = new Table(skin);
            entry.setBackground(skin.getDrawable("panel-soft"));
            entry.pad(6f);
            entry.add(new Label(evidence.title(), skin, "default")).left().row();
            Label text = new Label(evidence.text(), skin, "muted");
            text.setWrap(true);
            entry.add(text).left().width(520f).padTop(2f).row();
            entry.add(new Label("source: " + evidence.source(), skin, "muted")).left().padTop(2f);
            page.add(entry).left().fillX().padTop(4f).row();
        }

        page.add(buildConclusion()).left().fillX().padTop(10f).row();

        ScrollPane scroll = new ScrollPane(page, skin);
        scroll.setFadeScrollBars(false);
        scroll.setScrollingDisabled(true, false);
        content().add(scroll).grow().top().left();
    }

    private Table buildConclusion() {
        Table box = new Table(skin);
        box.setBackground(skin.getDrawable("panel-soft"));
        box.pad(7f);
        if (session.journal().revealAvailable()) {
            box.add(new Label(session.journal().reveal().title(), skin, "accent")).left().row();
            TextButton read = new TextButton("Put it together", skin);
            read.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, com.badlogic.gdx.scenes.scene2d.Actor actor) {
                    session.journal().markRevealSeen();
                    hide();
                    reader.showReveal(session.journal().reveal());
                }
            });
            box.add(read).left().padTop(4f);
            return box;
        }
        box.add(new Label("Still missing", skin, "warn")).left().row();
        int logsShort = session.journal().reveal().requiredLogs() - session.journal().foundLogCount();
        if (logsShort > 0) {
            box.add(new Label(logsShort + " more crew log" + (logsShort == 1 ? "" : "s") + " to find.",
                    skin, "muted")).left().padTop(2f).row();
        }
        for (String missing : session.journal().missingForReveal()) {
            box.add(new Label("- " + missing, skin, "muted")).left().padTop(1f).row();
        }
        return box;
    }
}
