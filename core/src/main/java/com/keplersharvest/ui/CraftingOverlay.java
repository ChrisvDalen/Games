package com.keplersharvest.ui;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.keplersharvest.crafting.RecipeDefinition;
import com.keplersharvest.game.GameSession;

import java.util.List;
import java.util.Map;

/** The fabrication bench: what can be made, and exactly what is missing when it cannot. */
public final class CraftingOverlay extends Overlay {

    private final GameSession session;
    private String stationId = "";
    private List<RecipeDefinition> recipes = List.of();

    public CraftingOverlay(Skin skin, GameSession session) {
        super(skin, "Fabrication Bench", 570f, 300f);
        this.session = session;
        setFooter("Press a number to fabricate   -   Esc to close");
    }

    public void openFor(String stationId, String stationName) {
        this.stationId = stationId;
        setTitle(stationName == null || stationName.isBlank() ? "Fabrication Bench" : stationName);
        show();
    }

    @Override
    protected void rebuild() {
        content().clear();
        recipes = session.crafting().recipesFor(stationId);

        Table list = new Table();
        if (recipes.isEmpty()) {
            list.add(new Label("This bench has no patterns loaded.", skin, "muted")).left().row();
        }
        for (int i = 0; i < recipes.size(); i++) {
            list.add(buildRecipe(i, recipes.get(i))).left().fillX().padBottom(4f).row();
        }

        ScrollPane scroll = new ScrollPane(list, skin);
        scroll.setFadeScrollBars(false);
        scroll.setScrollingDisabled(true, false);
        content().add(scroll).grow().top().left();
    }

    private Table buildRecipe(int index, RecipeDefinition recipe) {
        Table row = new Table(skin);
        row.setBackground(skin.getDrawable("panel-soft"));
        row.pad(6f);

        boolean unlocked = session.crafting().unlocked(recipe);
        Map<String, Integer> missing = session.inventory().missingFrom(recipe.inputs());
        boolean canMake = unlocked && missing.isEmpty();

        String label = (index < 9 ? (index + 1) + ". " : "   ") + recipe.name()
                + "  ->  " + recipe.outputCount() + " x " + session.content().itemName(recipe.outputItemId());
        row.add(new Label(label, skin, canMake ? "accent" : "default")).left().row();
        row.add(new Label(describeInputs(recipe), skin, "muted")).left().padTop(1f).row();

        if (!unlocked) {
            String moduleName = recipe.requiresModule()
                    .flatMap(session.content()::module)
                    .map(module -> module.name())
                    .orElse("a module");
            row.add(new Label("Locked until " + moduleName + " is online.", skin, "warn")).left().padTop(2f).row();
        } else if (!missing.isEmpty()) {
            row.add(new Label("Missing " + session.describeItems(missing), skin, "warn")).left().padTop(2f).row();
        } else {
            TextButton make = new TextButton("Fabricate", skin);
            make.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    session.craft(recipe.id(), stationId);
                    rebuild();
                }
            });
            row.add(make).left().padTop(3f).row();
        }
        return row;
    }

    private String describeInputs(RecipeDefinition recipe) {
        StringBuilder text = new StringBuilder("needs ");
        boolean first = true;
        for (Map.Entry<String, Integer> input : recipe.inputs().entrySet()) {
            if (!first) {
                text.append(", ");
            }
            first = false;
            int held = session.inventory().count(input.getKey());
            text.append(held).append('/').append(input.getValue()).append(' ')
                    .append(session.content().itemName(input.getKey()));
        }
        return text.toString();
    }

    @Override
    public boolean keyDown(int keycode) {
        if (keycode >= Input.Keys.NUM_1 && keycode <= Input.Keys.NUM_9) {
            int index = keycode - Input.Keys.NUM_1;
            if (index < recipes.size()) {
                session.craft(recipes.get(index).id(), stationId);
                rebuild();
            }
            return true;
        }
        return false;
    }
}
