package com.keplersharvest.configuration;

import com.badlogic.gdx.utils.JsonValue;
import com.keplersharvest.colony.BenefitKind;
import com.keplersharvest.colony.ModuleDefinition;
import com.keplersharvest.colony.RepairStage;
import com.keplersharvest.crafting.RecipeDefinition;
import com.keplersharvest.dialogue.DialogueLoader;
import com.keplersharvest.dialogue.DialogueTree;
import com.keplersharvest.farming.CropDefinition;
import com.keplersharvest.inventory.ItemCategory;
import com.keplersharvest.inventory.ItemDefinition;
import com.keplersharvest.inventory.ToolKind;
import com.keplersharvest.mystery.CrewLogDefinition;
import com.keplersharvest.mystery.EvidenceDefinition;
import com.keplersharvest.mystery.RevealDefinition;
import com.keplersharvest.npc.ColonistDefinition;
import com.keplersharvest.npc.ScheduleEntry;
import com.keplersharvest.quests.Objective;
import com.keplersharvest.quests.QuestDefinition;
import com.keplersharvest.quests.QuestReward;
import com.keplersharvest.time.DayPhase;
import com.keplersharvest.world.GridPoint;
import com.keplersharvest.world.MapObject;
import com.keplersharvest.world.MapObjectKind;
import com.keplersharvest.world.MapRegistry;
import com.keplersharvest.world.WorldMap;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Reads every content file and checks that the pieces refer to each other correctly.
 *
 * <p>Cross-validation runs at load time on purpose: a seed that names a missing crop, or a quest
 * that asks for an item nobody defines, fails at start-up with a precise message instead of
 * surfacing as a confusing no-op an hour into a playthrough.
 */
public final class ContentLoader {

    private final ResourceReader reader;

    public ContentLoader(ResourceReader reader) {
        this.reader = Objects.requireNonNull(reader, "reader");
    }

    public static GameContent loadDefault(ResourceReader reader) {
        return new ContentLoader(reader).load();
    }

    public GameContent load() {
        GameSettings settings = readSettings("config/game.json");
        Map<String, ItemDefinition> items = readItems("config/items.json");
        Map<String, CropDefinition> crops = readCrops("config/crops.json");
        Map<String, RecipeDefinition> recipes = readRecipes("config/recipes.json");
        Map<String, ModuleDefinition> modules = readModules("config/modules.json");
        Map<String, ColonistDefinition> colonists = readColonists("config/colonists.json");
        Map<String, QuestDefinition> quests = readQuests("config/quests.json");
        JsonValue mysteryRoot = Json5.parse(reader.readText("config/mystery.json"), "config/mystery.json");
        Map<String, CrewLogDefinition> crewLogs = readCrewLogs(mysteryRoot);
        Map<String, EvidenceDefinition> evidence = readEvidence(mysteryRoot);
        RevealDefinition reveal = readReveal(mysteryRoot);
        Map<String, DialogueTree> dialogues = readDialogues(colonists);
        MapRegistry maps = MapRegistry.load(reader);

        GameContent content = new GameContent(settings, items, crops, recipes, modules, colonists,
                quests, crewLogs, evidence, dialogues, reveal, maps);
        new ContentValidator(content).validate();
        return content;
    }

    private GameSettings readSettings(String path) {
        JsonValue root = Json5.parse(reader.readText(path), path);
        List<GameSettings.StartingItem> startingItems = new ArrayList<>();
        for (JsonValue entry : Json5.array(root, "startingItems")) {
            startingItems.add(new GameSettings.StartingItem(
                    Json5.requireString(entry, "item", path), Json5.integer(entry, "count", 1)));
        }
        return new GameSettings(
                Json5.decimal(root, "realSecondsPerGameMinute", 0.6f),
                Json5.integer(root, "wakeMinute", 6 * 60),
                Json5.integer(root, "collapseMinute", 2 * 60),
                Json5.integer(root, "baseMaxEnergy", 100),
                Json5.integer(root, "collapseEnergyPenalty", 35),
                Json5.integer(root, "inventorySize", 24),
                Json5.integer(root, "toolbarSize", 8),
                Json5.decimal(root, "playerTilesPerSecond", 4.2f),
                Json5.integer(root, "nightEnergyDrainPerHour", 3),
                Json5.integer(root, "hazardEnergyDrainPerHour", 6),
                Json5.integer(root, "dailyGreetingPoints", 6),
                Json5.intMap(root, "energyCosts"),
                startingItems);
    }

