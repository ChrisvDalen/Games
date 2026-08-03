package com.keplersharvest.game;

import com.keplersharvest.colony.ColonyState;
import com.keplersharvest.colony.ModuleDefinition;
import com.keplersharvest.colony.RepairResult;
import com.keplersharvest.configuration.GameContent;
import com.keplersharvest.configuration.GameSettings;
import com.keplersharvest.crafting.CraftResult;
import com.keplersharvest.crafting.CraftingService;
import com.keplersharvest.dialogue.DialogueRunner;
import com.keplersharvest.farming.FarmLand;
import com.keplersharvest.inventory.Inventory;
import com.keplersharvest.inventory.ItemDefinition;
import com.keplersharvest.inventory.Toolbar;
import com.keplersharvest.mystery.CrewLogDefinition;
import com.keplersharvest.mystery.EvidenceJournal;
import com.keplersharvest.npc.Colonist;
import com.keplersharvest.npc.RelationshipBook;
import com.keplersharvest.npc.RelationshipLevel;
import com.keplersharvest.player.PlayerState;
import com.keplersharvest.quests.QuestDefinition;
import com.keplersharvest.quests.QuestLog;
import com.keplersharvest.quests.QuestStatus;
import com.keplersharvest.quests.QuestWorldView;
import com.keplersharvest.time.ClockListener;
import com.keplersharvest.time.DayPhase;
import com.keplersharvest.time.GameClock;
import com.keplersharvest.time.TimeOfDay;
import com.keplersharvest.world.GridPoint;
import com.keplersharvest.world.MapObject;
import com.keplersharvest.world.MapObjectKind;
import com.keplersharvest.world.Movement;
import com.keplersharvest.world.WorldMap;
import com.keplersharvest.world.WorldObjectState;
import com.keplersharvest.world.WorldPosition;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.random.RandomGenerator;

/**
 * The whole running game: clock, player, world state and every domain system, wired together.
 *
 * <p>Rendering and input live outside this class. Everything here can be driven from a test, which
 * is what makes the farming, repair and quest rules verifiable without opening a window.
 */
public final class GameSession implements StoryState, QuestWorldView {

    /** How many recent notices the HUD keeps on screen. */
    private static final int NOTICE_HISTORY = 6;

    private final GameContent content;
    private final GameSettings settings;
    private final GameClock clock;
    private final PlayerState player;
    private final Inventory inventory;
    private final Toolbar toolbar;
    private final ColonyState colony;
    private final RelationshipBook relationships;
    private final QuestLog questLog;
    private final EvidenceJournal journal;
    private final WorldObjectState worldObjects = new WorldObjectState();
    private final CraftingService crafting;
    private final EventBus events = new EventBus();
    private final WorldRandom random;

    private final Map<String, FarmLand> farmlands = new LinkedHashMap<>();
    private final Map<String, Colonist> colonists = new LinkedHashMap<>();
    private final Set<String> flags = new LinkedHashSet<>();
    private final Map<String, String> landmarks = new LinkedHashMap<>();
    private final Deque<String> notices = new ArrayDeque<>();

    private boolean collapsePending;
    private long seed;

    private GameSession(GameContent content, long seed) {
        this.content = Objects.requireNonNull(content, "content");
        this.settings = content.settings();
        this.seed = seed;
        this.random = new WorldRandom(seed);
        this.clock = new GameClock(settings.realSecondsPerGameMinute(), settings.wakeMinute());
        this.inventory = new Inventory(settings.inventorySize());
        this.toolbar = new Toolbar(inventory, settings.toolbarSize());
        this.colony = new ColonyState(content.modules().values());
        this.relationships = new RelationshipBook();
        this.questLog = new QuestLog(content.quests().values());
        this.journal = new EvidenceJournal(content.crewLogs(), content.evidence(), content.reveal());
        this.crafting = new CraftingService(content.recipes(), id -> content.item(id).orElse(null), colony);

        WorldMap startMap = content.maps().require(content.maps().startMapId());
        GridPoint startTile = Movement.nearestFreeTile(startMap, startMap.spawnOrDefault(content.maps().startSpawn()));
        this.player = new PlayerState(startMap.id(), WorldPosition.centreOf(startTile), settings.baseMaxEnergy());

        content.colonists().values()
                .forEach(def -> colonists.put(def.id(), new Colonist(def, clock.phase(), colony::isOnline)));

        clock.addListener(new SessionClockListener());
        events.subscribe(questLog::onEvent);
        events.subscribe(event -> settleQuests());
    }

