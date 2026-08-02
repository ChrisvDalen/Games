package com.keplersharvest.game;

import com.keplersharvest.colony.ColonyModule;
import com.keplersharvest.farming.CropDefinition;
import com.keplersharvest.farming.FarmLand;
import com.keplersharvest.farming.FarmPlot;
import com.keplersharvest.farming.FarmResult;
import com.keplersharvest.inventory.ItemCategory;
import com.keplersharvest.inventory.ItemDefinition;
import com.keplersharvest.inventory.ToolKind;
import com.keplersharvest.npc.Colonist;
import com.keplersharvest.world.GridPoint;
import com.keplersharvest.world.MapObject;
import com.keplersharvest.world.MapObjectKind;
import com.keplersharvest.world.WorldMap;

import java.util.Objects;
import java.util.Optional;

/**
 * Turns "the player pressed interact" into a change in the world.
 *
 * <p>Split out of {@link GameSession} so the session stays a state holder and this class stays the
 * one place that decides what a key press means.
 */
public final class PlayerActions {

    private final GameSession session;

    public PlayerActions(GameSession session) {
        this.session = Objects.requireNonNull(session, "session");
    }

    /** What the interaction indicator should highlight, based on the tile the player faces. */
    public Optional<InteractionTarget> currentTarget() {
        GridPoint facing = session.player().facingTile();
        WorldMap map = session.currentMap();

        Optional<Colonist> colonist = colonistAt(facing);
        if (colonist.isPresent()) {
            return Optional.of(InteractionTarget.of(InteractionTarget.Kind.COLONIST, facing,
                    "Talk to " + colonist.get().name(), colonist.get().id()));
        }

        Optional<InteractionTarget> fromObject = map.objectAt(facing).flatMap(o -> describeObject(map, o));
        if (fromObject.isPresent()) {
            return fromObject;
        }

        // A bed or station under the player's own feet still counts.
        Optional<InteractionTarget> underfoot = map.objectAt(session.player().tile())
                .filter(o -> o.kind() == MapObjectKind.BED || o.kind() == MapObjectKind.CRAFTING_STATION)
                .flatMap(o -> describeObject(map, o));
        if (underfoot.isPresent()) {
            return underfoot;
        }

        return describeFarmTile(facing);
    }

    private Optional<Colonist> colonistAt(GridPoint tile) {
        return session.colonistsHere().stream()
                .filter(colonist -> colonist.position().equals(tile))
                .findFirst();
    }

    private Optional<InteractionTarget> describeObject(WorldMap map, MapObject object) {
        String key = com.keplersharvest.world.WorldObjectState.key(map.id(), object.id());
        return switch (object.kind()) {
            case MODULE -> {
                String moduleId = object.requireProperty("module");
                ColonyModule module = session.colony().module(moduleId).orElse(null);
                if (module == null) {
                    yield Optional.empty();
                }
                String prompt = module.online()
                        ? module.definition().name() + " (online)"
                        : "Repair " + module.definition().name() + " ("
                                + module.completedStages() + "/" + module.definition().stageCount() + ")";
                yield Optional.of(InteractionTarget.of(InteractionTarget.Kind.MODULE, object.position(),
                        prompt, moduleId));
            }
            case CREW_LOG -> {
                String logId = object.requireProperty("log");
                if (session.journal().hasLog(logId)) {
                    yield Optional.empty();
                }
                yield Optional.of(InteractionTarget.of(InteractionTarget.Kind.CREW_LOG, object.position(),
                        "Read data slate", logId));
            }
            case RESOURCE_NODE -> {
                int respawnDays = object.intProperty("respawnDays", 0);
                if (!session.worldObjects().isAvailable(key, session.clock().day(), respawnDays)) {
                    yield Optional.empty();
                }
                String itemId = object.requireProperty("item");
                yield Optional.of(InteractionTarget.of(InteractionTarget.Kind.RESOURCE_NODE, object.position(),
                        "Gather " + session.content().itemName(itemId), object.id()));
            }
            case CRAFTING_STATION -> Optional.of(InteractionTarget.of(InteractionTarget.Kind.CRAFTING_STATION,
                    object.position(), "Use " + object.property("title", "workbench"),
                    object.requireProperty("station")));
            case BED -> Optional.of(InteractionTarget.of(InteractionTarget.Kind.BED, object.position(),
                    "Sleep until morning", object.id()));
            case SIGN -> Optional.of(InteractionTarget.of(InteractionTarget.Kind.SIGN, object.position(),
                    "Read", object.id()));
            default -> Optional.empty();
        };
    }