    private Map<String, ItemDefinition> readItems(String path) {
        JsonValue root = Json5.parse(reader.readText(path), path);
        Map<String, ItemDefinition> items = new LinkedHashMap<>();
        for (JsonValue entry : Json5.array(root, "items")) {
            String id = Json5.requireString(entry, "id", path);
            ItemCategory category = Json5.requireEnum(ItemCategory.class, entry, "category", path);
            Optional<ToolKind> toolKind = Json5.optionalString(entry, "tool")
                    .map(raw -> ToolKind.valueOf(raw.toUpperCase(java.util.Locale.ROOT)));
            ItemDefinition item = new ItemDefinition(
                    id,
                    Json5.requireString(entry, "name", path),
                    Json5.string(entry, "description", ""),
                    category,
                    Json5.integer(entry, "maxStack", category == ItemCategory.TOOL ? 1 : 99),
                    Json5.string(entry, "colour", "cccccc"),
                    toolKind,
                    Json5.optionalString(entry, "plants"));
            if (items.put(id, item) != null) {
                throw new ConfigurationException("Duplicate item id '" + id + "' in " + path);
            }
        }
        return items;
    }

    private Map<String, CropDefinition> readCrops(String path) {
        JsonValue root = Json5.parse(reader.readText(path), path);
        Map<String, CropDefinition> crops = new LinkedHashMap<>();
        for (JsonValue entry : Json5.array(root, "crops")) {
            String id = Json5.requireString(entry, "id", path);
            CropDefinition crop = new CropDefinition(
                    id,
                    Json5.requireString(entry, "name", path),
                    Json5.string(entry, "description", ""),
                    Json5.requireString(entry, "seedItem", path),
                    Json5.requireString(entry, "produceItem", path),
                    Json5.integer(entry, "produceMin", 1),
                    Json5.integer(entry, "produceMax", 1),
                    Json5.intList(entry, "stageDays"),
                    Json5.bool(entry, "regrows", false),
                    Json5.integer(entry, "regrowDays", 0),
                    Json5.bool(entry, "requiresWater", true),
                    Json5.integer(entry, "witherAfterDryDays", 0),
                    Json5.string(entry, "colour", "5fbf6a"));
            if (crops.put(id, crop) != null) {
                throw new ConfigurationException("Duplicate crop id '" + id + "' in " + path);
            }
        }
        return crops;
    }

    private Map<String, RecipeDefinition> readRecipes(String path) {
        JsonValue root = Json5.parse(reader.readText(path), path);
        Map<String, RecipeDefinition> recipes = new LinkedHashMap<>();
        for (JsonValue entry : Json5.array(root, "recipes")) {
            String id = Json5.requireString(entry, "id", path);
            RecipeDefinition recipe = new RecipeDefinition(
                    id,
                    Json5.requireString(entry, "name", path),
                    Json5.string(entry, "description", ""),
                    Json5.requireString(entry, "station", path),
                    Json5.intMap(entry, "inputs"),
                    Json5.requireString(entry, "outputItem", path),
                    Json5.integer(entry, "outputCount", 1),
                    Json5.optionalString(entry, "requiresModule"));
            if (recipes.put(id, recipe) != null) {
                throw new ConfigurationException("Duplicate recipe id '" + id + "' in " + path);
            }
        }
        return recipes;
    }