    /** Starts a fresh game with the configured starting kit and auto-start quests. */
    public static GameSession newGame(GameContent content, long seed) {
        GameSession session = new GameSession(content, seed);
        for (GameSettings.StartingItem entry : content.settings().startingItems()) {
            session.inventory.add(content.requireItem(entry.itemId()), entry.count());
        }
        session.refreshEnergyBonus();
        session.player.energy().fill();
        session.startAutoQuests();
        session.notice("Landed at Meridian Station. The colony is quiet.");
        return session;
    }

    /** Creates an empty session for a save file to populate. */
    public static GameSession forLoading(GameContent content, long seed) {
        return new GameSession(content, seed);
    }

    // ---------------------------------------------------------------- accessors

    public GameContent content() {
        return content;
    }

    public GameSettings settings() {
        return settings;
    }

    public GameClock clock() {
        return clock;
    }

    public PlayerState player() {
        return player;
    }

    public Inventory inventory() {
        return inventory;
    }

    public Toolbar toolbar() {
        return toolbar;
    }

    public ColonyState colony() {
        return colony;
    }

    public RelationshipBook relationships() {
        return relationships;
    }

    public QuestLog questLog() {
        return questLog;
    }

    public EvidenceJournal journal() {
        return journal;
    }

    public WorldObjectState worldObjects() {
        return worldObjects;
    }

    public CraftingService crafting() {
        return crafting;
    }

    public EventBus events() {
        return events;
    }

    public RandomGenerator random() {
        return random;
    }

    /** How far along the random stream this session is, so a save can resume it exactly. */
    public long randomState() {
        return random.state();
    }

    /** Resumes the random stream from a position captured by {@link #randomState()}. */
    public void restoreRandomState(long state) {
        random.restore(state);
    }

    public long seed() {
        return seed;
    }

    public void setSeed(long seed) {
        this.seed = seed;
    }

    public WorldMap currentMap() {
        return content.maps().require(player.mapId());
    }

    public Collection<Colonist> colonists() {
        return colonists.values();
    }

    public Optional<Colonist> colonist(String id) {
        return Optional.ofNullable(colonists.get(id));
    }

    /** Colonists standing on the map the player is currently in. */
    public List<Colonist> colonistsHere() {
        return colonists.values().stream().filter(c -> c.mapId().equals(player.mapId())).toList();
    }

    public Map<String, String> landmarks() {
        return Map.copyOf(landmarks);
    }

    public Set<String> flags() {
        return Set.copyOf(flags);
    }

    /** Cultivated tiles for a map, created on first use. */
    public FarmLand farmLand(String mapId) {
        return farmlands.computeIfAbsent(mapId, id -> {
            WorldMap map = content.maps().require(id);
            return new FarmLand(id, content.crops(), map::farmable, random);
        });
    }

    public Map<String, FarmLand> farmlands() {
        return Map.copyOf(farmlands);
    }

    public List<String> notices() {
        return List.copyOf(notices);
    }

    public void notice(String text) {
        notices.addLast(text);
        while (notices.size() > NOTICE_HISTORY) {
            notices.removeFirst();
        }
    }

    public Optional<String> latestNotice() {
        return Optional.ofNullable(notices.peekLast());
    }

