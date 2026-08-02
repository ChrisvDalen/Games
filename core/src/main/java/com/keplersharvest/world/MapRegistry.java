package com.keplersharvest.world;

import com.badlogic.gdx.utils.JsonValue;
import com.keplersharvest.configuration.ConfigurationException;
import com.keplersharvest.configuration.Json5;
import com.keplersharvest.configuration.ResourceReader;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Every map the game knows about, listed in {@code config/maps.json}.
 *
 * <p>Adding a map means adding a {@code .tmj} file and one line of JSON - no engine change. Portals
 * are validated on load so a typo in a target map id fails at start-up rather than mid-walk.
 */
public final class MapRegistry {

    private final Map<String, WorldMap> maps;
    private final TileSet tileSet;
    private final String startMapId;
    private final String startSpawn;

    private MapRegistry(Map<String, WorldMap> maps, TileSet tileSet, String startMapId, String startSpawn) {
        this.maps = Map.copyOf(maps);
        this.tileSet = tileSet;
        this.startMapId = startMapId;
        this.startSpawn = startSpawn;
    }

    public static MapRegistry load(ResourceReader reader) {
        return load(reader, "config/maps.json");
    }

    public static MapRegistry load(ResourceReader reader, String indexPath) {
        Objects.requireNonNull(reader, "reader");
        JsonValue root = Json5.parse(reader.readText(indexPath), indexPath);
        TiledJsonMapLoader loader = new TiledJsonMapLoader(reader);
        TileSet tileSet = loader.loadTileSet(Json5.requireString(root, "tileset", indexPath));

        Map<String, WorldMap> maps = new LinkedHashMap<>();
        for (JsonValue entry : Json5.array(root, "maps")) {
            String id = Json5.requireString(entry, "id", indexPath);
            String file = Json5.requireString(entry, "file", indexPath);
            if (maps.containsKey(id)) {
                throw new ConfigurationException("Duplicate map id '" + id + "' in " + indexPath);
            }
            maps.put(id, loader.load(id, file));
        }
        if (maps.isEmpty()) {
            throw new ConfigurationException("No maps listed in " + indexPath);
        }

        JsonValue start = Json5.requireChild(root, "start", indexPath);
        String startMapId = Json5.requireString(start, "map", indexPath);
        String startSpawn = Json5.requireString(start, "spawn", indexPath);
        if (!maps.containsKey(startMapId)) {
            throw new ConfigurationException("Start map '" + startMapId + "' is not registered");
        }

        MapRegistry registry = new MapRegistry(maps, tileSet, startMapId, startSpawn);
        registry.validatePortals();
        return registry;
    }

    public TileSet tileSet() {
        return tileSet;
    }

    public String startMapId() {
        return startMapId;
    }

    public String startSpawn() {
        return startSpawn;
    }

    public Collection<WorldMap> maps() {
        return maps.values();
    }

    public Optional<WorldMap> find(String mapId) {
        return Optional.ofNullable(maps.get(mapId));
    }

    public WorldMap require(String mapId) {
        return find(mapId).orElseThrow(() -> new ConfigurationException("Unknown map: " + mapId));
    }

    private void validatePortals() {
        for (WorldMap map : maps.values()) {
            for (MapObject portal : map.objectsOfKind(MapObjectKind.PORTAL)) {
                String target = portal.requireProperty("target");
                WorldMap destination = maps.get(target);
                if (destination == null) {
                    throw new ConfigurationException("Portal " + portal.id() + " in map " + map.id()
                            + " targets unknown map '" + target + "'");
                }
                String spawn = portal.property("spawn", "");
                if (!spawn.isBlank() && destination.spawn(spawn).isEmpty()) {
                    throw new ConfigurationException("Portal " + portal.id() + " in map " + map.id()
                            + " targets unknown spawn '" + spawn + "' in map " + target);
                }
            }
        }
        if (require(startMapId).spawn(startSpawn).isEmpty()) {
            throw new ConfigurationException("Start spawn '" + startSpawn + "' missing from map " + startMapId);
        }
    }
}
