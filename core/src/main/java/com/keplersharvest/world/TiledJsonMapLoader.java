package com.keplersharvest.world;

import com.badlogic.gdx.utils.JsonValue;
import com.keplersharvest.configuration.ConfigurationException;
import com.keplersharvest.configuration.Json5;
import com.keplersharvest.configuration.ResourceReader;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Reads Tiled's JSON map format ({@code .tmj}) and tileset format ({@code .tsj}).
 *
 * <p>Only the subset the game needs is supported: finite orthogonal maps, tile layers named
 * {@code ground}, {@code overlay}, {@code collision} and {@code farmable}, and one object group.
 * That subset is enough for the maps to round-trip through Tiled, while the parser stays plain Java
 * so maps can be loaded in unit tests.
 */
public final class TiledJsonMapLoader {

    private static final String GROUND = "ground";
    private static final String OVERLAY = "overlay";
    private static final String COLLISION = "collision";
    private static final String FARMABLE = "farmable";

    private final ResourceReader reader;

    public TiledJsonMapLoader(ResourceReader reader) {
        this.reader = Objects.requireNonNull(reader, "reader");
    }

    public TileSet loadTileSet(String path) {
        JsonValue root = Json5.parse(reader.readText(path), path);
        int firstGid = Json5.integer(root, "firstgid", 1);
        Map<Integer, String> colours = new LinkedHashMap<>();
        Map<Integer, String> labels = new LinkedHashMap<>();
        for (JsonValue tile : Json5.array(root, "tiles")) {
            int id = Json5.requireInt(tile, "id", path);
            Map<String, String> properties = readProperties(tile);
            colours.put(id, properties.getOrDefault("colour", "ff00ff"));
            labels.put(id, properties.getOrDefault("label", Json5.string(tile, "type", "tile" + id)));
        }
        if (colours.isEmpty()) {
            throw new ConfigurationException("Tileset " + path + " declares no tiles");
        }
        return new TileSet(Json5.string(root, "name", path), firstGid, colours, labels);
    }

    public WorldMap load(String mapId, String path) {
        JsonValue root = Json5.parse(reader.readText(path), path);
        int width = Json5.requireInt(root, "width", path);
        int height = Json5.requireInt(root, "height", path);
        int tileSize = Json5.requireInt(root, "tilewidth", path);
        if (width < 1 || height < 1) {
            throw new ConfigurationException("Map " + path + " has a non-positive size");
        }

        Map<String, String> mapProperties = readProperties(root);
        String displayName = mapProperties.getOrDefault("displayName", mapId);
        String ambient = mapProperties.getOrDefault("ambientColour", "0b1016");

        int[] ground = new int[width * height];
        int[] overlay = new int[width * height];
        boolean[] solid = new boolean[width * height];
        boolean[] farmable = new boolean[width * height];
        List<MapObject> objects = new ArrayList<>();

        for (JsonValue layer : Json5.array(root, "layers")) {
            String type = Json5.string(layer, "type", "");
            String name = Json5.string(layer, "name", "");
            switch (type) {
                case "tilelayer" -> readTileLayer(path, layer, name, width, height, ground, overlay, solid, farmable);
                case "objectgroup" -> objects.addAll(readObjects(path, layer, width, height, tileSize));
                default -> { // group and image layers are ignored on purpose
                }
            }
        }

        WorldMap map = new WorldMap(mapId, displayName, width, height, tileSize,
                ground, overlay, solid, farmable, objects, ambient);
        validate(map, path);
        return map;
    }

    private void readTileLayer(String path,
                               JsonValue layer,
                               String name,
                               int width,
                               int height,
                               int[] ground,
                               int[] overlay,
                               boolean[] solid,
                               boolean[] farmable) {
        JsonValue data = layer.get("data");
        if (data == null || data.isNull()) {
            throw new ConfigurationException("Tile layer '" + name + "' in " + path + " has no data "
                    + "(is the map saved with CSV/uncompressed tile data?)");
        }
        int[] raw = data.asIntArray();
        if (raw.length != width * height) {
            throw new ConfigurationException("Tile layer '" + name + "' in " + path + " has "
                    + raw.length + " tiles, expected " + width * height);
        }
        for (int i = 0; i < raw.length; i++) {
            int row = i / width;
            int column = i % width;
            // Tiled rows run top-down; the game grid is bottom-up.
            int target = (height - 1 - row) * width + column;
            int gid = raw[i];
            switch (name) {
                case GROUND -> ground[target] = gid;
                case OVERLAY -> overlay[target] = gid;
                case COLLISION -> solid[target] = gid != 0;
                case FARMABLE -> farmable[target] = gid != 0;
                default -> { // unknown layers are decorative; ignore
                }
            }
        }
    }

    private List<MapObject> readObjects(String path, JsonValue layer, int width, int height, int tileSize) {
        List<MapObject> objects = new ArrayList<>();
        for (JsonValue object : Json5.array(layer, "objects")) {
            String rawKind = Json5.string(object, "class", Json5.string(object, "type", ""));
            MapObjectKind kind = MapObjectKind.parse(rawKind).orElse(null);
            if (kind == null) {
                throw new ConfigurationException("Map object in " + path + " has unknown class '" + rawKind + "'");
            }
            float x = Json5.decimal(object, "x", 0f);
            float y = Json5.decimal(object, "y", 0f);
            int column = (int) Math.floor(x / tileSize);
            int row = (int) Math.floor(y / tileSize);
            GridPoint position = new GridPoint(column, height - 1 - row);
            if (position.x() < 0 || position.x() >= width || position.y() < 0 || position.y() >= height) {
                throw new ConfigurationException("Map object in " + path + " sits outside the map at " + position);
            }
            String id = Json5.string(object, "name", "");
            if (id.isBlank()) {
                id = "object_" + Json5.integer(object, "id", objects.size());
            }
            objects.add(new MapObject(id, kind, position, readProperties(object)));
        }
        return objects;
    }

    /** Flattens Tiled's {@code [{name,type,value}]} property arrays into a string map. */
    private Map<String, String> readProperties(JsonValue owner) {
        Map<String, String> properties = new LinkedHashMap<>();
        for (JsonValue property : Json5.array(owner, "properties")) {
            String name = Json5.string(property, "name", null);
            JsonValue value = property.get("value");
            if (name != null && value != null && !value.isNull()) {
                properties.put(name, value.asString());
            }
        }
        return properties;
    }

    private void validate(WorldMap map, String path) {
        if (map.objectsOfKind(MapObjectKind.SPAWN).isEmpty()) {
            throw new ConfigurationException("Map " + path + " has no spawn point");
        }
        for (MapObject portal : map.objectsOfKind(MapObjectKind.PORTAL)) {
            if (!portal.properties().containsKey("target")) {
                throw new ConfigurationException("Portal " + portal.id() + " in " + path + " has no target map");
            }
        }
    }
}
