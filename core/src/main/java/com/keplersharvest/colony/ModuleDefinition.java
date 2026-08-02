package com.keplersharvest.colony;

import java.util.List;
import java.util.Objects;

/**
 * A repairable colony module, loaded from {@code config/modules.json}.
 *
 * <p>Placement lives in the map file, not here - a module is a rule set, and the map decides where
 * it stands. Adding a fourth module is a JSON edit plus a map object.
 *
 * @param grantsEvidence evidence ids added to the journal when the module comes back online
 */
public record ModuleDefinition(
        String id,
        String name,
        String description,
        List<RepairStage> stages,
        BenefitKind benefitKind,
        int benefitAmount,
        String benefitText,
        List<String> grantsEvidence,
        String colour) {

    public ModuleDefinition {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(benefitKind, "benefitKind");
        Objects.requireNonNull(benefitText, "benefitText");
        Objects.requireNonNull(colour, "colour");
        stages = List.copyOf(Objects.requireNonNull(stages, "stages"));
        grantsEvidence = List.copyOf(Objects.requireNonNull(grantsEvidence, "grantsEvidence"));
        if (stages.size() < 2) {
            throw new IllegalArgumentException("Module " + id + " must have multiple repair stages");
        }
    }

    public int stageCount() {
        return stages.size();
    }

    public RepairStage stage(int index) {
        return stages.get(index);
    }
}
