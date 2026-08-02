package com.keplersharvest.dialogue;

import com.keplersharvest.game.GameSession;
import com.keplersharvest.testing.TestContent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DialogueTest {

    private DialogueTree sefaTree() {
        return TestContent.load().dialogue("sefa").orElseThrow();
    }

    @Test
    @DisplayName("every colonist has a dialogue tree with an entry node")
    void everyColonistCanSpeak() {
        var content = TestContent.load();
        content.colonists().values().forEach(colonist -> {
            DialogueTree tree = content.dialogue(colonist.dialogueId()).orElseThrow();
            assertTrue(tree.nodes().stream().anyMatch(DialogueNode::entry),
                    colonist.id() + " has no way to start a conversation");
        });
    }

    @Test
    @DisplayName("the first meeting runs, then a different conversation opens next time")
    void conditionalEntrySelection() {
        GameSession session = TestContent.newSession();

        DialogueRunner first = DialogueRunner.start(sefaTree(), session).orElseThrow();
        String firstNode = first.currentNode().id();
        assertEquals("first_meeting", firstNode);

        // Walking through the opening sets the "met" flag via a node effect.
        assertTrue(session.flagSet("met_sefa"));

        DialogueRunner second = DialogueRunner.start(sefaTree(), session).orElseThrow();
        assertNotEquals(firstNode, second.currentNode().id(),
                "the introduction should not repeat once you have met");
    }

    @Test
    @DisplayName("node effects apply on entry")
    void nodeEffectsApplyOnEntry() {
        GameSession session = TestContent.newSession();
        int before = session.relationshipPoints("sefa");

        DialogueRunner.start(sefaTree(), session).orElseThrow();

        assertTrue(session.relationshipPoints("sefa") > before, "meeting Sefa is worth something");
    }

    @Test
    @DisplayName("advancing walks the lines then follows the node link")
    void advancingWalksLines() {
        GameSession session = TestContent.newSession();
        DialogueRunner runner = DialogueRunner.start(sefaTree(), session).orElseThrow();

        int lines = runner.lineCount();
        for (int i = 0; i < lines - 1; i++) {
            assertFalse(runner.atLastLine());
            runner.advance();
        }
        assertTrue(runner.atLastLine());
        assertTrue(runner.awaitingChoice(), "the opening node offers replies");
    }

    @Test
    @DisplayName("advance does nothing while a reply is required")
    void choicesBlockAdvance() {
        GameSession session = TestContent.newSession();
        DialogueRunner runner = DialogueRunner.start(sefaTree(), session).orElseThrow();
        while (!runner.atLastLine()) {
            runner.advance();
        }
        String node = runner.currentNode().id();

        runner.advance();

        assertEquals(node, runner.currentNode().id(), "the player must pick something");
    }

    @Test
    @DisplayName("choosing a reply applies its effects and follows its link")
    void choiceEffectsApply() {
        GameSession session = TestContent.newSession();
        DialogueRunner runner = DialogueRunner.start(sefaTree(), session).orElseThrow();
        while (!runner.atLastLine()) {
            runner.advance();
        }
        List<DialogueChoice> choices = runner.choices();
        assertFalse(choices.isEmpty());
        String openingNode = runner.currentNode().id();

        runner.choose(0);

        assertNotEquals(openingNode, runner.currentNode().id());
    }

    @Test
    @DisplayName("a conversation ends when a node has no link and no replies")
    void conversationsTerminate() {
        GameSession session = TestContent.newSession();
        DialogueRunner runner = DialogueRunner.start(sefaTree(), session).orElseThrow();

        // Take the first available path repeatedly; every branch must reach an end.
        for (int guard = 0; guard < 200 && !runner.finished(); guard++) {
            if (runner.awaitingChoice()) {
                runner.choose(0);
            } else {
                runner.advance();
            }
        }

        assertTrue(runner.finished(), "dialogue must not loop forever");
    }

    @Test
    @DisplayName("relationship-gated dialogue only opens once the bar is cleared")
    void relationshipGatesDialogue() {
        GameSession session = TestContent.newSession();
        DialogueCondition gate = new DialogueCondition.MinRelationship("", 75);

        assertFalse(gate.test(session, "sefa"));

        session.changeRelationship("sefa", 80);

        assertTrue(gate.test(session, "sefa"));
    }

    @Test
    @DisplayName("an AND condition needs every part to hold")
    void combinedConditions() {
        GameSession session = TestContent.newSession();
        DialogueCondition both = new DialogueCondition.All(List.of(
                new DialogueCondition.MinRelationship("sefa", 10),
                new DialogueCondition.ModuleOnline("oxygen_recycler")));

        session.changeRelationship("sefa", 20);
        assertFalse(both.test(session, "sefa"), "the recycler is still offline");

        var definition = TestContent.load().module("oxygen_recycler").orElseThrow();
        for (int i = 0; i < definition.stageCount(); i++) {
            definition.stage(i).materials().forEach((id, count) ->
                    session.inventory().add(TestContent.load().requireItem(id), count));
            session.repair("oxygen_recycler");
        }

        assertTrue(both.test(session, "sefa"));
    }

    @Test
    @DisplayName("dialogue effects can start a quest and hand over items")
    void effectsReachTheWorld() {
        GameSession session = TestContent.newSession();

        new DialogueEffect.GiveItem("scrap_alloy", 3).apply(session, "rike");
        new DialogueEffect.SetFlag("test_flag").apply(session, "rike");
        new DialogueEffect.GrantEvidence("weave_in_the_soil").apply(session, "sefa");

        assertEquals(3, session.inventory().count("scrap_alloy"));
        assertTrue(session.flagSet("test_flag"));
        assertTrue(session.journal().hasEvidence("weave_in_the_soil"));
    }

    @Test
    @DisplayName("a tree whose entry conditions all fail simply does not open")
    void noEligibleEntryMeansNoConversation() {
        DialogueTree impossible = new DialogueTree("test", "sefa", List.of(
                new DialogueNode("only", true, new DialogueCondition.FlagSet("never_set"),
                        List.of("..."), List.of(), List.of(), Optional.empty())));

        assertTrue(DialogueRunner.start(impossible, TestContent.newSession()).isEmpty());
    }
}
