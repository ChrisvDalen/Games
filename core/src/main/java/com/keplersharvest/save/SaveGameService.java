package com.keplersharvest.save;

import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.JsonWriter;
import com.keplersharvest.configuration.GameContent;
import com.keplersharvest.game.GameSession;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Objects;

/**
 * Reads and writes the single save slot as human-readable JSON.
 *
 * <p>Writes go to a temporary file first and are then moved into place, so a crash mid-write cannot
 * leave a half-written save behind. Reads never throw: a missing, unreadable or malformed file comes
 * back as a {@link LoadOutcome} the caller can show to the player.
 */
public final class SaveGameService {

    /** Overridable so tests never touch a real profile directory. */
    public static final String DIRECTORY_PROPERTY = "keplersharvest.saveDir";

    private static final String SLOT_FILE = "slot1.json";

    private final Path directory;
    private final Json json;

    public SaveGameService() {
        this(defaultDirectory());
    }

    public SaveGameService(Path directory) {
        this.directory = Objects.requireNonNull(directory, "directory");
        this.json = new Json();
        json.setOutputType(JsonWriter.OutputType.json);
        json.setUsePrototypes(false);
        json.setIgnoreUnknownFields(true);
    }

    public static Path defaultDirectory() {
        String override = System.getProperty(DIRECTORY_PROPERTY);
        if (override != null && !override.isBlank()) {
            return Path.of(override);
        }
        return Path.of(System.getProperty("user.home", "."), ".keplers-harvest");
    }

    public Path slotFile() {
        return directory.resolve(SLOT_FILE);
    }

    public boolean hasSave() {
        return Files.isRegularFile(slotFile());
    }

    /** Writes the session to the save slot. */
    public SaveOutcome save(GameSession session) {
        SaveData data = SaveMapper.capture(session);
        List<String> problems = SaveValidation.problems(data);
        if (!problems.isEmpty()) {
            return new SaveOutcome.Failed("Refusing to write an invalid save: " + String.join("; ", problems));
        }
        try {
            Files.createDirectories(directory);
            Path temporary = directory.resolve(SLOT_FILE + ".tmp");
            Files.writeString(temporary, json.prettyPrint(data), StandardCharsets.UTF_8);
            Files.move(temporary, slotFile(),
                    StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            return new SaveOutcome.Saved(slotFile());
        } catch (IOException | RuntimeException e) {
            return new SaveOutcome.Failed("Could not write the save file: " + e.getMessage());
        }
    }

    /** Loads the save slot into a fresh session. */
    public LoadOutcome load(GameContent content) {
        Path file = slotFile();
        if (!Files.isRegularFile(file)) {
            return new LoadOutcome.NoSave();
        }
        String text;
        try {
            text = Files.readString(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            return new LoadOutcome.Unreadable("Could not read " + file + ": " + e.getMessage());
        }
        return parse(content, text);
    }

    /** Exposed separately so the format can be tested without touching the filesystem. */
    public LoadOutcome parse(GameContent content, String text) {
        JsonValue root;
        try {
            root = new JsonReader().parse(text);
        } catch (RuntimeException e) {
            return new LoadOutcome.Unreadable("The save file is not valid JSON.");
        }
        if (root == null || !root.isObject()) {
            return new LoadOutcome.Unreadable("The save file is empty.");
        }

        SaveMigrations.Result migration = SaveMigrations.migrate(root);
        if (!migration.upToDate() && migration.version() < SaveData.CURRENT_VERSION) {
            return new LoadOutcome.Unreadable("No migration path from save format " + migration.version() + ".");
        }

        SaveData data;
        try {
            data = json.readValue(SaveData.class, migration.root());
        } catch (RuntimeException e) {
            return new LoadOutcome.Unreadable("The save file could not be read: " + e.getMessage());
        }

        List<String> problems = SaveValidation.problems(data);
        if (!problems.isEmpty()) {
            return new LoadOutcome.Invalid(problems);
        }

        try {
            return new LoadOutcome.Loaded(SaveMapper.restore(content, data), migration.stepsApplied());
        } catch (RuntimeException e) {
            return new LoadOutcome.Unreadable("The save did not fit this version of the game: " + e.getMessage());
        }
    }

    /** Deletes the save slot; used by "new game" when the player confirms an overwrite. */
    public void delete() {
        try {
            Files.deleteIfExists(slotFile());
        } catch (IOException ignored) {
            // Nothing useful to do: the next save overwrites it anyway.
        }
    }

    /** Result of writing a save. */
    public sealed interface SaveOutcome {
        record Saved(Path file) implements SaveOutcome {
        }

        record Failed(String reason) implements SaveOutcome {
        }

        default boolean succeeded() {
            return this instanceof Saved;
        }
    }

    /** Result of reading a save. */
    public sealed interface LoadOutcome {
        record Loaded(GameSession session, List<String> migrationsApplied) implements LoadOutcome {
        }

        record NoSave() implements LoadOutcome {
        }

        /** The file exists but could not be parsed. */
        record Unreadable(String reason) implements LoadOutcome {
        }

        /** The file parsed but describes an impossible game. */
        record Invalid(List<String> problems) implements LoadOutcome {
        }
    }
}
