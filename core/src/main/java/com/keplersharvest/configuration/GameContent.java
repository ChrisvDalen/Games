package com.keplersharvest.configuration;

import com.keplersharvest.colony.ModuleDefinition;
import com.keplersharvest.crafting.RecipeDefinition;
import com.keplersharvest.dialogue.DialogueTree;
import com.keplersharvest.farming.CropDefinition;
import com.keplersharvest.inventory.ItemDefinition;
import com.keplersharvest.mystery.CrewLogDefinition;
import com.keplersharvest.mystery.EvidenceDefinition;
import com.keplersharvest.mystery.RevealDefinition;
import com.keplersharvest.npc.ColonistDefinition;
import com.keplersharvest.quests.QuestDefinition;
import com.keplersharvest.world.MapRegistry;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Every piece of authored content, loaded once at start-up and then treated as read-only.
 *
 * <p>Systems take the registries they need rather than this whole object where practical; it exists
 * so that loading and cross-validation happen in exactly one place.
 */
public record GameContent(
        GameSettings settings,
        Map<String, ItemDefinition> items,
        Map<String, CropDefinition> crops,
        Map<String, RecipeDefinition> recipes,
        Map<String, ModuleDefinition> modules,
        Map<String, ColonistDefinition> colonists,
        Map<String, QuestDefinition> quests,
        Map<String, CrewLogDefinition> crewLogs,
        Map<String, EvidenceDefinition> evidence,
        Map<String, DialogueTree> dialogues,
        RevealDefinition reveal,
        MapRegistry maps) {

    public GameContent {
        Objects.requireNonNull(settings, "settings");
        items = Map.copyOf(items);
        crops = Map.copyOf(crops);
        recipes = Map.copyOf(recipes);
        modules = Map.copyOf(modules);
        colonists = Map.copyOf(colonists);
        quests = Map.copyOf(quests);
        crewLogs = Map.copyOf(crewLogs);
        evidence = Map.copyOf(evidence);
        dialogues = Map.copyOf(dialogues);
        Objects.requireNonNull(reveal, "reveal");
        Objects.requireNonNull(maps, "maps");
    }

    public Optional<ItemDefinition> item(String id) {
        return Optional.ofNullable(items.get(id));
    }

    public ItemDefinition requireItem(String id) {
        return item(id).orElseThrow(() -> new ConfigurationException("Unknown item: " + id));
    }

    public Optional<CropDefinition> crop(String id) {
        return Optional.ofNullable(crops.get(id));
    }

    public Optional<ModuleDefinition> module(String id) {
        return Optional.ofNullable(modules.get(id));
    }

    public Optional<ColonistDefinition> colonist(String id) {
        return Optional.ofNullable(colonists.get(id));
    }

    public Optional<QuestDefinition> quest(String id) {
        return Optional.ofNullable(quests.get(id));
    }

    public Optional<CrewLogDefinition> crewLog(String id) {
        return Optional.ofNullable(crewLogs.get(id));
    }

    public Optional<DialogueTree> dialogue(String id) {
        return Optional.ofNullable(dialogues.get(id));
    }

    /** Display name for an item id, falling back to a readable form of the id itself. */
    public String itemName(String id) {
        return item(id).map(ItemDefinition::name).orElseGet(() -> id.replace('_', ' '));
    }
}