    private Optional<InteractionTarget> describeFarmTile(GridPoint tile) {
        FarmLand land = session.farmLand(session.player().mapId());
        Optional<FarmPlot> plot = land.plotAt(tile);
        if (plot.isEmpty()) {
            return Optional.empty();
        }
        FarmPlot farmPlot = plot.get();
        if (!farmPlot.hasCrop()) {
            return Optional.empty();
        }
        if (farmPlot.withered()) {
            return Optional.of(InteractionTarget.of(InteractionTarget.Kind.HARVESTABLE_CROP, tile,
                    "Clear dead plant", null));
        }
        CropDefinition crop = land.cropAt(tile).orElse(null);
        if (crop == null) {
            return Optional.empty();
        }
        if (crop.matureAt(farmPlot.grownDays())) {
            return Optional.of(InteractionTarget.of(InteractionTarget.Kind.HARVESTABLE_CROP, tile,
                    "Harvest " + crop.name(), crop.id()));
        }
        int left = crop.daysToMaturity() - farmPlot.grownDays();
        return Optional.of(InteractionTarget.of(InteractionTarget.Kind.GROWING_CROP, tile,
                crop.name() + " - " + left + " day" + (left == 1 ? "" : "s") + " left", crop.id()));
    }

    /** The interact key: talk, repair, gather, read, harvest, sleep. */
    public InteractionResult interact() {
        Optional<InteractionTarget> target = currentTarget();
        if (target.isEmpty()) {
            return new InteractionResult.Nothing("Nothing here.");
        }
        InteractionTarget hit = target.get();
        return switch (hit.kind()) {
            case COLONIST -> talk(hit.refId().orElseThrow());
            case MODULE -> {
                session.repair(hit.refId().orElseThrow());
                yield new InteractionResult.Handled(session.latestNotice().orElse(""));
            }
            case CREW_LOG -> readLog(hit);
            case RESOURCE_NODE -> gather(hit.tile());
            case CRAFTING_STATION -> new InteractionResult.OpenCrafting(hit.refId().orElseThrow(), hit.prompt());
            case BED -> new InteractionResult.OfferSleep();
            case SIGN -> new InteractionResult.Handled(signText(hit.tile()));
            case HARVESTABLE_CROP -> harvest(hit.tile());
            case GROWING_CROP -> new InteractionResult.Handled(hit.prompt());
            case SOIL -> new InteractionResult.Nothing("Nothing here.");
        };
    }

    private InteractionResult talk(String colonistId) {
        Optional<com.keplersharvest.dialogue.DialogueRunner> runner = session.talkTo(colonistId);
        String name = session.colonist(colonistId).map(Colonist::name).orElse("Colonist");
        return runner
                .<InteractionResult>map(r -> new InteractionResult.StartDialogue(colonistId, name, r))
                .orElseGet(() -> new InteractionResult.Handled(name + " has nothing to say right now."));
    }

    private InteractionResult readLog(InteractionTarget hit) {
        String logId = hit.refId().orElseThrow();
        Optional<com.keplersharvest.mystery.CrewLogDefinition> log = session.recoverCrewLog(logId);
        if (log.isEmpty()) {
            return new InteractionResult.Nothing("The slate is blank.");
        }
        session.currentMap().objectAt(hit.tile()).ifPresent(object -> session.worldObjects()
                .markConsumed(com.keplersharvest.world.WorldObjectState.key(session.currentMap().id(), object.id())));
        return new InteractionResult.ReadCrewLog(log.get());
    }

