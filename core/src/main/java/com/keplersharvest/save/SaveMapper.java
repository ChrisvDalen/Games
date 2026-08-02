package com.keplersharvest.save;

import com.keplersharvest.colony.ColonyModule;
import com.keplersharvest.configuration.GameContent;
import com.keplersharvest.farming.FarmLand;
import com.keplersharvest.farming.FarmPlot;
import com.keplersharvest.farming.SoilState;
import com.keplersharvest.game.GameSession;
import com.keplersharvest.inventory.ItemDefinition;
import com.keplersharvest.inventory.ItemStack;
import com.keplersharvest.npc.Colonist;
import com.keplersharvest.quests.QuestState;
import com.keplersharvest.quests.QuestStatus;
import com.keplersharvest.world.Direction;
import com.keplersharvest.world.GridPoint;
import com.keplersharvest.world.WorldPosition;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Converts between a live {@link GameSession} and the flat {@link SaveData} form.
 *
 * <p>Loading is forgiving on purpose: entries naming content that no longer exists (a removed item,
 * a renamed quest) are skipped rather than aborting the load, so a save survives content edits.
 */
public final class SaveMapper {

    private SaveMapper() {
    }

    public static SaveData capture(GameSession session) {
        SaveData data = new SaveData();
        data.version = SaveData.CURRENT_VERSION;
        data.seed = session.seed();
        data.savedAt = Instant.now().toString();
        data.gameVersion = "0.1.0";

        data.day = session.clock().day();
        data.minuteOfDay = session.clock().now().minuteOfDay();

        data.mapId = session.player().mapId();
        data.playerX = session.player().position().x();
        data.playerY = session.player().position().y();
        data.facing = session.player().facing().name();
        data.energy = session.player().energy().current();

        data.toolbarIndex = session.toolbar().selectedIndex();
        for (int slot = 0; slot < session.inventory().size(); slot++) {
            int index = slot;
            session.inventory().slot(slot).ifPresent(stack ->
                    data.inventory.add(new SaveData.SlotEntry(index, stack.item().id(), stack.count())));
        }

        session.farmlands().forEach((mapId, land) -> land.plots().forEach(plot -> {
            if (plot.soil() == SoilState.WILD && !plot.hasCrop()) {
                return;
            }
            SaveData.PlotEntry entry = new SaveData.PlotEntry();
            entry.map = mapId;
            entry.x = plot.position().x();
            entry.y = plot.position().y();
            entry.soil = plot.soil().name();
            entry.crop = plot.cropId().orElse("");
            entry.grownDays = plot.grownDays();
            entry.dryDays = plot.dryDays();
            entry.watered = plot.watered();
            entry.withered = plot.withered();
            data.plots.add(entry);
        }));

        for (ColonyModule module : session.colony().modules()) {
            data.modules.add(new SaveData.ModuleEntry(module.id(), module.completedStages()));
        }

        session.relationships().snapshot()
                .forEach((id, points) -> data.relationships.add(new SaveData.CountEntry(id, points)));
        for (Colonist colonist : session.colonists()) {
            if (colonist.lastGreetedDay() > 0) {
                data.greetings.add(new SaveData.CountEntry(colonist.id(), colonist.lastGreetedDay()));
            }
        }

        for (QuestState state : session.questLog().all()) {
            if (state.status() == QuestStatus.NOT_STARTED) {
                continue;
            }
            SaveData.QuestEntry entry = new SaveData.QuestEntry();
            entry.id = state.questId();
            entry.status = state.status().name();
            for (int counter : state.counterSnapshot()) {
                entry.counters.add(counter);
            }
            data.quests.add(entry);
        }

        session.worldObjects().depletedSnapshot()
                .forEach((key, day) -> data.depletedNodes.add(new SaveData.CountEntry(key, day)));
        data.consumedObjects.addAll(session.worldObjects().consumedSnapshot());

        data.flags.addAll(session.flags());
        session.landmarks().forEach((id, name) -> data.landmarks.add(new SaveData.TextEntry(id, name)));

        data.foundLogs.addAll(session.journal().foundLogsSnapshot());
        data.evidence.addAll(session.journal().evidenceSnapshot());
        data.revealSeen = session.journal().revealSeen();

        return data;
    }

