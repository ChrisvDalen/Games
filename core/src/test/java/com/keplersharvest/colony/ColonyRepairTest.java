package com.keplersharvest.colony;

import com.keplersharvest.configuration.GameContent;
import com.keplersharvest.inventory.Inventory;
import com.keplersharvest.testing.TestContent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ColonyRepairTest {

    private GameContent content;
    private ColonyState colony;
    private Inventory inventory;

    @BeforeEach
    void setUp() {
        content = TestContent.load();
        colony = new ColonyState(content.modules().values());
        inventory = new Inventory(30);
    }

    private void stock(Map<String, Integer> materials) {
        materials.forEach((itemId, count) -> inventory.add(content.requireItem(itemId), count));
    }

    private RepairStage stageOf(String moduleId, int index) {
        return content.module(moduleId).orElseThrow().stage(index);
    }

    @Test
    @DisplayName("every module ships with more than one repair stage")
    void modulesHaveMultipleStages() {
        colony.modules().forEach(module ->
                assertTrue(module.definition().stageCount() >= 2,
                        module.id() + " should take more than one trip"));
    }

    @Test
    @DisplayName("a repair with nothing in the pack reports exactly what is missing")
    void reportsMissingMaterials() {
        RepairResult result = colony.repairNextStage("oxygen_recycler", inventory);

        RepairResult.MissingMaterials missing =
                assertInstanceOf(RepairResult.MissingMaterials.class, result);
        assertEquals(stageOf("oxygen_recycler", 0).materials(), missing.missing());
        assertEquals(0, colony.module("oxygen_recycler").orElseThrow().completedStages());
    }

    @Test
    @DisplayName("a partially stocked repair consumes nothing")
    void partialMaterialsAreNotConsumed() {
        inventory.add(content.requireItem("scrap_alloy"), 4);

        RepairResult result = colony.repairNextStage("oxygen_recycler", inventory);

        assertInstanceOf(RepairResult.MissingMaterials.class, result);
        assertEquals(4, inventory.count("scrap_alloy"), "materials stay in the pack until a stage can be paid for");
    }

    @Test
    @DisplayName("completing every stage brings the module online and pays its materials")
    void stagesCompleteInOrder() {
        ModuleDefinition definition = content.module("oxygen_recycler").orElseThrow();
        for (int index = 0; index < definition.stageCount(); index++) {
            stock(definition.stage(index).materials());
            RepairResult result = colony.repairNextStage("oxygen_recycler", inventory);

            RepairResult.StageCompleted completed =
                    assertInstanceOf(RepairResult.StageCompleted.class, result);
            assertEquals(index, completed.stageIndex());
            assertEquals(index == definition.stageCount() - 1, completed.moduleOnline());
        }

        assertTrue(colony.isOnline("oxygen_recycler"));
        assertTrue(inventory.isEmpty(), "the repair consumed exactly what it asked for");
    }

    @Test
    @DisplayName("a finished module refuses further work")
    void finishedModuleIsLeftAlone() {
        ModuleDefinition definition = content.module("oxygen_recycler").orElseThrow();
        for (int index = 0; index < definition.stageCount(); index++) {
            stock(definition.stage(index).materials());
            colony.repairNextStage("oxygen_recycler", inventory);
        }
        stock(definition.stage(0).materials());

        RepairResult result = colony.repairNextStage("oxygen_recycler", inventory);

        assertInstanceOf(RepairResult.AlreadyOnline.class, result);
        assertEquals(4, inventory.count("scrap_alloy"), "nothing is consumed by a redundant repair");
    }

    @Test
    @DisplayName("an unknown module id is reported rather than silently ignored")
    void unknownModuleIsReported() {
        assertInstanceOf(RepairResult.UnknownModule.class, colony.repairNextStage("warp_core", inventory));
    }

    @Test
    @DisplayName("benefits only apply once the module is fully online")
    void benefitsRequireCompletion() {
        assertEquals(0, colony.maxEnergyBonus());
        ModuleDefinition definition = content.module("oxygen_recycler").orElseThrow();

        stock(definition.stage(0).materials());
        colony.repairNextStage("oxygen_recycler", inventory);
        assertEquals(0, colony.maxEnergyBonus(), "a half-repaired recycler gives nothing");

        stock(definition.stage(1).materials());
        colony.repairNextStage("oxygen_recycler", inventory);

        assertEquals(definition.benefitAmount(), colony.maxEnergyBonus());
        assertTrue(colony.hasBenefit(BenefitKind.MAX_ENERGY));
        assertFalse(colony.hasBenefit(BenefitKind.SURVEY), "the dish is still down");
    }

    @Test
    @DisplayName("progress is reported as a fraction for the renderer")
    void progressTracksStages() {
        ColonyModule module = colony.module("comms_array").orElseThrow();
        assertEquals(0f, module.progress());

        stock(stageOf("comms_array", 0).materials());
        colony.repairNextStage("comms_array", inventory);

        assertEquals(1f / 3f, module.progress(), 0.0001f);
        assertFalse(module.online());
    }
}
