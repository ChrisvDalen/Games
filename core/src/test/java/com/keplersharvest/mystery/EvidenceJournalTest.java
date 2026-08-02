package com.keplersharvest.mystery;

import com.keplersharvest.configuration.GameContent;
import com.keplersharvest.testing.TestContent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EvidenceJournalTest {

    private EvidenceJournal journal() {
        GameContent content = TestContent.load();
        return new EvidenceJournal(content.crewLogs(), content.evidence(), content.reveal());
    }

    @Test
    @DisplayName("recovering a log files the clues it establishes")
    void logsGrantEvidence() {
        EvidenceJournal journal = journal();

        assertTrue(journal.recordLog("log_intake"));

        assertTrue(journal.hasLog("log_intake"));
        assertTrue(journal.hasEvidence("weave_makes_the_air"));
        assertEquals(1, journal.foundLogCount());
    }

    @Test
    @DisplayName("a log can only be recovered once")
    void logsAreNotDuplicated() {
        EvidenceJournal journal = journal();
        journal.recordLog("log_intake");

        assertFalse(journal.recordLog("log_intake"));
        assertEquals(1, journal.foundLogCount());
    }

    @Test
    @DisplayName("unknown ids are ignored rather than corrupting the journal")
    void unknownIdsAreIgnored() {
        EvidenceJournal journal = journal();

        assertFalse(journal.recordLog("log_that_does_not_exist"));
        assertFalse(journal.recordEvidence("made_up_clue"));
        assertEquals(0, journal.foundLogCount());
    }

    @Test
    @DisplayName("logs are listed in authored reading order")
    void logsSortByOrder() {
        EvidenceJournal journal = journal();
        journal.recordLog("log_last");
        journal.recordLog("log_intake");
        journal.recordLog("log_vote");

        var titles = journal.foundLogsInOrder().stream().map(CrewLogDefinition::id).toList();

        assertEquals(java.util.List.of("log_intake", "log_vote", "log_last"), titles);
    }

    @Test
    @DisplayName("the reveal stays locked until every log and clue is in hand")
    void revealNeedsTheFullPicture() {
        EvidenceJournal journal = journal();
        RevealDefinition reveal = journal.reveal();

        for (String logId : TestContent.load().crewLogs().keySet()) {
            assertFalse(journal.revealAvailable(), "not yet - " + journal.foundLogCount() + " logs so far");
            journal.recordLog(logId);
        }

        assertEquals(reveal.requiredLogs(), journal.foundLogCount());
        assertTrue(journal.revealAvailable());
        assertTrue(journal.missingForReveal().isEmpty());
    }

    @Test
    @DisplayName("the journal names what is still missing")
    void reportsMissingClues() {
        EvidenceJournal journal = journal();
        journal.recordLog("log_intake");

        var missing = journal.missingForReveal();

        assertFalse(missing.isEmpty());
        assertFalse(missing.contains(TestContent.load().evidence().get("weave_makes_the_air").title()),
                "an established clue must not still be listed as missing");
    }

    @Test
    @DisplayName("journal state survives a restore, dropping ids that no longer exist")
    void restoreDropsUnknownContent() {
        EvidenceJournal journal = journal();

        journal.restore(Set.of("log_intake", "deleted_log"), Set.of("the_vote", "deleted_clue"), true);

        assertTrue(journal.hasLog("log_intake"));
        assertFalse(journal.hasLog("deleted_log"));
        assertTrue(journal.hasEvidence("the_vote"));
        assertFalse(journal.hasEvidence("deleted_clue"));
        assertTrue(journal.revealSeen());
        assertEquals(1, journal.foundLogCount());
    }
}
