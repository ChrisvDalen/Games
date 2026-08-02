package com.keplersharvest.configuration;

/**
 * Reads text content (JSON configuration, maps) from wherever the game keeps its assets.
 *
 * <p>This exists so that every content-driven system can be exercised in a unit test without
 * booting libGDX: tests use {@link FileResourceReader}, the running game uses
 * {@code GdxResourceReader}.
 */
public interface ResourceReader {

    /** Returns the full text of {@code path}, relative to the assets root. */
    String readText(String path);

    /** Returns {@code true} when {@code path} exists and can be read. */
    boolean exists(String path);
}