    private Map<String, ModuleDefinition> readModules(String path) {
        JsonValue root = Json5.parse(reader.readText(path), path);
        Map<String, ModuleDefinition> modules = new LinkedHashMap<>();
        for (JsonValue entry : Json5.array(root, "modules")) {
            String id = Json5.requireString(entry, "id", path);
            List<RepairStage> stages = new ArrayList<>();
            int index = 0;
            for (JsonValue stage : Json5.array(entry, "stages")) {
                stages.add(new RepairStage(
                        index++,
                        Json5.requireString(stage, "name", path),
                        Json5.string(stage, "description", ""),
                        Json5.intMap(stage, "materials")));
            }
            ModuleDefinition module = new ModuleDefinition(
                    id,
                    Json5.requireString(entry, "name", path),
                    Json5.string(entry, "description", ""),
                    stages,
                    Json5.requireEnum(BenefitKind.class, entry, "benefitKind", path),
                    Json5.integer(entry, "benefitAmount", 0),
                    Json5.string(entry, "benefitText", ""),
                    Json5.stringList(entry, "grantsEvidence"),
                    Json5.string(entry, "colour", "8899aa"));
            if (modules.put(id, module) != null) {
                throw new ConfigurationException("Duplicate module id '" + id + "' in " + path);
            }
        }
        return modules;
    }

    private Map<String, ColonistDefinition> readColonists(String path) {
        JsonValue root = Json5.parse(reader.readText(path), path);
        Map<String, ColonistDefinition> colonists = new LinkedHashMap<>();
        for (JsonValue entry : Json5.array(root, "colonists")) {
            String id = Json5.requireString(entry, "id", path);
            List<ScheduleEntry> schedule = new ArrayList<>();
            for (JsonValue slot : Json5.array(entry, "schedule")) {
                schedule.add(new ScheduleEntry(
                        Json5.requireEnum(DayPhase.class, slot, "phase", path),
                        Json5.requireString(slot, "map", path),
                        new GridPoint(Json5.requireInt(slot, "x", path), Json5.requireInt(slot, "y", path)),
                        Json5.optionalString(slot, "requiresModule")));
            }
            ColonistDefinition colonist = new ColonistDefinition(
                    id,
                    Json5.requireString(entry, "name", path),
                    Json5.requireString(entry, "role", path),
                    Json5.string(entry, "personality", ""),
                    Json5.string(entry, "dialogue", id),
                    Json5.optionalString(entry, "personalQuest"),
                    schedule,
                    Json5.string(entry, "colour", "d0a060"));
            if (colonists.put(id, colonist) != null) {
                throw new ConfigurationException("Duplicate colonist id '" + id + "' in " + path);
            }
        }
        return colonists;
    }

    private Map<String, QuestDefinition> readQuests(String path) {
        JsonValue root = Json5.parse(reader.readText(path), path);
        Map<String, QuestDefinition> quests = new LinkedHashMap<>();
        for (JsonValue entry : Json5.array(root, "quests")) {
            String id = Json5.requireString(entry, "id", path);
            List<Objective> objectives = new ArrayList<>();
            for (JsonValue objective : Json5.array(entry, "objectives")) {
                objectives.add(readObjective(objective, path));
            }
            JsonValue rewardJson = entry.get("reward");
            QuestReward reward = rewardJson == null ? QuestReward.NONE : new QuestReward(
                    Json5.intMap(rewardJson, "items"),
                    Json5.intMap(rewardJson, "relationship"),
                    Json5.stringList(rewardJson, "evidence"));
            QuestDefinition quest = new QuestDefinition(
                    id,
                    Json5.requireString(entry, "title", path),
                    Json5.string(entry, "summary", ""),
                    objectives,
                    Json5.stringList(entry, "prerequisites"),
                    reward,
                    Json5.bool(entry, "autoStart", false),
                    Json5.optionalString(entry, "giver"),
                    Json5.optionalString(entry, "completionText"));
            if (quests.put(id, quest) != null) {
                throw new ConfigurationException("Duplicate quest id '" + id + "' in " + path);
            }
        }
        return quests;
    }

