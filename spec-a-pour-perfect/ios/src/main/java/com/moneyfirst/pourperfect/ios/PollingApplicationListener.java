package com.moneyfirst.pourperfect.ios;

import com.badlogic.gdx.ApplicationListener;
import com.moneyfirst.pourperfect.PourPerfectGame;

/**
 * Delegates every {@link ApplicationListener} call to the wrapped {@link PourPerfectGame},
 * additionally polling {@link IosAdsService}'s native ad-completion flags once per frame before
 * each render. This keeps the per-frame polling need entirely inside the {@code ios} module -
 * {@code core}'s {@code AdsService} interface stays exactly as specified in
 * {@code docs/MONEY_FIRST_ARCHITECTURE.md}, with no platform-specific update/poll method added to
 * it just to satisfy one platform's async-callback plumbing.
 */
final class PollingApplicationListener implements ApplicationListener {

    private final PourPerfectGame game;
    private final IosAdsService adsService;

    PollingApplicationListener(PourPerfectGame game, IosAdsService adsService) {
        this.game = game;
        this.adsService = adsService;
    }

    @Override
    public void create() {
        game.create();
    }

    @Override
    public void resize(int width, int height) {
        game.resize(width, height);
    }

    @Override
    public void render() {
        adsService.pollNativeCallbacks();
        game.render();
    }

    @Override
    public void pause() {
        game.pause();
    }

    @Override
    public void resume() {
        game.resume();
    }

    @Override
    public void dispose() {
        game.dispose();
    }
}