    // ---------------------------------------------------------------- simulation

    /** Advances time and time-driven effects. Menus pause the clock, so this becomes a no-op. */
    public void update(float deltaSeconds) {
        clock.advance(deltaSeconds);
        if (collapsePending) {
            collapsePending = false;
            collapse();
        }
    }

    /**
     * Ends the day.
     *
     * @return a summary of what changed overnight
     */
    public SleepReport sleep() {
        int dayBefore = clock.day();
        clock.sleepUntilMorning();
        player.energy().fill();
        int harvestable = countHarvestableCrops();
        notice("You slept. Day " + clock.day() + " begins.");
        return new SleepReport(dayBefore, clock.day(), player.energy().current(), harvestable);
    }

    /** Passing out: the day still ends, but energy comes back short. */
    private void collapse() {
        clock.sleepUntilMorning();
        player.energy().fill();
        player.energy().drain(settings.collapseEnergyPenalty());
        returnToBed();
        notice("You pushed too far and woke up drained.");
    }

    /**
     * Puts the player in a bed after passing out.
     *
     * <p>A bed on the map they collapsed in wins; otherwise beds are considered in map-id order, so
     * a second bed added later cannot silently change where an existing save wakes up.
     */
    private void returnToBed() {
        content.maps().maps().stream()
                .sorted(Comparator.comparing((WorldMap map) -> !map.id().equals(player.mapId()))
                        .thenComparing(WorldMap::id))
                .flatMap(map -> map.objectsOfKind(MapObjectKind.BED).stream()
                        .map(bed -> Map.entry(map, bed)))
                .findFirst()
                .ifPresent(entry -> player.moveTo(entry.getKey().id(),
                        WorldPosition.centreOf(Movement.nearestFreeTile(entry.getKey(), entry.getValue().position()))));
    }

    private int countHarvestableCrops() {
        return (int) farmlands.values().stream()
                .flatMap(land -> land.plots().stream())
                .filter(plot -> plot.cropId()
                        .flatMap(content::crop)
                        .map(crop -> !plot.withered() && crop.matureAt(plot.grownDays()))
                        .orElse(false))
                .count();
    }

    /** Moves the player to another map at a named spawn point. */
    public void travelTo(String mapId, String spawnName) {
        WorldMap target = content.maps().require(mapId);
        GridPoint tile = Movement.nearestFreeTile(target, target.spawnOrDefault(spawnName));
        player.moveTo(mapId, WorldPosition.centreOf(tile));
        notice("Entered " + target.displayName() + ".");
        discoverLandmarksUnder(tile);
    }

    /** Records any landmark the player is standing on. */
    public void discoverLandmarksUnder(GridPoint tile) {
        currentMap().objectAt(tile)
                .filter(object -> object.kind() == MapObjectKind.LANDMARK)
                .ifPresent(object -> discoverLandmark(object.id(), object.property("title", object.id())));
    }

    public void discoverLandmark(String landmarkId, String displayName) {
        if (landmarks.putIfAbsent(landmarkId, displayName) == null) {
            notice("Discovered: " + displayName);
            publish(new GameEvent.LandmarkDiscovered(landmarkId, displayName));
        }
    }

    // ---------------------------------------------------------------- player actions

    /**
     * Greets a colonist and opens their conversation.
     *
     * <p>The first greeting of a day is worth relationship points; later ones are free but do not
     * pay again.
     */
    public Optional<DialogueRunner> talkTo(String colonistId) {
        Colonist colonist = colonists.get(colonistId);
        if (colonist == null) {
            return Optional.empty();
        }
        if (!colonist.greetedToday(clock.day())) {
            colonist.markGreeted(clock.day());
            changeRelationship(colonistId, settings.dailyGreetingPoints());
        }
        publish(new GameEvent.ColonistGreeted(colonistId));
        return content.dialogue(colonist.definition().dialogueId())
                .flatMap(tree -> DialogueRunner.start(tree, this));
    }

