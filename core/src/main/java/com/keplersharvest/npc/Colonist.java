package com.keplersharvest.npc;

import com.keplersharvest.time.DayPhase;
import com.keplersharvest.world.GridPoint;

import java.util.Objects;
import java.util.function.Predicate;

/** A colonist as they exist in the running world: definition plus their current position. */
public final class Colonist {

    private final ColonistDefinition definition;
    private String mapId;
    private GridPoint position;
    private int lastGreetedDay;

    public Colonist(ColonistDefinition definition, DayPhase phase, Predicate<String> moduleOnline) {
        this.definition = Objects.requireNonNull(definition, "definition");
        applySchedule(phase, moduleOnline);
    }

    public ColonistDefinition definition() {
        return definition;
    }

    public String id() {
        return definition.id();
    }

    public String name() {
        return definition.name();
    }

    public String mapId() {
        return mapId;
    }

    public GridPoint position() {
        return position;
    }

    /** Moves the colonist to wherever their schedule puts them for this phase. */
    public void applySchedule(DayPhase phase, Predicate<String> moduleOnline) {
        ScheduleEntry entry = definition.locationDuring(phase, moduleOnline);
        this.mapId = entry.mapId();
        this.position = entry.position();
    }

    /** Day of the last first-greeting, used to hand out the daily conversation bonus only once. */
    public int lastGreetedDay() {
        return lastGreetedDay;
    }

    public boolean greetedToday(int day) {
        return lastGreetedDay == day;
    }

    public void markGreeted(int day) {
        this.lastGreetedDay = day;
    }

    public void restoreGreeting(int day) {
        this.lastGreetedDay = day;
    }
}
