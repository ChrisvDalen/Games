package com.keplersharvest.lwjgl3;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Command-line parsing for the smoke-test flag.
 *
 * <p>Pure logic sitting in front of a window, so it is worth pinning down here rather than finding
 * out on a build machine that {@code --smoke-test} silently did nothing.
 */
class DesktopLauncherTest {

    @Test
    @DisplayName("no flag means a normal interactive run")
    void noFlagMeansNoSmokeTest() {
        assertEquals(0, DesktopLauncher.smokeTestSeconds(new String[0]));
        assertEquals(0, DesktopLauncher.smokeTestSeconds(new String[] {"--windowed", "--debug"}));
    }

    @Test
    @DisplayName("the flag on its own uses the default duration")
    void bareFlagUsesTheDefault() {
        assertEquals(DesktopLauncher.DEFAULT_SMOKE_TEST_SECONDS,
                DesktopLauncher.smokeTestSeconds(new String[] {"--smoke-test"}));
    }

    @Test
    @DisplayName("a duration after the flag is honoured")
    void durationIsRead() {
        assertEquals(12, DesktopLauncher.smokeTestSeconds(new String[] {"--smoke-test", "12"}));
        assertEquals(3, DesktopLauncher.smokeTestSeconds(new String[] {"--debug", "--smoke-test", "3"}));
    }

    @Test
    @DisplayName("a non-numeric or non-positive duration falls back to something runnable")
    void nonsenseDurationsFallBack() {
        assertEquals(DesktopLauncher.DEFAULT_SMOKE_TEST_SECONDS,
                DesktopLauncher.smokeTestSeconds(new String[] {"--smoke-test", "--verbose"}));
        assertEquals(1, DesktopLauncher.smokeTestSeconds(new String[] {"--smoke-test", "0"}));
        assertEquals(1, DesktopLauncher.smokeTestSeconds(new String[] {"--smoke-test", "-4"}));
    }
}
