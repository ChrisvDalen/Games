package com.keplersharvest.testing;

import com.keplersharvest.configuration.ContentLoader;
import com.keplersharvest.configuration.FileResourceReader;
import com.keplersharvest.configuration.GameContent;
import com.keplersharvest.game.GameSession;

/**
 * Loads the real shipped content once and shares it across tests.
 *
 * <p>Tests run against the actual JSON rather than fixtures, so a content edit that breaks a rule
 * shows up as a failing test instead of at run time.
 */
public final class TestContent {

    private static GameContent cached;

    private TestContent() {
    }

    public static synchronized GameContent load() {
        if (cached == null) {
            cached = ContentLoader.loadDefault(FileResourceReader.locateAssets());
        }
        return cached;
    }

    /** A fresh session on a fixed seed, so randomised yields are reproducible. */
    public static GameSession newSession() {
        return GameSession.newGame(load(), 20260802L);
    }
}