    /** Attempts one repair stage on a module, spending energy and materials. */
    public RepairResult repair(String moduleId) {
        int cost = settings.energyCost("repair");
        if (player.energy().tooTiredFor(cost)) {
            notice("Too tired to work on that.");
            return new RepairResult.MissingMaterials(moduleId, Map.of());
        }
        RepairResult result = colony.repairNextStage(moduleId, inventory);
        if (result instanceof RepairResult.StageCompleted completed) {
            player.energy().spend(cost);
            ModuleDefinition definition = content.module(moduleId).orElseThrow();
            if (completed.moduleOnline()) {
                notice(definition.name() + " is back online. " + definition.benefitText());
                definition.grantsEvidence().forEach(this::grantEvidence);
                refreshEnergyBonus();
                refreshColonistSchedules();
            } else {
                notice("Stage complete: " + completed.stageName() + ".");
            }
            publish(new GameEvent.ModuleStageRepaired(moduleId, completed.stageIndex(), completed.moduleOnline()));
        } else if (result instanceof RepairResult.MissingMaterials missing) {
            notice("Still needed: " + describeItems(missing.missing()));
        } else if (result instanceof RepairResult.AlreadyOnline) {
            notice("That module is already running.");
        }
        return result;
    }

    public CraftResult craft(String recipeId, String stationId) {
        int cost = settings.energyCost("craft");
        if (player.energy().tooTiredFor(cost)) {
            notice("Too tired to work the bench.");
            return new CraftResult.MissingIngredients(recipeId, Map.of());
        }
        CraftResult result = crafting.craft(recipeId, stationId, inventory);
        switch (result) {
            case CraftResult.Crafted crafted -> {
                player.energy().spend(cost);
                notice("Crafted " + crafted.amount() + " x " + content.itemName(crafted.outputItemId()) + ".");
                publish(new GameEvent.ItemCrafted(crafted.outputItemId(), crafted.amount()));
            }
            case CraftResult.MissingIngredients missing ->
                    notice("Missing: " + describeItems(missing.missing()));
            case CraftResult.NoRoom ignored -> notice("No room for that.");
            case CraftResult.Locked locked -> notice("Needs "
                    + content.module(locked.requiredModuleId()).map(ModuleDefinition::name).orElse("a module")
                    + " online.");
            case CraftResult.UnknownRecipe ignored -> notice("Unknown recipe.");
            case CraftResult.WrongStation ignored -> notice("Wrong bench for that recipe.");
        }
        return result;
    }

    /** Files a crew log and everything it implies. */
    public Optional<CrewLogDefinition> recoverCrewLog(String logId) {
        Optional<CrewLogDefinition> definition = content.crewLog(logId);
        if (definition.isEmpty() || journal.hasLog(logId)) {
            return Optional.empty();
        }
        journal.recordLog(logId);
        notice("Recovered crew log: " + definition.get().title());
        publish(new GameEvent.CrewLogFound(logId));
        checkReveal();
        return definition;
    }

    /** True once the player has enough to draw the conclusion but has not yet read it. */
    public boolean revealReady() {
        return journal.revealAvailable() && !journal.revealSeen();
    }

    private void checkReveal() {
        if (journal.revealAvailable() && !journal.revealSeen()) {
            notice("The pieces fit together. Open the journal.");
            publish(new GameEvent.RevealUnlocked(journal.reveal().id()));
        }
    }

    // ---------------------------------------------------------------- shared helpers

    public String describeItems(Map<String, Integer> amounts) {
        return amounts.entrySet().stream()
                .map(entry -> entry.getValue() + " x " + content.itemName(entry.getKey()))
                .reduce((a, b) -> a + ", " + b)
                .orElse("nothing");
    }

    public void publish(GameEvent event) {
        events.publish(event);
    }

