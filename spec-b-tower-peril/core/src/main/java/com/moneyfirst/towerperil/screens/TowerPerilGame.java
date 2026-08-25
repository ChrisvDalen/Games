package com.moneyfirst.towerperil.screens;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.moneyfirst.towerperil.ads.AdPacingPolicy;
import com.moneyfirst.towerperil.ads.AdsService;
import com.moneyfirst.towerperil.economy.IapService;
import com.moneyfirst.towerperil.economy.PlayerEconomy;
import com.moneyfirst.towerperil.gacha.GachaMergeSystem;

import java.util.Random;

/**
 * Root {@link Game} for Tower Peril. Owns the shared render resources and
 * cross-screen state (economy, gacha system, ad pacing) and wires the
 * pin-pull -> auto-battle -> gacha/merge -> hub screen flow.
 *
 * <p>Platform launchers construct this with a real {@link AdsService} and
 * {@link IapService} implementation; nothing in {@code core} (including this
 * class) ever imports a platform ad or billing SDK directly.
 */
public final class TowerPerilGame extends Game {

    private final AdsService adsService;
    private final IapService iapService;

    private ShapeRenderer shapeRenderer;
    private SpriteBatch spriteBatch;
    private BitmapFont font;

    private final PlayerEconomy economy = new PlayerEconomy();
    private final GachaMergeSystem gachaMergeSystem = new GachaMergeSystem(new Random());
    private final AdPacingPolicy adPacingPolicy;

    private int levelIndex = 0;
    private static final long SEED_BASE = 20260101L;

    public TowerPerilGame(AdsService adsService, IapService iapService) {
        this.adsService = adsService;
        this.iapService = iapService;
        this.adPacingPolicy = new AdPacingPolicy(adsService);
    }

    @Override
    public void create() {
        shapeRenderer = new ShapeRenderer();
        spriteBatch = new SpriteBatch();
        font = new BitmapFont();
        adsService.loadInterstitial();
        adsService.loadRewarded();
        setScreen(new HubScreen(this));
    }

    @Override
    public void dispose() {
        super.dispose();
        if (shapeRenderer != null) {
            shapeRenderer.dispose();
        }
        if (spriteBatch != null) {
            spriteBatch.dispose();
        }
        if (font != null) {
            font.dispose();
        }
    }

    public void startNextPinPullLevel() {
        long seed = SEED_BASE + levelIndex;
        levelIndex++;
        setScreen(new PinPullScreen(this, seed));
    }

    public ShapeRenderer getShapeRenderer() {
        return shapeRenderer;
    }

    public SpriteBatch getSpriteBatch() {
        return spriteBatch;
    }

    public BitmapFont getFont() {
        return font;
    }

    public AdsService getAdsService() {
        return adsService;
    }

    public IapService getIapService() {
        return iapService;
    }

    public PlayerEconomy getEconomy() {
        return economy;
    }

    public GachaMergeSystem getGachaMergeSystem() {
        return gachaMergeSystem;
    }

    public AdPacingPolicy getAdPacingPolicy() {
        return adPacingPolicy;
    }

    public int getLevelIndex() {
        return levelIndex;
    }
}
