package com.keplersharvest.colony;

import java.util.Map;

/** Outcome of attempting the next repair stage on a module. */
public sealed interface RepairResult {

    String moduleId();

    /** A stage was paid for and completed. */
    record StageCompleted(String moduleId, int stageIndex, String stageName, boolean moduleOnline)
            implements RepairResult {
    }

    /** Nothing was consumed; these amounts are still needed. */
    record MissingMaterials(String moduleId, Map<String, Integer> missing) implements RepairResult {
    }

    record AlreadyOnline(String moduleId) implements RepairResult {
    }

    record UnknownModule(String moduleId) implements RepairResult {
    }

    default boolean succeeded() {
        return this instanceof StageCompleted;
    }
}