    /** Starts any quest whose prerequisites are now satisfied and that starts on its own. */
    public void startAutoQuests() {
        for (QuestDefinition started : questLog.startAvailableAutoQuests()) {
            notice("New task: " + started.title());
            events.publish(new GameEvent.QuestStarted(started.id()));
        }
    }

    /** Completes finished quests and pays their rewards. */
    private void settleQuests() {
        List<QuestDefinition> finished = questLog.completeFinishedQuests(this);
        for (QuestDefinition quest : finished) {
            notice("Completed: " + quest.title());
            quest.completionText().ifPresent(this::notice);
            applyReward(quest);
            events.publish(new GameEvent.QuestCompleted(quest.id()));
        }
        if (!finished.isEmpty()) {
            startAutoQuests();
        }
    }

    private void applyReward(QuestDefinition quest) {
        quest.reward().items().forEach(this::giveItem);
        quest.reward().relationship().forEach(this::changeRelationship);
        quest.reward().evidence().forEach(this::grantEvidence);
    }

    private void refreshEnergyBonus() {
        player.energy().setBonusMax(colony.maxEnergyBonus());
    }

    private void refreshColonistSchedules() {
        colonists.values().forEach(c -> c.applySchedule(clock.phase(), colony::isOnline));
    }

    // ---------------------------------------------------------------- StoryState

    @Override
    public int day() {
        return clock.day();
    }

    @Override
    public int relationshipPoints(String colonistId) {
        return relationships.points(colonistId);
    }

    @Override
    public boolean questActive(String questId) {
        return questLog.isActive(questId);
    }

    @Override
    public boolean questCompleted(String questId) {
        return questLog.isCompleted(questId);
    }

    @Override
    public boolean questAvailable(String questId) {
        return questLog.available(questId);
    }

    @Override
    public boolean questNotStarted(String questId) {
        return questLog.status(questId) == QuestStatus.NOT_STARTED;
    }

    @Override
    public boolean moduleOnline(String moduleId) {
        return colony.isOnline(moduleId);
    }

    @Override
    public boolean crewLogFound(String logId) {
        return journal.hasLog(logId);
    }

    @Override
    public int crewLogCount() {
        return journal.foundLogCount();
    }

    @Override
    public boolean evidenceKnown(String evidenceId) {
        return journal.hasEvidence(evidenceId);
    }

    @Override
    public boolean flagSet(String flag) {
        return flags.contains(flag);
    }

    @Override
    public void changeRelationship(String colonistId, int delta) {
        Optional<RelationshipLevel> promoted = relationships.add(colonistId, delta);
        promoted.ifPresent(level -> notice(content.colonist(colonistId)
                .map(c -> c.name() + " now considers you a " + level.label().toLowerCase(Locale.ROOT) + ".")
                .orElse("Relationship improved.")));
        publish(new GameEvent.RelationshipChanged(colonistId, relationships.points(colonistId)));
    }

    @Override
    public void startQuest(String questId) {
        if (questLog.start(questId)) {
            content.quest(questId).ifPresent(quest -> notice("New task: " + quest.title()));
            publish(new GameEvent.QuestStarted(questId));
        }
    }

    @Override
    public void giveItem(String itemId, int count) {
        Optional<ItemDefinition> item = content.item(itemId);
        if (item.isEmpty()) {
            return;
        }
        int leftover = inventory.add(item.get(), count);
        int received = count - leftover;
        if (received > 0) {
            notice("Received " + received + " x " + item.get().name() + ".");
            publish(new GameEvent.ItemGathered(itemId, received));
        }
        if (leftover > 0) {
            notice("No room for " + leftover + " x " + item.get().name() + ".");
        }
    }

    @Override
    public void setFlag(String flag) {
        flags.add(flag);
    }

    @Override
    public void clearFlag(String flag) {
        flags.remove(flag);
    }

