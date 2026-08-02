package com.keplersharvest.quests;

/**
 * The slice of world state objectives are allowed to read.
 *
 * <p>Objectives that describe a *condition* (hold 5 alloy, have the recycler online) poll this view,
 * so they stay correct even if the player drops or spends things. Objectives that describe an *act*
 * (harvest 3 pods) accumulate from events instead.
 */
public interface QuestWorldView {

    int itemCount(String itemId);

    boolean moduleOnline(String moduleId);

    boolean landmarkDiscovered(String landmarkId);

    boolean crewLogFound(String logId);

    int crewLogsFound();

    int relationshipPoints(String colonistId);
}
