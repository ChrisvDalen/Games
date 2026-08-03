package com.keplersharvest.ui;

import com.keplersharvest.configuration.FileResourceReader;
import com.keplersharvest.configuration.GameContent;
import com.keplersharvest.dialogue.DialogueNode;
import com.keplersharvest.dialogue.DialogueTree;
import com.keplersharvest.testing.TestContent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The interface draws every string through the generated pixel font, which only covers printable
 * ASCII. A curly quote or an em dash pasted into a dialogue line would render as a blank box, so
 * this guards the content against characters the font cannot draw.
 */
class PixelFontCoverageTest {

    private static final char FIRST = ' ';
    private static final char LAST = '~';

    @Test
    @DisplayName("every player-visible string is renderable by the pixel font")
    void contentStaysWithinTheFont() {
        GameContent content = TestContent.load();
        List<String> offenders = new ArrayList<>();

        content.items().values().forEach(item -> {
            check(offenders, "item " + item.id() + " name", item.name());
            check(offenders, "item " + item.id() + " description", item.description());
        });
        content.crops().values().forEach(crop -> {
            check(offenders, "crop " + crop.id() + " name", crop.name());
            check(offenders, "crop " + crop.id() + " description", crop.description());
        });
        content.modules().values().forEach(module -> {
            check(offenders, "module " + module.id() + " name", module.name());
            check(offenders, "module " + module.id() + " description", module.description());
            check(offenders, "module " + module.id() + " benefit", module.benefitText());
            module.stages().forEach(stage -> {
                check(offenders, "module " + module.id() + " stage", stage.name());
                check(offenders, "module " + module.id() + " stage text", stage.description());
            });
        });
        content.recipes().values().forEach(recipe -> {
            check(offenders, "recipe " + recipe.id() + " name", recipe.name());
            check(offenders, "recipe " + recipe.id() + " description", recipe.description());
        });
        content.colonists().values().forEach(colonist -> {
            check(offenders, "colonist " + colonist.id() + " name", colonist.name());
            check(offenders, "colonist " + colonist.id() + " role", colonist.role());
        });
        content.quests().values().forEach(quest -> {
            check(offenders, "quest " + quest.id() + " title", quest.title());
            check(offenders, "quest " + quest.id() + " summary", quest.summary());
            quest.completionText().ifPresent(text -> check(offenders, "quest " + quest.id() + " completion", text));
            quest.objectives().forEach(objective ->
                    check(offenders, "quest " + quest.id() + " objective", objective.description()));
        });
        content.crewLogs().values().forEach(log -> {
            check(offenders, "log " + log.id() + " title", log.title());
            check(offenders, "log " + log.id() + " author", log.author());
            log.body().forEach(line -> check(offenders, "log " + log.id() + " body", line));
        });
        content.evidence().values().forEach(evidence -> {
            check(offenders, "evidence " + evidence.id() + " title", evidence.title());
            check(offenders, "evidence " + evidence.id() + " text", evidence.text());
            check(offenders, "evidence " + evidence.id() + " source", evidence.source());
        });
        content.reveal().body().forEach(line -> check(offenders, "reveal body", line));
        for (DialogueTree tree : content.dialogues().values()) {
            for (DialogueNode node : tree.nodes()) {
                node.lines().forEach(line -> check(offenders, "dialogue " + tree.id() + "/" + node.id(), line));
                node.choices().forEach(choice ->
                        check(offenders, "dialogue " + tree.id() + "/" + node.id() + " choice", choice.text()));
            }
        }
        content.maps().maps().forEach(map -> {
            check(offenders, "map " + map.id() + " name", map.displayName());
            map.objects().forEach(object -> object.properties()
                    .forEach((key, value) -> check(offenders, "map " + map.id() + " " + object.id() + "." + key, value)));
        });

        assertTrue(offenders.isEmpty(),
                "Content uses characters the pixel font cannot draw:\n" + String.join("\n", offenders));
    }

    @Test
    @DisplayName("the generated font declares a glyph for every printable ASCII character")
    void fontCoversPrintableAscii() throws Exception {
        Path descriptor = FileResourceReader.locateAssets().root().resolve("ui/pixel-font.fnt");
        assertTrue(Files.isRegularFile(descriptor),
                "run ./gradlew generatePixelFont - missing " + descriptor);

        TreeSet<Integer> declared = new TreeSet<>();
        for (String line : Files.readAllLines(descriptor)) {
            if (line.startsWith("char id=")) {
                declared.add(Integer.parseInt(line.substring(8, line.indexOf(' ', 8))));
            }
        }

        List<String> missing = new ArrayList<>();
        for (char c = FIRST; c <= LAST; c++) {
            if (!declared.contains((int) c)) {
                missing.add("'" + c + "'");
            }
        }
        assertTrue(missing.isEmpty(), "font is missing glyphs for " + missing);
    }

    private void check(List<String> offenders, String where, String text) {
        if (text == null) {
            return;
        }
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\n' || c == '\t') {
                continue;
            }
            if (c < FIRST || c > LAST) {
                offenders.add(where + ": U+" + String.format("%04X", (int) c) + " in \"" + text + "\"");
                return;
            }
        }
    }
}
