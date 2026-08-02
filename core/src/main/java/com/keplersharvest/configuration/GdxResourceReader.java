package com.keplersharvest.configuration;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;

/** {@link ResourceReader} backed by libGDX internal files. Only usable once the app is running. */
public final class GdxResourceReader implements ResourceReader {

    @Override
    public String readText(String path) {
        FileHandle handle = Gdx.files.internal(path);
        if (!handle.exists()) {
            throw new ConfigurationException("Missing asset: " + path);
        }
        return handle.readString("UTF-8");
    }

    @Override
    public boolean exists(String path) {
        return Gdx.files.internal(path).exists();
    }
}
