package com.keplersharvest.game;

/**
 * The narrative-facing slice of the game session.
 *
 * <p>Dialogue conditions and effects are written against this interface rather than against the
 * session itself, so a conversation can be driven in a unit test with a small stub.
 */
public interface StoryState {

    int day();

    int relationshipPoints(String colonistId);

    boolean questActive(String questId);

    boolean questCompleted(String questId);

    boolean questAvailable(String questId);

    boolean questNotStarted(String questId);

    boolean moduleOnline(String moduleId);

    boolean crewLogFound(String logId);

    int crewLogCount();

    boolean evidenceKnown(String evidenceId);

    boolean flagSet(String flag);

    void changeRelationship(String colonistId, int delta);

    void startQuest(String questId);

    void giveItem(String itemId, int count);

    void setFlag(String flag);

    void clearFlag(String flag);

    void grantEvidence(String evidenceId);
}
