package com.keplersharvest.dialogue;

import com.badlogic.gdx.utils.JsonValue;
import com.keplersharvest.configuration.ConfigurationException;
import com.keplersharvest.configuration.Json5;
import com.keplersharvest.configuration.ResourceReader;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Parses dialogue trees from JSON.
 *
 * <p>Conditions are authored as an object whose keys are ANDed together, which keeps the common
 * case ("only if the recycler is online") a single line for a writer.
 */
public final class DialogueLoader {

    private final ResourceReader reader;

    public DialogueLoader(ResourceReader reader) {
        this.reader = Objects.requireNonNull(reader, "reader");
    }

    public DialogueTree load(String dialogueId, String path) {
        JsonValue root = Json5.parse(reader.readText(path), path);
        String speakerId = Json5.string(root, "speaker", dialogueId);
        List<DialogueNode> nodes = new ArrayList<>();
        for (JsonValue node : Json5.array(root, "nodes")) {
            nodes.add(readNode(node, path));
        }
        if (nodes.isEmpty()) {
            throw new ConfigurationException("Dialogue " + path + " has no nodes");
        }
        DialogueTree tree = new DialogueTree(dialogueId, speakerId, nodes);
        validateLinks(tree, path);
        return tree;
    }

    private DialogueNode readNode(JsonValue node, String path) {
        String id = Json5.requireString(node, "id", path);
        List<String> lines = Json5.stringList(node, "lines");
        if (lines.isEmpty()) {
            throw new ConfigurationException("Dialogue node " + id + " in " + path + " has no lines");
        }
        List<DialogueChoice> choices = new ArrayList<>();
        for (JsonValue choice : Json5.array(node, "choices")) {
            choices.add(new DialogueChoice(
                    Json5.requireString(choice, "text", path),
                    readCondition(choice.get("when"), path),
                    Json5.optionalString(choice, "next"),
                    readEffects(choice, path)));
        }
        return new DialogueNode(
                id,
                Json5.bool(node, "entry", false),
                readCondition(node.get("when"), path),
                lines,
                readEffects(node, path),
                choices,
                Json5.optionalString(node, "next"));
    }

    private List<DialogueEffect> readEffects(JsonValue owner, String path) {
        List<DialogueEffect> effects = new ArrayList<>();
        for (JsonValue effect : Json5.array(owner, "effects")) {
            String type = Json5.requireString(effect, "type", path);
            effects.add(switch (type) {
                case "relationship" -> new DialogueEffect.ChangeRelationship(
                        Json5.string(effect, "colonist", ""), Json5.requireInt(effect, "amount", path));
                case "startQuest" -> new DialogueEffect.StartQuest(Json5.requireString(effect, "quest", path));
                case "giveItem" -> new DialogueEffect.GiveItem(
                        Json5.requireString(effect, "item", path), Json5.integer(effect, "count", 1));
                case "setFlag" -> new DialogueEffect.SetFlag(Json5.requireString(effect, "flag", path));
                case "clearFlag" -> new DialogueEffect.ClearFlag(Json5.requireString(effect, "flag", path));
                case "evidence" -> new DialogueEffect.GrantEvidence(Json5.requireString(effect, "evidence", path));
                default -> throw new ConfigurationException("Unknown dialogue effect '" + type + "' in " + path);
            });
        }
        return effects;
    }

    /** Reads a {@code when} object; a missing or empty object means "always". */
    private DialogueCondition readCondition(JsonValue when, String path) {
        if (when == null || when.isNull()) {
            return DialogueCondition.ALWAYS;
        }
        List<DialogueCondition> parts = new ArrayList<>();
        Optional<String> relationshipTarget = Json5.optionalString(when, "relationshipWith");
        if (when.has("minRelationship")) {
            parts.add(new DialogueCondition.MinRelationship(
                    relationshipTarget.orElse(""), when.getInt("minRelationship")));
        }
        addIfPresent(when, "questActive", value -> parts.add(new DialogueCondition.QuestActive(value)));
        addIfPresent(when, "questCompleted", value -> parts.add(new DialogueCondition.QuestCompleted(value)));
        addIfPresent(when, "questAvailable", value -> parts.add(new DialogueCondition.QuestAvailable(value)));
        addIfPresent(when, "questNotStarted", value -> parts.add(new DialogueCondition.QuestNotStarted(value)));
        addIfPresent(when, "moduleOnline", value -> parts.add(new DialogueCondition.ModuleOnline(value)));
        addIfPresent(when, "moduleOffline", value -> parts.add(new DialogueCondition.ModuleOffline(value)));
        addIfPresent(when, "logFound", value -> parts.add(new DialogueCondition.LogFound(value)));
        addIfPresent(when, "evidence", value -> parts.add(new DialogueCondition.EvidenceKnown(value)));
        addIfPresent(when, "flag", value -> parts.add(new DialogueCondition.FlagSet(value)));
        addIfPresent(when, "flagNot", value -> parts.add(new DialogueCondition.FlagClear(value)));
        if (when.has("minLogs")) {
            parts.add(new DialogueCondition.MinLogs(when.getInt("minLogs")));
        }
        if (when.has("minDay")) {
            parts.add(new DialogueCondition.MinDay(when.getInt("minDay")));
        }
        if (when.has("anyOf")) {
            List<DialogueCondition> alternatives = new ArrayList<>();
            for (JsonValue alternative : Json5.array(when, "anyOf")) {
                alternatives.add(readCondition(alternative, path));
            }
            parts.add(new DialogueCondition.Any(alternatives));
        }
        return switch (parts.size()) {
            case 0 -> DialogueCondition.ALWAYS;
            case 1 -> parts.get(0);
            default -> new DialogueCondition.All(parts);
        };
    }

    private void addIfPresent(JsonValue when, String key, java.util.function.Consumer<String> consumer) {
        JsonValue value = when.get(key);
        if (value != null && !value.isNull()) {
            consumer.accept(value.asString());
        }
    }

    /** Fails fast on a {@code next} that points at a node that does not exist. */
    private void validateLinks(DialogueTree tree, String path) {
        for (DialogueNode node : tree.nodes()) {
            node.next().ifPresent(next -> requireNode(tree, next, node.id(), path));
            for (DialogueChoice choice : node.choices()) {
                choice.next().ifPresent(next -> requireNode(tree, next, node.id(), path));
            }
        }
    }

    private void requireNode(DialogueTree tree, String nodeId, String from, String path) {
        if (tree.node(nodeId).isEmpty()) {
            throw new ConfigurationException(
                    "Dialogue node '" + from + "' in " + path + " links to unknown node '" + nodeId + "'");
        }
    }
}
