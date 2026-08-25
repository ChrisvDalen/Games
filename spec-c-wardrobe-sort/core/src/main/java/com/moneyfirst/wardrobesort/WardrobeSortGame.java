package com.moneyfirst.wardrobesort;

import com.badlogic.gdx.Game;
import com.moneyfirst.wardrobesort.screens.HubScreen;
import com.moneyfirst.wardrobesort.screens.ResultsScreen;
import com.moneyfirst.wardrobesort.screens.RoundScreen;
import com.moneyfirst.wardrobesort.screens.ShopScreen;

import java.util.Objects;

/**
 * Top-level libGDX {@link Game}: owns the shared session state (current
 * round/seed, timer, avatar, hint balance, ad pacing) and switches between
 * the hub, round, results, and shop screens. {@link AdsService} and
 * {@link IapService} are constructor-injected so platform launchers wire in
 * their real implementations and core stays fully unit-testable in
 * isolation from this class (which itself needs a live OpenGL context and
 * so is not JUnit-tested directly - see {@code core/src/test} for the tested
 * pure-logic classes this class composes).
 */
public final class WardrobeSortGame extends Game {

    private final AdsService adsService;
    private final IapService iapService;

    private final RoundGenerator roundGenerator = RoundGenerator.withBaseCatalog();
    private final AdPacingPolicy adPacingPolicy = new AdPacingPolicy();
    private final HintSystem hintSystem = new HintSystem(1);

    private final long seed;
    private int roundNumber = 1;

    private Round currentRound;
    private RoundTimer currentTimer;
    private AvatarState currentAvatarState;

    public WardrobeSortGame(AdsService adsService, IapService iapService) {
        this(adsService, iapService, System.currentTimeMillis());
    }

    /** Seed-injecting constructor, mainly for deterministic integration testing of the wiring itself. */
    public WardrobeSortGame(AdsService adsService, IapService iapService, long seed) {
        this.adsService = Objects.requireNonNull(adsService, "adsService");
        this.iapService = Objects.requireNonNull(iapService, "iapService");
        this.seed = seed;
    }

    @Override
    public void create() {
        adsService.loadInterstitial();
        adsService.loadRewarded();
        goToHub();
    }

    public void goToHub() {
        adsService.showBanner();
        setScreen(new HubScreen(this));
    }

    public void goToShop() {
        setScreen(new ShopScreen(this));
    }

    /** Begins (or resumes, on retry) the current round number. */
    public void startRound() {
        adsService.hideBanner();
        currentRound = roundGenerator.generateRound(roundNumber, seed);
        currentTimer = new RoundTimer(currentRound.timerSeconds());
        currentAvatarState = new AvatarState();
        hintSystem.resetForRound();
        setScreen(new RoundScreen(this, currentRound, currentTimer, currentAvatarState));
    }

    /**
     * Called by {@link RoundScreen} when a round ends (win or timeout). Runs
     * the interstitial-after-every-round ad pacing policy, then advances to
     * the results screen either way.
     */
    public void onRoundFinished(boolean won) {
        boolean shouldShowInterstitial = adPacingPolicy.onRoundCompleted(roundNumber);
        if (shouldShowInterstitial) {
            adsService.showInterstitialIfLoaded(() -> {
                adsService.loadInterstitial();
                goToResults(won);
            });
        } else {
            goToResults(won);
        }
    }

    private void goToResults(boolean won) {
        setScreen(new ResultsScreen(this, won, roundNumber));
    }

    /** Called from the results screen's "next round" action. */
    public void advanceToNextRound() {
        roundNumber++;
        startRound();
    }

    public void requestRewardedExtraTime(Runnable onGranted) {
        if (!adPacingPolicy.canRequestRewarded(RewardedPurpose.EXTRA_TIME) || currentTimer == null) {
            return;
        }
        adsService.showRewardedIfLoaded(
            () -> {
                if (currentTimer.extend()) {
                    onGranted.run();
                }
                adsService.loadRewarded();
            },
            adsService::loadRewarded);
    }

    public void requestRewardedHint(Runnable onGranted) {
        if (!adPacingPolicy.canRequestRewarded(RewardedPurpose.OUTFIT_HINT)) {
            return;
        }
        adsService.showRewardedIfLoaded(
            () -> {
                hintSystem.grantHints(1);
                onGranted.run();
                adsService.loadRewarded();
            },
            adsService::loadRewarded);
    }

    public AdsService adsService() {
        return adsService;
    }

    public IapService iapService() {
        return iapService;
    }

    public RoundGenerator roundGenerator() {
        return roundGenerator;
    }

    public AdPacingPolicy adPacingPolicy() {
        return adPacingPolicy;
    }

    public HintSystem hintSystem() {
        return hintSystem;
    }

    public int roundNumber() {
        return roundNumber;
    }

    public long seed() {
        return seed;
    }

    public Round currentRound() {
        return currentRound;
    }
}
