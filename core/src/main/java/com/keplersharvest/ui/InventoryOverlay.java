package com.keplersharvest.ui;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.keplersharvest.game.GameSession;
import com.keplersharvest.inventory.ItemStack;

import java.util.Optional;

/** The full pack, with a description of whatever the toolbar has selected. */
public final class InventoryOverlay extends Overlay {

    private static final int COLUMNS = 6;

    private final GameSession session;

    public InventoryOverlay(Skin skin, GameSession session) {
        super(skin, "Pack", 580f, 310f);
        this.session = session;
        setFooter("1-8 select a toolbar slot   -   I or Esc to close");
    }

    @Override
    protected void rebuild() {
        content().clear();

        Table grid = new Table();
        int size = session.inventory().size();
        for (int index = 0; index < size; index++) {
            Table cell = buildSlot(index);
            grid.add(cell).width(86f).height(40f).pad(3f);
            if ((index + 1) % COLUMNS == 0) {
                grid.row();
            }
        }
        content().add(grid).left().top().row();

        content().add(buildDetails()).left().top().padTop(8f).width(552f);
    }

    private Table buildSlot(int index) {
        Table cell = new Table(skin);
        boolean onToolbar = index < session.toolbar().size();
        boolean selected = onToolbar && index == session.toolbar().selectedIndex();
        cell.setBackground(skin.getDrawable(selected ? "slot-selected" : "slot"));
        cell.pad(3f);

        Optional<ItemStack> stack = session.inventory().slot(index);
        String corner = onToolbar ? String.valueOf(index + 1) : " ";
        cell.add(new Label(corner, skin, "muted")).left().top().row();
        if (stack.isPresent()) {
            ItemStack held = stack.get();
            cell.add(new Label(shorten(held.item().name()), skin, "default")).left().width(80f).row();
            if (held.item().stackable()) {
                cell.add(new Label("x" + held.count(), skin, "accent")).left();
            } else {
                cell.add(new Label(held.item().category().name().toLowerCase(java.util.Locale.ROOT),
                        skin, "muted")).left();
            }
        } else {
            cell.add(new Label("-", skin, "muted")).left().row();
            cell.add(new Label("", skin, "muted")).left();
        }
        return cell;
    }

    private Table buildDetails() {
        Table details = new Table(skin);
        details.setBackground(skin.getDrawable("panel-soft"));
        details.pad(7f);
        Optional<ItemStack> selected = session.toolbar().selected();
        if (selected.isEmpty()) {
            details.add(new Label("Nothing equipped. Press 1-8 to choose a toolbar slot.", skin, "muted")).left();
            return details;
        }
        ItemStack stack = selected.get();
        details.add(new Label(stack.item().name(), skin, "accent")).left().row();
        Label description = new Label(stack.item().description(), skin, "default");
        description.setWrap(true);
        details.add(description).left().width(524f).padTop(2f).row();
        String meta = stack.item().category().name().toLowerCase(java.util.Locale.ROOT)
                + (stack.item().stackable() ? "   -   stacks to " + stack.item().maxStack() : "   -   does not stack");
        details.add(new Label(meta, skin, "muted")).left().padTop(3f);
        return details;
    }

    /** Grid cells are narrow; the full name is always visible in the details panel below. */
    private String shorten(String name) {
        return name.length() <= 13 ? name : name.substring(0, 12) + ".";
    }

    @Override
    public boolean keyDown(int keycode) {
        if (keycode >= Input.Keys.NUM_1 && keycode <= Input.Keys.NUM_8) {
            int slot = keycode - Input.Keys.NUM_1;
            if (slot < session.toolbar().size()) {
                session.toolbar().select(slot);
                rebuild();
            }
            return true;
        }
        return false;
    }
}