    @Override
    public void grantEvidence(String evidenceId) {
        if (journal.recordEvidence(evidenceId)) {
            notice(Optional.ofNullable(content.evidence().get(evidenceId))
                    .map(definition -> "Journal updated: " + definition.title() + ".")
                    .orElse("Journal updated."));
            publish(new GameEvent.EvidenceRecorded(evidenceId));
            checkReveal();
        }
    }

    // ---------------------------------------------------------------- QuestWorldView

    @Override
    public int itemCount(String itemId) {
        return inventory.count(itemId);
    }

    @Override
    public boolean landmarkDiscovered(String landmarkId) {
        return landmarks.containsKey(landmarkId);
    }

    @Override
    public int crewLogsFound() {
        return journal.foundLogCount();
    }

    // ---------------------------------------------------------------- save support

    /** Re-applies derived state after a save has been loaded into this session. */
    public void afterLoad() {
        refreshEnergyBonus();
        refreshColonistSchedules();
        settleQuests();
    }

    public void restoreFlags(Collection<String> saved) {
        flags.clear();
        flags.addAll(saved);
    }

    public void restoreLandmarks(Map<String, String> saved) {
        landmarks.clear();
        landmarks.putAll(saved);
    }

    /** Recomputes quest completion, e.g. after loading or after inventory changes. */
    public void refreshQuests() {
        settleQuests();
    }

    // ---------------------------------------------------------------- clock reactions

    /** Overnight growth, colonist movement and the effects of staying up too late. */
    private final class SessionClockListener implements ClockListener {

        @Override
        public void onDayStarted(int day) {
            farmlands.values().forEach(FarmLand::advanceDay);
            worldObjects.pruneRespawned(day, 30);
            colonists.values().forEach(c -> c.applySchedule(DayPhase.at(settings.wakeMinute()), colony::isOnline));
            events.publish(new GameEvent.DayStarted(day));
        }

        @Override
        public void onPhaseChanged(DayPhase phase, TimeOfDay now) {
            refreshColonistSchedules();
            if (phase == DayPhase.NIGHT) {
                notice("Night falls. Sleep soon.");
            }
        }

        @Override
        public void onMinute(TimeOfDay now) {
            if (now.minuteOfDay() == settings.collapseMinute()) {
                collapsePending = true;
                return;
            }
            if (now.minute() != 0) {
                return;
            }
            if (now.phase() == DayPhase.NIGHT) {
                player.energy().drain(settings.nightEnergyDrainPerHour());
                if (hazardousHere()) {
                    player.energy().drain(settings.hazardEnergyDrainPerHour());
                    notice("Spore bloom stings your lungs out here.");
                }
            }
        }

        /** Some maps are dangerous after dark; the map file says so. */
        private boolean hazardousHere() {
            return declaresNightHazard(currentMap().objects());
        }
    }

    /**
     * True when any object marks its map as dangerous after dark.
     *
     * <p>Deliberately blind to object kind: a hazard on a plain marker should sting just as much as
     * one on a landmark. It is also deliberately scoped to the objects it is handed, so that adding
     * a second hazardous map cannot make a safe one dangerous.
     */
    static boolean declaresNightHazard(Collection<MapObject> objects) {
        return objects.stream().anyMatch(object -> object.boolProperty("nightHazard", false));
    }

    /**
     * Summary handed back from {@link #sleep()} so the UI can show what changed.
     *
     * <p>Collapsing is not reported here: it happens inside the clock listener with no caller to
     * hand a report to, so {@link #collapse()} posts its own notice instead.
     */
    public record SleepReport(int previousDay, int newDay, int energy, int harvestableCrops) {
    }

    /** Convenience for tests and tooling: every map object of a kind across all maps. */
    public List<MapObject> allObjectsOfKind(MapObjectKind kind) {
        List<MapObject> objects = new ArrayList<>();
        content.maps().maps().forEach(map -> objects.addAll(map.objectsOfKind(kind)));
        return List.copyOf(objects);
    }
}
