package com.keplersharvest.configuration;

import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Small strict-reading helpers over libGDX {@link JsonValue}.
 *
 * <p>Content is hand-authored, so every accessor reports the offending field by name rather than
 * letting a {@code null} escape into the domain model.
 */
public final class Json5 {

    private Json5() {
    }

    public static JsonValue parse(String text, String origin) {
        try {
            return new JsonReader().parse(text);
        } catch (RuntimeException e) {
            throw new ConfigurationException("Malformed JSON in " + origin, e);
        }
    }

    public static JsonValue requireChild(JsonValue parent, String name, String origin) {
        JsonValue child = parent.get(name);
        if (child == null) {
            throw new ConfigurationException("Missing '" + name + "' in " + origin);
        }
        return child;
    }

    public static String requireString(JsonValue parent, String name, String origin) {
        JsonValue child = parent.get(name);
        if (child == null || child.isNull() || child.asString() == null) {
            throw new ConfigurationException("Missing string '" + name + "' in " + origin);
        }
        return child.asString();
    }

    public static String string(JsonValue parent, String name, String fallback) {
        JsonValue child = parent.get(name);
        return child == null || child.isNull() ? fallback : child.asString();
    }

    public static Optional<String> optionalString(JsonValue parent, String name) {
        JsonValue child = parent.get(name);
        if (child == null || child.isNull()) {
            return Optional.empty();
        }
        String value = child.asString();
        return value == null || value.isBlank() ? Optional.empty() : Optional.of(value);
    }

    public static int requireInt(JsonValue parent, String name, String origin) {
        JsonValue child = parent.get(name);
        if (child == null || child.isNull()) {
            throw new ConfigurationException("Missing integer '" + name + "' in " + origin);
        }
        return child.asInt();
    }

    public static int integer(JsonValue parent, String name, int fallback) {
        JsonValue child = parent.get(name);
        return child == null || child.isNull() ? fallback : child.asInt();
    }

    public static float decimal(JsonValue parent, String name, float fallback) {
        JsonValue child = parent.get(name);
        return child == null || child.isNull() ? fallback : child.asFloat();
    }

    public static boolean bool(JsonValue parent, String name, boolean fallback) {
        JsonValue child = parent.get(name);
        return child == null || child.isNull() ? fallback : child.asBoolean();
    }

    /** Reads {@code {"scrap_alloy": 6, "silica_shard": 2}} into an ordered map. */
    public static Map<String, Integer> intMap(JsonValue parent, String name) {
        Map<String, Integer> result = new LinkedHashMap<>();
        JsonValue child = parent == null ? null : parent.get(name);
        if (child == null || child.isNull()) {
            return result;
        }
        for (JsonValue entry = child.child; entry != null; entry = entry.next) {
            result.put(entry.name, entry.asInt());
        }
        return result;
    }

    public static List<String> stringList(JsonValue parent, String name) {
        List<String> result = new ArrayList<>();
        JsonValue child = parent == null ? null : parent.get(name);
        if (child == null || child.isNull()) {
            return result;
        }
        for (JsonValue entry = child.child; entry != null; entry = entry.next) {
            result.add(entry.asString());
        }
        return result;
    }

    public static List<Integer> intList(JsonValue parent, String name) {
        List<Integer> result = new ArrayList<>();
        JsonValue child = parent == null ? null : parent.get(name);
        if (child == null || child.isNull()) {
            return result;
        }
        for (JsonValue entry = child.child; entry != null; entry = entry.next) {
            result.add(entry.asInt());
        }
        return result;
    }

    /** Iterates the elements of an array field, tolerating a missing field as "empty". */
    public static Iterable<JsonValue> array(JsonValue parent, String name) {
        JsonValue child = parent == null ? null : parent.get(name);
        if (child == null || child.isNull()) {
            return List.of();
        }
        List<JsonValue> items = new ArrayList<>();
        for (JsonValue entry = child.child; entry != null; entry = entry.next) {
            items.add(entry);
        }
        return items;
    }

    public static <E extends Enum<E>> E requireEnum(Class<E> type, JsonValue parent, String name, String origin) {
        String raw = requireString(parent, name, origin);
        try {
            return Enum.valueOf(type, raw.toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new ConfigurationException(
                    "Unknown " + type.getSimpleName() + " '" + raw + "' in " + origin, e);
        }
    }
}