    private String signText(GridPoint tile) {
        return session.currentMap().objectAt(tile)
                .map(object -> object.property("text", "The lettering has weathered away."))
                .orElse("");
    }

    /** Picks a resource node, honouring tool requirements, energy and respawn timers. */
    public InteractionResult gather(GridPoint tile) {
        WorldMap map = session.currentMap();
        Optional<MapObject> object = map.objectAt(tile).filter(o -> o.kind() == MapObjectKind.RESOURCE_NODE);
        if (object.isEmpty()) {
            return new InteractionResult.Nothing("Nothing to gather.");
        }
        MapObject node = object.get();
        String key = com.keplersharvest.world.WorldObjectState.key(map.id(), node.id());
        int respawnDays = node.intProperty("respawnDays", 0);
        if (!session.worldObjects().isAvailable(key, session.clock().day(), respawnDays)) {
            return new InteractionResult.Nothing("Already picked clean.");
        }

        String requiredTool = node.property("tool", "");
        if (!requiredTool.isBlank()) {
            ToolKind needed = ToolKind.valueOf(requiredTool.toUpperCase(java.util.Locale.ROOT));
            if (session.toolbar().equippedTool().filter(needed::equals).isEmpty()) {
                return new InteractionResult.Nothing("You need the "
                        + needed.name().toLowerCase(java.util.Locale.ROOT) + " for that.");
            }
        }

        int cost = session.settings().energyCost("gather");
        if (session.player().energy().tooTiredFor(cost)) {
            return new InteractionResult.Nothing("Too tired.");
        }

        String itemId = node.requireProperty("item");
        ItemDefinition item = session.content().item(itemId).orElse(null);
        if (item == null) {
            return new InteractionResult.Nothing("Nothing useful here.");
        }
        int min = node.intProperty("min", 1);
        int max = Math.max(min, node.intProperty("max", min));
        int amount = min + session.random().nextInt(max - min + 1);
        if (!session.inventory().canFit(item, amount)) {
            return new InteractionResult.Nothing("Your pack is full.");
        }

        session.player().energy().spend(cost);
        session.inventory().add(item, amount);
        if (respawnDays > 0) {
            session.worldObjects().markDepleted(key, session.clock().day());
        } else {
            session.worldObjects().markConsumed(key);
        }
        session.publish(new GameEvent.ItemGathered(itemId, amount));
        String message = "Gathered " + amount + " x " + item.name() + ".";
        session.notice(message);
        return new InteractionResult.Handled(message);
    }

    private InteractionResult harvest(GridPoint tile) {
        FarmLand land = session.farmLand(session.player().mapId());
        Optional<CropDefinition> crop = land.cropAt(tile);
        if (land.readyToHarvest(tile)) {
            ItemDefinition produce = crop.flatMap(c -> session.content().item(c.produceItemId())).orElse(null);
            if (produce != null && !session.inventory().canFit(produce, crop.orElseThrow().produceMin())) {
                return new InteractionResult.Nothing("Your pack is full.");
            }
        }
        FarmResult result = land.harvest(tile);
        if (result instanceof FarmResult.Harvested harvested) {
            session.content().item(harvested.produceItemId())
                    .ifPresent(item -> session.inventory().add(item, harvested.amount()));
            session.publish(new GameEvent.CropHarvested(
                    crop.map(CropDefinition::id).orElse(""), harvested.produceItemId(), harvested.amount()));
        }
        session.notice(result.message());
        return result.succeeded()
                ? new InteractionResult.Handled(result.message())
                : new InteractionResult.Nothing(result.message());
    }

