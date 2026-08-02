package com.keplersharvest.ui;

import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.keplersharvest.mystery.CrewLogDefinition;
import com.keplersharvest.mystery.RevealDefinition;

import java.util.List;

/** A long-form reader for crew logs and for the final reveal. */
public final class ReaderOverlay extends Overlay {

    private String heading = "";
    private String byline = "";
    private List<String> paragraphs = List.of();

    public ReaderOverlay(Skin skin) {
        super(skin, "Data Slate", 590f, 310f);
        setFooter("Esc or Enter to close");
    }

    public void showCrewLog(CrewLogDefinition log) {
        setTitle("Recovered Data Slate");
        heading = log.title();
        byline = log.author() + (log.stardate().isBlank() ? "" : "   -   " + log.stardate());
        paragraphs = log.body();
        show();
    }

    public void showReveal(RevealDefinition reveal) {
        setTitle("Evidence Journal - Conclusion");
        heading = reveal.title();
        byline = "";
        paragraphs = reveal.body();
        show();
    }

    public void showText(String title, String bodyHeading, List<String> body) {
        setTitle(title);
        heading = bodyHeading;
        byline = "";
        paragraphs = body;
        show();
    }

    @Override
    protected void rebuild() {
        content().clear();
        Table page = new Table();
        page.add(new Label(heading, skin, "accent")).left().width(540f).row();
        if (!byline.isBlank()) {
            page.add(new Label(byline, skin, "muted")).left().padBottom(5f).row();
        }
        for (String paragraph : paragraphs) {
            Label line = new Label(paragraph, skin, "default");
            line.setWrap(true);
            page.add(line).left().width(540f).padTop(5f).row();
        }
        ScrollPane scroll = new ScrollPane(page, skin);
        scroll.setFadeScrollBars(false);
        scroll.setScrollingDisabled(true, false);
        content().add(scroll).grow().top().left();
    }
}
