package com.keplersharvest.world;

import java.util.Map;
import java.util.Objects;

/**
 * A placed, interactable thing read from a map's object layer.
 *
 * @param id         unique within its map
 * @param properties Tiled custom properties, kept as strings so new object kinds need no loader
 *                   changes
 */
public record MapObject(String id, MapObjectKind kind, GridPoint position, Map<String, String> properties) {

    public MapObject {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(position, "position");
        properties = Map.copyOf(Objects.requireNonNull(properties, "properties"));
    }

    public String property(String key, String fallback) {
        return properties.getOrDefault(key, fallback);
    }

    public String requireProperty(String key) {
        String value = properties.get(key);
        if (value == null) {
            throw new IllegalStateException("Map object " + id + " (" + kind + ") is missing property '" + key + "'");
        }
        return value;
    }

    public int intProperty(String key, int fallback) {
        String value = properties.get(key);
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    public boolean boolProperty(String key, boolean fallback) {
        String value = properties.get(key);
        return value == null ? fallback : Boolean.parseBoolean(value.trim());
    }
}
