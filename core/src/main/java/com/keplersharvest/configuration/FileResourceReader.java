package com.keplersharvest.configuration;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/** Plain filesystem {@link ResourceReader}, used by tests and by tooling. */
public final class FileResourceReader implements ResourceReader {

    private final Path root;

    public FileResourceReader(Path root) {
        this.root = Objects.requireNonNull(root, "root");
    }

    /**
     * Locates the assets directory for a non-libGDX context. Honours the
     * {@code keplersharvest.assets} system property (set by the Gradle test task) and otherwise
     * walks up from the working directory looking for an {@code assets} folder.
     */
    public static FileResourceReader locateAssets() {
        String configured = System.getProperty("keplersharvest.assets");
        if (configured != null && !configured.isBlank()) {
            return new FileResourceReader(Path.of(configured));
        }
        Path candidate = Path.of("").toAbsolutePath();
        for (int depth = 0; depth < 5 && candidate != null; depth++) {
            Path assets = candidate.resolve("assets");
            if (Files.isDirectory(assets)) {
                return new FileResourceReader(assets);
            }
            if (Files.isDirectory(candidate.resolve("config"))) {
                return new FileResourceReader(candidate);
            }
            candidate = candidate.getParent();
        }
        throw new ConfigurationException("Could not locate the assets directory. "
                + "Set the 'keplersharvest.assets' system property.");
    }

    public Path root() {
        return root;
    }

    @Override
    public String readText(String path) {
        Path file = root.resolve(path);
        try {
            return Files.readString(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot read asset: " + file, e);
        }
    }

    @Override
    public boolean exists(String path) {
        return Files.isRegularFile(root.resolve(path));
    }
}