    /** Builds a session from save data. Unknown content ids are skipped with no error. */
    public static GameSession restore(GameContent content, SaveData data) {
        GameSession session = GameSession.forLoading(content, data.seed);

        int minutesFromStart = (data.day - 1) * 24 * 60 + data.minuteOfDay
                - content.settings().wakeMinute();
        if (minutesFromStart > 0) {
            session.clock().advanceMinutes(minutesFromStart);
        }

        String mapId = content.maps().find(data.mapId).map(m -> m.id()).orElse(content.maps().startMapId());
        session.player().moveTo(mapId, new WorldPosition(data.playerX, data.playerY));
        session.player().setFacing(parseDirection(data.facing));

        restoreInventory(content, data, session);
        restorePlots(content, data, session);
        restoreModules(data, session);
        restoreRelationships(data, session);
        restoreQuests(data, session);
        restoreWorldObjects(data, session);

        session.restoreFlags(data.flags);
        Map<String, String> landmarks = new LinkedHashMap<>();
        data.landmarks.forEach(entry -> landmarks.put(entry.key, entry.value));
        session.restoreLandmarks(landmarks);

        Set<String> logs = new LinkedHashSet<>(data.foundLogs);
        Set<String> evidence = new LinkedHashSet<>(data.evidence);
        session.journal().restore(logs, evidence, data.revealSeen);

        session.afterLoad();
        // Energy last: the max depends on which modules were restored as online.
        session.player().energy().set(data.energy);
        return session;
    }

    private static void restoreInventory(GameContent content, SaveData data, GameSession session) {
        session.inventory().clear();
        for (SaveData.SlotEntry entry : data.inventory) {
            if (entry.slot >= session.inventory().size()) {
                continue;
            }
            Optional<ItemDefinition> item = content.item(entry.item);
            if (item.isEmpty()) {
                continue;
            }
            int count = Math.clamp(entry.count, 1, item.get().maxStack());
            session.inventory().setSlot(entry.slot, ItemStack.of(item.get(), count));
        }
        int toolbarIndex = Math.clamp(data.toolbarIndex, 0, session.toolbar().size() - 1);
        session.toolbar().select(toolbarIndex);
    }

    private static void restorePlots(GameContent content, SaveData data, GameSession session) {
        for (SaveData.PlotEntry entry : data.plots) {
            if (content.maps().find(entry.map).isEmpty()) {
                continue;
            }
            FarmLand land = session.farmLand(entry.map);
            FarmPlot plot = land.restorePlot(new GridPoint(entry.x, entry.y));
            String cropId = entry.crop == null || entry.crop.isBlank() ? null : entry.crop;
            if (cropId != null && content.crop(cropId).isEmpty()) {
                cropId = null;
            }
            plot.restore(parseSoil(entry.soil), cropId, entry.grownDays, entry.dryDays,
                    entry.watered, entry.withered);
        }
    }

    private static void restoreModules(SaveData data, GameSession session) {
        for (SaveData.ModuleEntry entry : data.modules) {
            session.colony().module(entry.id).ifPresent(module -> module.restore(entry.completedStages));
        }
    }

    private static void restoreRelationships(SaveData data, GameSession session) {
        Map<String, Integer> points = new LinkedHashMap<>();
        data.relationships.forEach(entry -> points.put(entry.key, entry.value));
        session.relationships().restore(points);
        data.greetings.forEach(entry ->
                session.colonist(entry.key).ifPresent(colonist -> colonist.restoreGreeting(entry.value)));
    }

    private static void restoreQuests(SaveData data, GameSession session) {
        for (SaveData.QuestEntry entry : data.quests) {
            session.questLog().state(entry.id).ifPresent(state -> {
                int[] counters = new int[entry.counters.size()];
                for (int i = 0; i < counters.length; i++) {
                    counters[i] = entry.counters.get(i);
                }
                state.restore(parseStatus(entry.status), counters);
            });
        }
    }

    private static void restoreWorldObjects(SaveData data, GameSession session) {
        Map<String, Integer> depleted = new LinkedHashMap<>();
        data.depletedNodes.forEach(entry -> depleted.put(entry.key, entry.value));
        session.worldObjects().restore(depleted, new LinkedHashSet<>(data.consumedObjects));
    }

    private static Direction parseDirection(String raw) {
        try {
            return Direction.valueOf(raw);
        } catch (RuntimeException e) {
            return Direction.DOWN;
        }
    }

    private static SoilState parseSoil(String raw) {
        try {
            return SoilState.valueOf(raw);
        } catch (RuntimeException e) {
            return SoilState.WILD;
        }
    }

    private static QuestStatus parseStatus(String raw) {
        try {
            return QuestStatus.valueOf(raw);
        } catch (RuntimeException e) {
            return QuestStatus.NOT_STARTED;
        }
    }
}
