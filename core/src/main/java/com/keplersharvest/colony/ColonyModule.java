package com.keplersharvest.colony;

import java.util.Objects;

/** Live repair progress for one module. */
public final class ColonyModule {

    private final ModuleDefinition definition;
    private int completedStages;

    public ColonyModule(ModuleDefinition definition) {
        this.definition = Objects.requireNonNull(definition, "definition");
    }

    public ModuleDefinition definition() {
        return definition;
    }

    public String id() {
        return definition.id();
    }

    public int completedStages() {
        return completedStages;
    }

    public boolean online() {
        return completedStages >= definition.stageCount();
    }

    /** The stage the player would work on next, or empty when the module is finished. */
    public java.util.Optional<RepairStage> nextStage() {
        return online() ? java.util.Optional.empty() : java.util.Optional.of(definition.stage(completedStages));
    }

    /** 0.0 to 1.0, used by the HUD and by the module's rendered condition. */
    public float progress() {
        return (float) completedStages / definition.stageCount();
    }

    void completeStage() {
        if (!online()) {
            completedStages++;
        }
    }

    /** Restores progress from a save, clamped to the current definition so content can change. */
    public void restore(int completedStages) {
        this.completedStages = Math.clamp(completedStages, 0, definition.stageCount());
    }
}