    private Objective readObjective(JsonValue json, String path) {
        String type = Json5.requireString(json, "type", path);
        String description = Json5.string(json, "description", "");
        int count = Json5.integer(json, "count", 1);
        return switch (type) {
            case "collectItem" -> new Objective.CollectItem(
                    Json5.requireString(json, "item", path), count, description);
            case "repairModule" -> new Objective.RepairModule(
                    Json5.requireString(json, "module", path), description);
            case "talkTo" -> new Objective.TalkTo(
                    Json5.requireString(json, "colonist", path), description);
            case "discoverLandmark" -> new Objective.DiscoverLandmark(
                    Json5.requireString(json, "landmark", path), description);
            case "findCrewLog" -> new Objective.FindCrewLog(
                    Json5.requireString(json, "log", path), description);
            case "findAnyCrewLogs" -> new Objective.FindAnyCrewLogs(count, description);
            case "craftItem" -> new Objective.CraftItem(
                    Json5.requireString(json, "item", path), count, description);
            case "harvestCrop" -> new Objective.HarvestCrop(
                    Json5.requireString(json, "crop", path), count, description);
            case "reachRelationship" -> new Objective.ReachRelationship(
                    Json5.requireString(json, "colonist", path), Json5.integer(json, "points", 45), description);
            default -> throw new ConfigurationException("Unknown objective type '" + type + "' in " + path);
        };
    }

    private Map<String, CrewLogDefinition> readCrewLogs(JsonValue root) {
        Map<String, CrewLogDefinition> logs = new LinkedHashMap<>();
        int order = 0;
        for (JsonValue entry : Json5.array(root, "crewLogs")) {
            String id = Json5.requireString(entry, "id", "config/mystery.json");
            CrewLogDefinition log = new CrewLogDefinition(
                    id,
                    Json5.requireString(entry, "title", "config/mystery.json"),
                    Json5.requireString(entry, "author", "config/mystery.json"),
                    Json5.string(entry, "stardate", ""),
                    Json5.stringList(entry, "body"),
                    Json5.integer(entry, "order", order++),
                    Json5.stringList(entry, "grantsEvidence"));
            if (logs.put(id, log) != null) {
                throw new ConfigurationException("Duplicate crew log id '" + id + "'");
            }
        }
        return logs;
    }

    private Map<String, EvidenceDefinition> readEvidence(JsonValue root) {
        Map<String, EvidenceDefinition> evidence = new LinkedHashMap<>();
        for (JsonValue entry : Json5.array(root, "evidence")) {
            String id = Json5.requireString(entry, "id", "config/mystery.json");
            EvidenceDefinition definition = new EvidenceDefinition(
                    id,
                    Json5.requireString(entry, "title", "config/mystery.json"),
                    Json5.requireString(entry, "text", "config/mystery.json"),
                    Json5.string(entry, "source", "Observation"));
            if (evidence.put(id, definition) != null) {
                throw new ConfigurationException("Duplicate evidence id '" + id + "'");
            }
        }
        return evidence;
    }

    private RevealDefinition readReveal(JsonValue root) {
        JsonValue entry = Json5.requireChild(root, "reveal", "config/mystery.json");
        return new RevealDefinition(
                Json5.requireString(entry, "id", "config/mystery.json"),
                Json5.requireString(entry, "title", "config/mystery.json"),
                Json5.stringList(entry, "body"),
                Json5.integer(entry, "requiredLogs", 5),
                Json5.stringList(entry, "requiredEvidence"));
    }

    private Map<String, DialogueTree> readDialogues(Map<String, ColonistDefinition> colonists) {
        DialogueLoader loader = new DialogueLoader(reader);
        Map<String, DialogueTree> dialogues = new LinkedHashMap<>();
        for (ColonistDefinition colonist : colonists.values()) {
            String dialogueId = colonist.dialogueId();
            if (dialogues.containsKey(dialogueId)) {
                continue;
            }
            String path = "config/dialogue/" + dialogueId + ".json";
            if (!reader.exists(path)) {
                throw new ConfigurationException(
                        "Colonist " + colonist.id() + " references missing dialogue file " + path);
            }
            dialogues.put(dialogueId, loader.load(dialogueId, path));
        }
        return dialogues;
    }

