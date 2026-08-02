package com.keplersharvest.ui;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.keplersharvest.dialogue.DialogueChoice;
import com.keplersharvest.dialogue.DialogueRunner;

import java.util.List;

/**
 * A conversation.
 *
 * <p>Space or E advances a line; number keys or clicks pick a reply. The overlay closes itself when
 * the runner reports the conversation is over.
 */
public final class DialogueOverlay extends Overlay {

    private DialogueRunner runner;
    private String speakerName = "";
    private Runnable onClose = () -> {
    };

    public DialogueOverlay(Skin skin) {
        super(skin, "", 590f, 150f);
    }

    public void begin(String speakerName, DialogueRunner runner, Runnable onClose) {
        this.speakerName = speakerName;
        this.runner = runner;
        this.onClose = onClose == null ? () -> {
        } : onClose;
        setTitle(speakerName);
        show();
    }

    @Override
    protected void rebuild() {
        content().clear();
        if (runner == null || runner.finished()) {
            return;
        }

        Label line = new Label(runner.currentLine(), skin, "default");
        line.setWrap(true);
        content().add(line).left().top().width(548f).expandY().row();

        List<DialogueChoice> choices = runner.choices();
        if (choices.isEmpty()) {
            setFooter(runner.atLastLine() ? "Space to finish" : "Space to continue");
            return;
        }
        setFooter("Press 1-" + choices.size() + " or click a reply");
        Table replies = new Table();
        for (int i = 0; i < choices.size(); i++) {
            int index = i;
            TextButton button = new TextButton((i + 1) + ".  " + choices.get(i).text(), skin);
            button.getLabel().setAlignment(com.badlogic.gdx.utils.Align.left);
            button.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    choose(index);
                }
            });
            replies.add(button).left().fillX().padTop(2f).row();
        }
        content().add(replies).left().fillX();
    }

    private void choose(int index) {
        runner.choose(index);
        afterStep();
    }

    private void advance() {
        runner.advance();
        afterStep();
    }

    private void afterStep() {
        if (runner.finished()) {
            close();
        } else {
            rebuild();
        }
    }

    private void close() {
        runner = null;
        hide();
        onClose.run();
    }

    @Override
    public boolean keyDown(int keycode) {
        if (runner == null) {
            return false;
        }
        List<DialogueChoice> choices = runner.choices();
        if (!choices.isEmpty() && keycode >= Input.Keys.NUM_1 && keycode <= Input.Keys.NUM_9) {
            int index = keycode - Input.Keys.NUM_1;
            if (index < choices.size()) {
                choose(index);
            }
            return true;
        }
        if (keycode == Input.Keys.SPACE || keycode == Input.Keys.E || keycode == Input.Keys.ENTER) {
            if (choices.isEmpty()) {
                advance();
            }
            return true;
        }
        if (keycode == Input.Keys.ESCAPE) {
            // Walking away mid-conversation is allowed; effects already applied stay applied.
            close();
            return true;
        }
        return false;
    }
}