    /** The tool key: till, water, cut, scan, or plant the selected seed. */
    public InteractionResult useTool() {
        Optional<ItemDefinition> held = session.toolbar().selectedItem();
        if (held.isEmpty()) {
            return new InteractionResult.Nothing("Nothing equipped.");
        }
        ItemDefinition item = held.get();
        if (item.category() == ItemCategory.SEED) {
            return plant(item);
        }
        Optional<ToolKind> tool = item.toolKind();
        if (tool.isEmpty()) {
            return new InteractionResult.Nothing(item.name() + " is not a tool.");
        }
        GridPoint tile = session.player().facingTile();
        return switch (tool.get()) {
            case TILLER -> applyFarmAction("till", () -> session.farmLand(session.player().mapId()).till(tile));
            case IRRIGATOR -> applyFarmAction("water", () -> session.farmLand(session.player().mapId()).water(tile));
            case CUTTER -> gather(tile);
            case SCANNER -> scan(tile);
        };
    }

    private InteractionResult plant(ItemDefinition seed) {
        GridPoint tile = session.player().facingTile();
        String cropId = seed.plantsCropId().orElseThrow();
        int cost = session.settings().energyCost("plant");
        if (session.player().energy().tooTiredFor(cost)) {
            return new InteractionResult.Nothing("Too tired to plant.");
        }
        FarmResult result = session.farmLand(session.player().mapId()).plant(tile, cropId);
        if (result instanceof FarmResult.Planted planted) {
            session.inventory().remove(seed.id(), 1);
            session.player().energy().spend(cost);
            session.publish(new GameEvent.CropPlanted(planted.crop().id()));
        }
        session.notice(result.message());
        return result.succeeded()
                ? new InteractionResult.Handled(result.message())
                : new InteractionResult.Nothing(result.message());
    }

    private InteractionResult applyFarmAction(String costKey, java.util.function.Supplier<FarmResult> action) {
        int cost = session.settings().energyCost(costKey);
        if (session.player().energy().tooTiredFor(cost)) {
            return new InteractionResult.Nothing("Too tired.");
        }
        FarmResult result = action.get();
        if (result.succeeded()) {
            session.player().energy().spend(cost);
            if (result instanceof FarmResult.Tilled) {
                session.publish(new GameEvent.SoilTilled(1));
            }
        }
        session.notice(result.message());
        return result.succeeded()
                ? new InteractionResult.Handled(result.message())
                : new InteractionResult.Nothing(result.message());
    }

    /** The scanner reads module diagnostics and exposes hidden data slates. */
    private InteractionResult scan(GridPoint tile) {
        WorldMap map = session.currentMap();
        Optional<MapObject> object = map.objectAt(tile);
        if (object.isPresent() && object.get().kind() == MapObjectKind.MODULE) {
            String moduleId = object.get().requireProperty("module");
            ColonyModule module = session.colony().module(moduleId).orElse(null);
            if (module != null) {
                String text = module.online()
                        ? module.definition().name() + ": nominal. " + module.definition().benefitText()
                        : module.definition().name() + ": " + module.nextStage()
                                .map(stage -> stage.name() + " needs "
                                        + session.describeItems(stage.materials()))
                                .orElse("offline");
                session.notice(text);
                return new InteractionResult.Handled(text);
            }
        }
        if (object.isPresent() && object.get().kind() == MapObjectKind.CREW_LOG) {
            return readLog(InteractionTarget.of(InteractionTarget.Kind.CREW_LOG, tile, "Read data slate",
                    object.get().requireProperty("log")));
        }
        String text = "Scan: no anomalies at " + tile + ".";
        session.notice(text);
        return new InteractionResult.Handled(text);
    }

    /** Called after movement: walking onto a portal changes map. */
    public boolean checkPortal() {
        GridPoint tile = session.player().tile();
        Optional<MapObject> portal = session.currentMap().objectAt(tile)
                .filter(object -> object.kind() == MapObjectKind.PORTAL);
        if (portal.isEmpty()) {
            return false;
        }
        MapObject object = portal.get();
        session.travelTo(object.requireProperty("target"), object.property("spawn", ""));
        return true;
    }
}