    /** Checks references between content files once everything is parsed. */
    private record ContentValidator(GameContent content) {

        void validate() {
            validateItems();
            validateCrops();
            validateRecipes();
            validateModules();
            validateQuests();
            validateColonists();
            validateMystery();
            validateMaps();
            validateStartingKit();
        }

        private void validateItems() {
            content.items().values().stream()
                    .filter(item -> item.plantsCropId().isPresent())
                    .forEach(item -> require(content.crops().containsKey(item.plantsCropId().orElseThrow()),
                            "Seed " + item.id() + " plants unknown crop " + item.plantsCropId().orElseThrow()));
        }

        private void validateCrops() {
            content.crops().values().forEach(crop -> {
                require(content.items().containsKey(crop.seedItemId()),
                        "Crop " + crop.id() + " names unknown seed item " + crop.seedItemId());
                require(content.items().containsKey(crop.produceItemId()),
                        "Crop " + crop.id() + " names unknown produce item " + crop.produceItemId());
                require(crop.regrowDays() <= crop.daysToMaturity(),
                        "Crop " + crop.id() + " regrows faster than it grows");
            });
        }

        private void validateRecipes() {
            content.recipes().values().forEach(recipe -> {
                recipe.inputs().keySet().forEach(itemId -> require(content.items().containsKey(itemId),
                        "Recipe " + recipe.id() + " needs unknown item " + itemId));
                require(content.items().containsKey(recipe.outputItemId()),
                        "Recipe " + recipe.id() + " produces unknown item " + recipe.outputItemId());
                recipe.requiresModule().ifPresent(moduleId -> require(content.modules().containsKey(moduleId),
                        "Recipe " + recipe.id() + " is gated on unknown module " + moduleId));
            });
        }

        private void validateModules() {
            content.modules().values().forEach(module -> {
                module.stages().forEach(stage -> stage.materials().keySet()
                        .forEach(itemId -> require(content.items().containsKey(itemId),
                                "Module " + module.id() + " stage '" + stage.name()
                                        + "' needs unknown item " + itemId)));
                module.grantsEvidence().forEach(id -> require(content.evidence().containsKey(id),
                        "Module " + module.id() + " grants unknown evidence " + id));
            });
        }

        private void validateQuests() {
            content.quests().values().forEach(quest -> {
                quest.prerequisites().forEach(id -> require(content.quests().containsKey(id),
                        "Quest " + quest.id() + " requires unknown quest " + id));
                quest.reward().items().keySet().forEach(id -> require(content.items().containsKey(id),
                        "Quest " + quest.id() + " rewards unknown item " + id));
                quest.reward().evidence().forEach(id -> require(content.evidence().containsKey(id),
                        "Quest " + quest.id() + " rewards unknown evidence " + id));
                quest.reward().relationship().keySet().forEach(id -> require(content.colonists().containsKey(id),
                        "Quest " + quest.id() + " rewards unknown colonist " + id));
                quest.objectives().forEach(objective -> validateObjective(quest.id(), objective));
            });
        }

