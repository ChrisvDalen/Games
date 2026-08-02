package com.keplersharvest.lwjgl3;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.keplersharvest.KeplersHarvestGame;

/**
 * Desktop entry point.
 *
 * <p>Pass {@code --smoke-test [seconds]} to boot the game, render for a few seconds and exit. That
 * is how a build machine checks the window, content loading and renderer all come up, since the
 * unit tests deliberately never open a window.
 */
public final class DesktopLauncher {

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

    private static int smokeTestSeconds(String[] args) {
        for (int i = 0; i < args.length; i++) {
            if (!"--smoke-test".equals(args[i])) {
                continue;
            }
            if (i + 1 < args.length) {
                try {
                    return Math.max(1, Integer.parseInt(args[i + 1]));
                } catch (NumberFormatException ignored) {
                    // Fall through to the default below.
                }
            }
            return 6;
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
                com.badlogic.gdx.Input.Keys.I,
                com.badlogic.gdx.Input.Keys.ESCAPE,
                com.badlogic.gdx.Input.Keys.Q,
                com.badlogic.gdx.Input.Keys.ESCAPE,
                com.badlogic.gdx.Input.Keys.J,
                com.badlogic.gdx.Input.Keys.ESCAPE,
                com.badlogic.gdx.Input.Keys.ESCAPE,
                com.badlogic.gdx.Input.Keys.ESCAPE,
                // Leave the pack open so the screenshot shows an overlay as well as the world.
                com.badlogic.gdx.Input.Keys.I,
            };
            try {
                Thread.sleep(step);
                // One shot of the world and HUD before any panel covers them.
                com.badlogic.gdx.Gdx.app.postRunnable(() -> captureScreenshot("smoke-world.png"));
                for (int key : panelKeys) {
                    Thread.sleep(step);
                    press(key);
                }
                Thread.sleep(step);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
            com.badlogic.gdx.Gdx.app.postRunnable(() -> {
                captureScreenshot("smoke-test.png");
                System.out.println("Smoke test: ran for ~" + seconds + "s without error, exiting.");
                com.badlogic.gdx.Gdx.app.exit();
            });
        }, "smoke-test-timer");
        timer.setDaemon(true);
        timer.start();
    }

    private static void press(int keycode) {
        com.badlogic.gdx.Gdx.app.postRunnable(() -> {
            com.badlogic.gdx.InputProcessor processor = com.badlogic.gdx.Gdx.input.getInputProcessor();
            if (processor != null) {
                processor.keyDown(keycode);
                processor.keyUp(keycode);
            }
        });
    }

    /** Writes a PNG beside the working directory so a build can eyeball what was rendered. */
    private static void captureScreenshot(String fileName) {
        try {
            com.badlogic.gdx.graphics.Pixmap pixmap = com.badlogic.gdx.graphics.Pixmap.createFromFrameBuffer(
                    0, 0,
                    com.badlogic.gdx.Gdx.graphics.getBackBufferWidth(),
                    com.badlogic.gdx.Gdx.graphics.getBackBufferHeight());
            com.badlogic.gdx.graphics.PixmapIO.writePNG(
                    com.badlogic.gdx.Gdx.files.local(fileName), pixmap, 6, true);
            pixmap.dispose();
            System.out.println("Smoke test: wrote " + fileName);
        } catch (RuntimeException e) {
            System.out.println("Smoke test: could not capture a screenshot - " + e.getMessage());
        }
    }
}
