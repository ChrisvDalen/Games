package com.keplersharvest.farming;

import com.keplersharvest.testing.TestContent;
import com.keplersharvest.world.GridPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FarmLandTest {

    private static final GridPoint BED = new GridPoint(5, 5);

    private FarmLand land;

    @BeforeEach
    void setUp() {
        land = new FarmLand("terrace", TestContent.load().crops(), point -> true, new Random(7));
    }

    private void plantLumenPod() {
        assertInstanceOf(FarmResult.Tilled.class, land.till(BED));
        assertInstanceOf(FarmResult.Planted.class, land.plant(BED, "lumen_pod"));
    }

    @Test
    @DisplayName("seed will not go into untilled ground")
    void plantingNeedsTilledSoil() {
        FarmResult result = land.plant(BED, "lumen_pod");

        assertInstanceOf(FarmResult.Rejected.class, result);
        assertFalse(land.plotAt(BED).map(FarmPlot::hasCrop).orElse(false));
    }

    @Test
    @DisplayName("a tile cannot be tilled twice or double-planted")
    void noDuplicateWork() {
        plantLumenPod();

        assertInstanceOf(FarmResult.Rejected.class, land.till(BED));
        assertInstanceOf(FarmResult.Rejected.class, land.plant(BED, "glassroot"));
    }

    @Test
    @DisplayName("watering only counts once per day and dries out overnight")
    void wateringIsDaily() {
        plantLumenPod();

        assertInstanceOf(FarmResult.Watered.class, land.water(BED));
        assertInstanceOf(FarmResult.Rejected.class, land.water(BED));

        land.advanceDay();

        assertFalse(land.plotAt(BED).orElseThrow().watered(), "soil dries overnight");
        assertInstanceOf(FarmResult.Watered.class, land.water(BED));
    }

    @Test
    @DisplayName("a watered crop matures after its configured number of days")
    void growsOverMultipleDays() {
        plantLumenPod();
        int daysToMaturity = TestContent.load().crop("lumen_pod").orElseThrow().daysToMaturity();
        assertEquals(3, daysToMaturity, "Lumen Pod is the fast starter crop");

        for (int day = 1; day <= daysToMaturity; day++) {
            land.water(BED);
            assertFalse(land.readyToHarvest(BED), "not ready before day " + daysToMaturity);
            land.advanceDay();
        }

        assertTrue(land.readyToHarvest(BED));
        assertEquals(daysToMaturity, land.plotAt(BED).orElseThrow().grownDays());
    }

    @Test
    @DisplayName("crops with different growth times mature at different times")
    void cropsHaveDistinctGrowthTimes() {
        var crops = TestContent.load().crops();
        assertEquals(3, crops.get("lumen_pod").daysToMaturity());
        assertEquals(5, crops.get("glassroot").daysToMaturity());
        assertEquals(4, crops.get("cinder_moss").daysToMaturity());
        assertEquals(6, crops.get("orbit_berry").daysToMaturity());
    }

    @Test
    @DisplayName("an unwatered crop does not grow")
    void neglectStopsGrowth() {
        plantLumenPod();

        land.advanceDay();
        land.advanceDay();

        assertEquals(0, land.plotAt(BED).orElseThrow().grownDays());
        assertEquals(2, land.plotAt(BED).orElseThrow().dryDays());
    }

    @Test
    @DisplayName("prolonged neglect kills the plant")
    void neglectEventuallyWithers() {
        assertInstanceOf(FarmResult.Tilled.class, land.till(BED));
        assertInstanceOf(FarmResult.Planted.class, land.plant(BED, "glassroot"));
        int tolerance = TestContent.load().crop("glassroot").orElseThrow().witherAfterDryDays();

        for (int day = 0; day < tolerance; day++) {
            land.advanceDay();
        }

        assertTrue(land.plotAt(BED).orElseThrow().withered());
        assertInstanceOf(FarmResult.Rejected.class, land.water(BED));
        assertInstanceOf(FarmResult.Cleared.class, land.harvest(BED), "a dead plant can be pulled up");
        assertEquals(SoilState.TILLED, land.plotAt(BED).orElseThrow().soil(), "the bed survives");
    }

    @Test
    @DisplayName("a crop that needs no water grows on neglect")
    void droughtCropIgnoresWater() {
        land.till(BED);
        land.plant(BED, "cinder_moss");

        for (int day = 0; day < 4; day++) {
            land.advanceDay();
        }

        assertTrue(land.readyToHarvest(BED));
        assertFalse(land.plotAt(BED).orElseThrow().withered());
    }

    @Test
    @DisplayName("harvesting an immature crop is refused and leaves it standing")
    void cannotHarvestEarly() {
        plantLumenPod();
        land.water(BED);
        land.advanceDay();

        assertInstanceOf(FarmResult.Rejected.class, land.harvest(BED));
        assertTrue(land.plotAt(BED).orElseThrow().hasCrop());
    }

    @Test
    @DisplayName("harvesting a one-shot crop yields produce and clears the bed")
    void harvestClearsSingleCrop() {
        plantLumenPod();
        for (int day = 0; day < 3; day++) {
            land.water(BED);
            land.advanceDay();
        }

        FarmResult result = land.harvest(BED);

        FarmResult.Harvested harvested = assertInstanceOf(FarmResult.Harvested.class, result);
        assertEquals("lumen_pod", harvested.produceItemId());
        assertTrue(harvested.amount() >= 1 && harvested.amount() <= 2);
        assertFalse(harvested.regrew());
        assertFalse(land.plotAt(BED).orElseThrow().hasCrop());
        assertEquals(SoilState.TILLED, land.plotAt(BED).orElseThrow().soil());
    }

    @Test
    @DisplayName("a regrowing crop bears again after its regrow period")
    void regrowingCropBearsAgain() {
        land.till(BED);
        land.plant(BED, "orbit_berry");
        CropDefinition berry = TestContent.load().crop("orbit_berry").orElseThrow();

        for (int day = 0; day < berry.daysToMaturity(); day++) {
            land.water(BED);
            land.advanceDay();
        }

        FarmResult.Harvested first = assertInstanceOf(FarmResult.Harvested.class, land.harvest(BED));
        assertTrue(first.regrew());
        assertTrue(land.plotAt(BED).orElseThrow().hasCrop(), "the vine stays in the ground");
        assertFalse(land.readyToHarvest(BED));

        for (int day = 0; day < berry.regrowDays(); day++) {
            land.water(BED);
            land.advanceDay();
        }

        assertTrue(land.readyToHarvest(BED), "a second crop after " + berry.regrowDays() + " days");
    }

    @Test
    @DisplayName("ground the map does not mark as farmable cannot be tilled")
    void respectsFarmableGround() {
        FarmLand rock = new FarmLand("colony", TestContent.load().crops(), point -> false, new Random(1));

        assertInstanceOf(FarmResult.Rejected.class, rock.till(BED));
    }

    @Test
    @DisplayName("growth stages are spread across the crop's lifetime")
    void stagesAdvanceWithGrowth() {
        CropDefinition berry = TestContent.load().crop("orbit_berry").orElseThrow();

        assertEquals(0, berry.stageFor(0));
        assertEquals(0, berry.stageFor(1));
        assertEquals(1, berry.stageFor(2));
        assertEquals(2, berry.stageFor(4));
        assertEquals(2, berry.stageFor(99), "the last stage is the ceiling");
    }
}
