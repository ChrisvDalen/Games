package com.keplersharvest.colony;

import com.keplersharvest.inventory.Inventory;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Repair progress across every colony module, and the benefits those repairs unlock.
 *
 * <p>Materials are only consumed when a whole stage can be paid for, so a partial inventory never
 * disappears into a half-finished repair.
 */
public final class ColonyState {

    private final Map<String, ColonyModule> modules = new LinkedHashMap<>();

    public ColonyState(Collection<ModuleDefinition> definitions) {
        Objects.requireNonNull(definitions, "definitions")
                .forEach(def -> modules.put(def.id(), new ColonyModule(def)));
    }

    public Collection<ColonyModule> modules() {
        return modules.values();
    }

    public Optional<ColonyModule> module(String moduleId) {
        return Optional.ofNullable(modules.get(moduleId));
    }

    public boolean isOnline(String moduleId) {
        return module(moduleId).map(ColonyModule::online).orElse(false);
    }

    public long onlineCount() {
        return modules.values().stream().filter(ColonyModule::online).count();
    }

    /** Attempts the next repair stage, paying for it out of {@code inventory}. */
    public RepairResult repairNextStage(String moduleId, Inventory inventory) {
        ColonyModule module = modules.get(moduleId);
        if (module == null) {
            return new RepairResult.UnknownModule(moduleId);
        }
        if (module.online()) {
            return new RepairResult.AlreadyOnline(moduleId);
        }
        RepairStage stage = module.nextStage().orElseThrow();
        Map<String, Integer> missing = inventory.missingFrom(stage.materials());
        if (!missing.isEmpty()) {
            return new RepairResult.MissingMaterials(moduleId, missing);
        }
        inventory.removeAll(stage.materials());
        module.completeStage();
        return new RepairResult.StageCompleted(moduleId, stage.index(), stage.name(), module.online());
    }

    /** Extra maximum energy contributed by online modules. */
    public int maxEnergyBonus() {
        return modules.values().stream()
                .filter(ColonyModule::online)
                .filter(m -> m.definition().benefitKind() == BenefitKind.MAX_ENERGY)
                .mapToInt(m -> m.definition().benefitAmount())
                .sum();
    }

    public boolean hasBenefit(BenefitKind kind) {
        return modules.values().stream()
                .filter(ColonyModule::online)
                .anyMatch(m -> m.definition().benefitKind() == kind);
    }
}
