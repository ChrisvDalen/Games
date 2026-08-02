package com.keplersharvest.save;

import java.util.ArrayList;

/**
 * The on-disk shape of a save file.
 *
 * <p>Deliberately dumb public fields with no-argument constructors: libGDX's {@code Json} writes and
 * reads these by reflection, and keeping them free of behaviour means the save format can evolve
 * independently of the domain classes it mirrors.
 */
public final class SaveData {

    /** Bumped whenever the format changes; see {@link SaveMigrations}. */
    public static final int CURRENT_VERSION = 1;

    public int version = CURRENT_VERSION;
    public long seed;
    public String savedAt = "";
    public String gameVersion = "";

    public int day = 1;
    public int minuteOfDay;

    public String mapId = "";
    public float playerX;
    public float playerY;
    public String facing = "DOWN";
    public int energy;

    public int toolbarIndex;
    public ArrayList<SlotEntry> inventory = new ArrayList<>();
    public ArrayList<PlotEntry> plots = new ArrayList<>();
    public ArrayList<ModuleEntry> modules = new ArrayList<>();
    public ArrayList<CountEntry> relationships = new ArrayList<>();
    public ArrayList<CountEntry> greetings = new ArrayList<>();
    public ArrayList<QuestEntry> quests = new ArrayList<>();
    public ArrayList<CountEntry> depletedNodes = new ArrayList<>();
    public ArrayList<TextEntry> landmarks = new ArrayList<>();
    public ArrayList<String> consumedObjects = new ArrayList<>();
    public ArrayList<String> flags = new ArrayList<>();
    public ArrayList<String> foundLogs = new ArrayList<>();
    public ArrayList<String> evidence = new ArrayList<>();
    public boolean revealSeen;

    /** One occupied inventory slot. */
    public static final class SlotEntry {
        public int slot;
        public String item = "";
        public int count;

        public SlotEntry() {
        }

        public SlotEntry(int slot, String item, int count) {
            this.slot = slot;
            this.item = item;
            this.count = count;
        }
    }

    /** One cultivated tile. */
    public static final class PlotEntry {
        public String map = "";
        public int x;
        public int y;
        public String soil = "WILD";
        public String crop = "";
        public int grownDays;
        public int dryDays;
        public boolean watered;
        public boolean withered;

        public PlotEntry() {
        }
    }

    public static final class ModuleEntry {
        public String id = "";
        public int completedStages;

        public ModuleEntry() {
        }

        public ModuleEntry(String id, int completedStages) {
            this.id = id;
            this.completedStages = completedStages;
        }
    }

    public static final class QuestEntry {
        public String id = "";
        public String status = "NOT_STARTED";
        public ArrayList<Integer> counters = new ArrayList<>();

        public QuestEntry() {
        }
    }

    /** A string key with an integer value, used for several small maps. */
    public static final class CountEntry {
        public String key = "";
        public int value;

        public CountEntry() {
        }

        public CountEntry(String key, int value) {
            this.key = key;
            this.value = value;
        }
    }

    /** A string key with a string value. */
    public static final class TextEntry {
        public String key = "";
        public String value = "";

        public TextEntry() {
        }

        public TextEntry(String key, String value) {
            this.key = key;
            this.value = value;
        }
    }
}
