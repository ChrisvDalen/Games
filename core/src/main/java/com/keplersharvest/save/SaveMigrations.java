package com.keplersharvest.save;

import com.badlogic.gdx.utils.JsonValue;

import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;

/**
 * Upgrades older save files to the current format.
 *
 * <p>Migrations run on the raw {@link JsonValue} before it is mapped onto {@link SaveData}, so a
 * step can rename, split or default fields that no longer exist as Java members. Register one step
 * per version bump; {@link #migrate} then chains them.
 */
public final class SaveMigrations {

    /** A single version-to-version upgrade. */
    public record Step(int fromVersion, String description, UnaryOperator<JsonValue> apply) {
    }

    private static final List<Step> STEPS = List.of(
            // Format 0 predates versioning: files written before the field existed.
            new Step(0, "Stamp unversioned saves as format 1", root -> {
                setInt(root, "version", 1);
                return root;
            }),
            // Format 1 recorded only the world seed. Starting the restored stream at the seed is
            // exactly what a format-1 file used to do on load, so old saves behave as they did.
            new Step(1, "Start the random stream from the world seed", root -> {
                setLong(root, "randomState", root.getLong("seed", 0L));
                setInt(root, "version", 2);
                return root;
            })
    );

    private SaveMigrations() {
    }

    /** The version recorded in the file, defaulting to 0 for pre-versioning saves. */
    public static int versionOf(JsonValue root) {
        return root == null ? 0 : root.getInt("version", 0);
    }

    /**
     * Applies every step needed to bring {@code root} up to the current format.
     *
     * @return the migrated tree, and the descriptions of the steps applied
     */
    public static Result migrate(JsonValue root) {
        List<String> applied = new ArrayList<>();
        JsonValue current = root;
        int version = versionOf(current);
        boolean progressed = true;
        while (version < SaveData.CURRENT_VERSION && progressed) {
            progressed = false;
            for (Step step : STEPS) {
                if (step.fromVersion() == version) {
                    current = step.apply().apply(current);
                    applied.add(step.description());
                    version = versionOf(current);
                    progressed = true;
                    break;
                }
            }
        }
        return new Result(current, version, List.copyOf(applied));
    }

    public record Result(JsonValue root, int version, List<String> stepsApplied) {
        public boolean upToDate() {
            return version == SaveData.CURRENT_VERSION;
        }
    }

    private static void setInt(JsonValue root, String name, int value) {
        setLong(root, name, value);
    }

    private static void setLong(JsonValue root, String name, long value) {
        JsonValue existing = root.get(name);
        if (existing != null) {
            existing.set(value, null);
            return;
        }
        JsonValue child = new JsonValue(value);
        child.name = name;
        root.addChild(name, child);
    }
}
