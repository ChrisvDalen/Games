package com.moneyfirst.pourperfect.fakes;

import com.badlogic.gdx.Preferences;

import java.util.HashMap;
import java.util.Map;

/**
 * Plain in-memory {@link Preferences} fake. {@code Preferences} is itself only an interface in
 * libGDX (the real implementations are Android's SharedPreferences-backed one and RoboVM's
 * NSUserDefaults-backed one), so this fake lets every persisted-state class in {@code core} be
 * exercised in plain JUnit with no libGDX application/backend running at all.
 */
public final class InMemoryPreferences implements Preferences {

    private final Map<String, Object> values = new HashMap<>();

    @Override
    public Preferences putBoolean(String key, boolean val) {
        values.put(key, val);
        return this;
    }

    @Override
    public Preferences putInteger(String key, int val) {
        values.put(key, val);
        return this;
    }

    @Override
    public Preferences putLong(String key, long val) {
        values.put(key, val);
        return this;
    }

    @Override
    public Preferences putFloat(String key, float val) {
        values.put(key, val);
        return this;
    }

    @Override
    public Preferences putString(String key, String val) {
        values.put(key, val);
        return this;
    }

    @Override
    public Preferences put(Map<String, ?> vals) {
        values.putAll(vals);
        return this;
    }

    @Override
    public boolean getBoolean(String key) {
        return getBoolean(key, false);
    }

    @Override
    public int getInteger(String key) {
        return getInteger(key, 0);
    }

    @Override
    public long getLong(String key) {
        return getLong(key, 0L);
    }

    @Override
    public float getFloat(String key) {
        return getFloat(key, 0f);
    }

    @Override
    public String getString(String key) {
        return getString(key, "");
    }

    @Override
    public boolean getBoolean(String key, boolean defValue) {
        Object v = values.get(key);
        return v instanceof Boolean ? (Boolean) v : defValue;
    }

    @Override
    public int getInteger(String key, int defValue) {
        Object v = values.get(key);
        return v instanceof Integer ? (Integer) v : defValue;
    }

    @Override
    public long getLong(String key, long defValue) {
        Object v = values.get(key);
        return v instanceof Long ? (Long) v : defValue;
    }

    @Override
    public float getFloat(String key, float defValue) {
        Object v = values.get(key);
        return v instanceof Float ? (Float) v : defValue;
    }

    @Override
    public String getString(String key, String defValue) {
        Object v = values.get(key);
        return v instanceof String ? (String) v : defValue;
    }

    @Override
    public Map<String, ?> get() {
        return new HashMap<>(values);
    }

    @Override
    public boolean contains(String key) {
        return values.containsKey(key);
    }

    @Override
    public void clear() {
        values.clear();
    }

    @Override
    public void remove(String key) {
        values.remove(key);
    }

    @Override
    public void flush() {
        // No-op: everything is already "persisted" in the in-memory map.
    }
}
