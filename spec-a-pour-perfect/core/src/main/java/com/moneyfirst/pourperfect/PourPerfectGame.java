package com.moneyfirst.pourperfect;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Game;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.moneyfirst.pourperfect.ads.AdPacingPolicy;
import com.moneyfirst.pourperfect.ads.AdsService;
import com.moneyfirst.pourperfect.cafe.CafeProgress;
import com.moneyfirst.pourperfect.iap.IapService;
import com.moneyfirst.pourperfect.screens.LevelSelectScreen;
import com.moneyfirst.pourperfect.state.LevelProgressionState;
import com.moneyfirst.pourperfect.state.LevelRepository;
import com.moneyfirst.pourperfect.state.StreakTracker;

/**
 * The libGDX {@link Game} entry point. Platform launchers (Android {@code AndroidLauncher}, iOS
 * {@code IOSLauncher}) construct this with their real {@link AdsService}/{@link IapService}
 * implementations; core unit tests construct it with fakes/mocks instead - this class and every
 * screen it owns depend only on the two interfaces, never a platform SDK class.
 */
public final class PourPerfectGame extends Game {

    private static final String PROGRESSION_PREFS_NAME = "pour_perfect_progression";
    private static final String CAFE_PREFS_NAME = "pour_perfect_cafe";

    private final AdsService adsService;
    private final IapService iapService;
    private final AdPacingPolicy adPacingPolicy = new AdPacingPolicy();

    private LevelProgressionState progressionState;
    private LevelRepository levelRepository;
    private CafeProgress cafeProgress;

    private ShapeRenderer shapeRenderer;
    private SpriteBatch batch;
    private BitmapFont font;

    public PourPerfectGame(AdsService adsService, IapService iapService) {
        this.adsService = adsService;
        this.iapService = iapService;
    }

    @Override
    public void create() {
        Preferences progressionPrefs = Gdx.app.getPreferences(PROGRESSION_PREFS_NAME);
        progressionState = LevelProgressionState.load(progressionPrefs);
        levelRepository = new LevelRepository(progressionState);
        if (iapService instanceof com.moneyfirst.pourperfect.iap.GdxPayIapService gdxPayIapService) {
            // See GdxPayIapService.attachProgressionState javadoc: it can't load its own
            // Preferences-backed state before Gdx.app exists, so the one canonical instance
            // loaded here is handed to it instead. No-op for fakes/mocks used in tests.
            gdxPayIapService.attachProgressionState(progressionState);
        }
        cafeProgress = CafeProgress.load(Gdx.app.getPreferences(CAFE_PREFS_NAME));
        cafeProgress.refreshEligibility(iapService.isOwned(com.moneyfirst.pourperfect.iap.PourPerfectIapCatalog.CAFE_EXPANSION),
                progressionState.getCurrentLevelIndex());

        StreakTracker.recordPlay(progressionPrefs, StreakTracker.todayUtc());

        shapeRenderer = new ShapeRenderer();
        batch = new SpriteBatch();
        font = new BitmapFont();

        adsService.loadInterstitial();
        adsService.loadRewarded();

        setScreen(new LevelSelectScreen(this));
    }

    public AdsService getAdsService() {
        return adsService;
    }

    public IapService getIapService() {
        return iapService;
    }

    public AdPacingPolicy getAdPacingPolicy() {
        return adPacingPolicy;
    }

    public LevelProgressionState getProgressionState() {
        return progressionState;
    }

    public LevelRepository getLevelRepository() {
        return levelRepository;
    }

    public CafeProgress getCafeProgress() {
        return cafeProgress;
    }

    public ShapeRenderer getShapeRenderer() {
        return shapeRenderer;
    }

    public SpriteBatch getBatch() {
        return batch;
    }

    public BitmapFont getFont() {
        return font;
    }

    @Override
    public void dispose() {
        super.dispose();
        if (shapeRenderer != null) {
            shapeRenderer.dispose();
        }
        if (batch != null) {
            batch.dispose();
        }
        if (font != null) {
            font.dispose();
        }
    }
}
