package com.keplersharvest.lwjgl3;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;
import com.keplersharvest.KeplersHarvestGame;

/**
 * Desktop entry point.
 *
 * <p>Pass {@code --smoke-test [seconds]} to boot the game, render for a few seconds and exit. That
 * is how a build machine checks the window, content loading and renderer all come up, since the
 * unit tests deliberately never open a window.
 */
public final class DesktopLauncher {

    /** Seconds a {@code --smoke-test} run lasts when no duration follows the flag. */
    static final int DEFAULT_SMOKE_TEST_SECONDS = 6;

    private DesktopLauncher() {
    }

    public static void main(String[] args) {
        int smokeTestSeconds = smokeTestSeconds(args);
        if (smokeTestSeconds > 0) {
            startExitTimer(smokeTestSeconds);
        }

        Lwjgl3ApplicationConfiguration configuration = new Lwjgl3ApplicationConfiguration();
        configuration.setTitle(KeplersHarvestGame.TITLE);
        configuration.setWindowedMode(1280, 720);
        configuration.setWindowSizeLimits(960, 540, -1, -1);
        configuration.useVsync(true);
        configuration.setForegroundFPS(60);
        configuration.setBackBufferConfig(8, 8, 8, 8, 16, 0, 0);
        new Lwjgl3Application(new KeplersHarvestGame(smokeTestSeconds > 0), configuration);
    }

    /**
     * Reads the smoke-test duration out of the command line.
     *
     * @return the number of seconds to run for, or 0 when the flag is absent
     */
    static int smokeTestSeconds(String[] args) {
        for (int i = 0; i < args.length; i++) {
            if (!"--smoke-test".equals(args[i])) {
                continue;
            }
            if (i + 1 < args.length) {
                try {
                    return Math.max(1, Integer.parseInt(args[i + 1]));
                } catch (NumberFormatException ignored) {
                    // Not a number, so it is the next argument rather than a duration.
                }
            }
            return DEFAULT_SMOKE_TEST_SECONDS;
        }
        return 0;
    }

    /**
     * Runs the game unattended: walks a little, opens each interface panel in turn, takes a
     * screenshot and exits. Opening the panels is the point - it catches a missing skin style or a
     * broken layout, which the windowless unit tests cannot see.
     */
    private static void startExitTimer(int seconds) {
        Thread timer = new Thread(() -> {
            long step = Math.max(250L, seconds * 1000L / 8);
            int[] panelKeys = {
                Input.Keys.I,
                Input.Keys.ESCAPE,
                Input.Keys.Q,
                Input.Keys.ESCAPE,
                Input.Keys.J,
                Input.Keys.ESCAPE,
                Input.Keys.ESCAPE,
                Input.Keys.ESCAPE,
                // Leave the pack open so the screenshot shows an overlay as well as the world.
                Input.Keys.I,
            };
            try {
                Thread.sleep(step);
                // One shot of the world and HUD before any panel covers them.
                Gdx.app.postRunnable(() -> captureScreenshot("smoke-world.png"));
                for (int key : panelKeys) {
                    Thread.sleep(step);
                    press(key);
                }
                Thread.sleep(step);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
            Gdx.app.postRunnable(() -> {
                captureScreenshot("smoke-test.png");
                System.out.println("Smoke test: ran for ~" + seconds + "s without error, exiting.");
                Gdx.app.exit();
            });
        }, "smoke-test-timer");
        timer.setDaemon(true);
        timer.start();
    }

    private static void press(int keycode) {
        Gdx.app.postRunnable(() -> {
            InputProcessor processor = Gdx.input.getInputProcessor();
            if (processor != null) {
                processor.keyDown(keycode);
                processor.keyUp(keycode);
            }
        });
    }

    /** Writes a PNG beside the working directory so a build can eyeball what was rendered. */
    private static void captureScreenshot(String fileName) {
        try {
            Pixmap pixmap = Pixmap.createFromFrameBuffer(
                    0, 0,
                    Gdx.graphics.getBackBufferWidth(),
                    Gdx.graphics.getBackBufferHeight());
            PixmapIO.writePNG(Gdx.files.local(fileName), pixmap, 6, true);
            pixmap.dispose();
            System.out.println("Smoke test: wrote " + fileName);
        } catch (RuntimeException e) {
            System.out.println("Smoke test: could not capture a screenshot - " + e.getMessage());
        }
    }
}