        private void validateObjective(String questId, Objective objective) {
            switch (objective) {
                case Objective.CollectItem o -> require(content.items().containsKey(o.itemId()),
                        "Quest " + questId + " collects unknown item " + o.itemId());
                case Objective.CraftItem o -> require(content.items().containsKey(o.itemId()),
                        "Quest " + questId + " crafts unknown item " + o.itemId());
                case Objective.HarvestCrop o -> require(content.crops().containsKey(o.cropId()),
                        "Quest " + questId + " harvests unknown crop " + o.cropId());
                case Objective.RepairModule o -> require(content.modules().containsKey(o.moduleId()),
                        "Quest " + questId + " repairs unknown module " + o.moduleId());
                case Objective.TalkTo o -> require(content.colonists().containsKey(o.colonistId()),
                        "Quest " + questId + " talks to unknown colonist " + o.colonistId());
                case Objective.ReachRelationship o -> require(content.colonists().containsKey(o.colonistId()),
                        "Quest " + questId + " befriends unknown colonist " + o.colonistId());
                case Objective.FindCrewLog o -> require(content.crewLogs().containsKey(o.logId()),
                        "Quest " + questId + " finds unknown crew log " + o.logId());
                case Objective.DiscoverLandmark o -> require(landmarkExists(o.landmarkId()),
                        "Quest " + questId + " discovers unknown landmark " + o.landmarkId());
                case Objective.FindAnyCrewLogs o -> require(o.count() <= content.crewLogs().size(),
                        "Quest " + questId + " asks for more crew logs than exist");
            }
        }

        private boolean landmarkExists(String landmarkId) {
            return content.maps().maps().stream()
                    .flatMap(map -> map.objectsOfKind(MapObjectKind.LANDMARK).stream())
                    .anyMatch(object -> object.id().equals(landmarkId));
        }

        private void validateColonists() {
            content.colonists().values().forEach(colonist -> {
                require(content.dialogues().containsKey(colonist.dialogueId()),
                        "Colonist " + colonist.id() + " has no dialogue tree");
                colonist.personalQuestId().ifPresent(id -> require(content.quests().containsKey(id),
                        "Colonist " + colonist.id() + " owns unknown quest " + id));
                colonist.schedule().forEach(entry -> {
                    WorldMap map = content.maps().find(entry.mapId()).orElseThrow(() ->
                            new ConfigurationException("Colonist " + colonist.id()
                                    + " is scheduled on unknown map " + entry.mapId()));
                    require(map.inBounds(entry.position()), "Colonist " + colonist.id()
                            + " is scheduled outside map " + entry.mapId() + " at " + entry.position());
                    entry.requiresModule().ifPresent(moduleId -> require(content.modules().containsKey(moduleId),
                            "Colonist " + colonist.id() + " waits on unknown module " + moduleId));
                });
            });
        }

        private void validateMystery() {
            content.crewLogs().values().forEach(log -> log.grantsEvidence()
                    .forEach(id -> require(content.evidence().containsKey(id),
                            "Crew log " + log.id() + " grants unknown evidence " + id)));
            content.reveal().requiredEvidence().forEach(id -> require(content.evidence().containsKey(id),
                    "The reveal requires unknown evidence " + id));
            require(content.reveal().requiredLogs() <= content.crewLogs().size(),
                    "The reveal requires more crew logs than exist");
        }

        private void validateMaps() {
            for (WorldMap map : content.maps().maps()) {
                for (MapObject object : map.objects()) {
                    switch (object.kind()) {
                        case MODULE -> require(content.modules().containsKey(object.requireProperty("module")),
                                "Map " + map.id() + " places unknown module " + object.property("module", "?"));
                        case CREW_LOG -> require(content.crewLogs().containsKey(object.requireProperty("log")),
                                "Map " + map.id() + " places unknown crew log " + object.property("log", "?"));
                        case RESOURCE_NODE -> require(content.items().containsKey(object.requireProperty("item")),
                                "Map " + map.id() + " places a node yielding unknown item "
                                        + object.property("item", "?"));
                        case CRAFTING_STATION -> require(!content.recipes().isEmpty(),
                                "Map " + map.id() + " places a crafting station but no recipes exist");
                        default -> {
                        }
                    }
                }
            }
        }

        private void validateStartingKit() {
            content.settings().startingItems().forEach(entry ->
                    require(content.items().containsKey(entry.itemId()),
                            "Starting kit contains unknown item " + entry.itemId()));
        }

        private void require(boolean condition, String message) {
            if (!condition) {
                throw new ConfigurationException(message);
            }
        }
    }
}
