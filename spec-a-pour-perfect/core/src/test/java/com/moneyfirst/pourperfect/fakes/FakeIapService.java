package com.moneyfirst.pourperfect.fakes;

import com.moneyfirst.pourperfect.iap.IapService;
import com.moneyfirst.pourperfect.iap.PourPerfectIapCatalog;
import com.moneyfirst.pourperfect.state.LevelProgressionState;

import java.util.EnumSet;
import java.util.Set;

/** In-memory {@link IapService} test double: every {@link #purchase} "succeeds" synchronously. */
public final class FakeIapService implements IapService {

    private final Set<PourPerfectIapCatalog> owned = EnumSet.noneOf(PourPerfectIapCatalog.class);
    private final LevelProgressionState progressionState;
    public int restoreCalls;

    public FakeIapService(LevelProgressionState progressionState) {
        this.progressionState = progressionState;
    }

    @Override
    public void purchase(PourPerfectIapCatalog product) {
        owned.add(product);
        product.apply(progressionState);
    }

    @Override
    public boolean isOwned(PourPerfectIapCatalog product) {
        return owned.contains(product);
    }

    @Override
    public void restore() {
        restoreCalls++;
    }
}
