package com.keplersharvest.mystery;

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * What the player has recovered and worked out about the vanished crew.
 *
 * <p>Recovering a log automatically files the evidence it establishes, so clue bookkeeping lives in
 * one place rather than being scattered across the systems that hand clues out.
 */
public final class EvidenceJournal {

    private final Map<String, CrewLogDefinition> logCatalogue;
    private final Map<String, EvidenceDefinition> evidenceCatalogue;
    private final RevealDefinition reveal;

    private final Set<String> foundLogs = new LinkedHashSet<>();
    private final Set<String> evidence = new LinkedHashSet<>();
    private boolean revealSeen;

    public EvidenceJournal(Map<String, CrewLogDefinition> logCatalogue,
                           Map<String, EvidenceDefinition> evidenceCatalogue,
                           RevealDefinition reveal) {
        this.logCatalogue = Map.copyOf(Objects.requireNonNull(logCatalogue, "logCatalogue"));
        this.evidenceCatalogue = Map.copyOf(Objects.requireNonNull(evidenceCatalogue, "evidenceCatalogue"));
        this.reveal = Objects.requireNonNull(reveal, "reveal");
    }

    public int totalLogs() {
        return logCatalogue.size();
    }

    public int foundLogCount() {
        return foundLogs.size();
    }

    public boolean hasLog(String logId) {
        return foundLogs.contains(logId);
    }

    public boolean hasEvidence(String evidenceId) {
        return evidence.contains(evidenceId);
    }

    public Optional<CrewLogDefinition> logDefinition(String logId) {
        return Optional.ofNullable(logCatalogue.get(logId));
    }

    /** Recovered logs, in authored reading order. */
    public List<CrewLogDefinition> foundLogsInOrder() {
        return foundLogs.stream()
                .map(logCatalogue::get)
                .filter(Objects::nonNull)
                .sorted(Comparator.comparingInt(CrewLogDefinition::order))
                .toList();
    }

    public List<EvidenceDefinition> recordedEvidence() {
        return evidence.stream().map(evidenceCatalogue::get).filter(Objects::nonNull).toList();
    }

    /**
     * Files a crew log and everything it establishes.
     *
     * @return false when it was already recovered
     */
    public boolean recordLog(String logId) {
        CrewLogDefinition definition = logCatalogue.get(logId);
        if (definition == null || !foundLogs.add(logId)) {
            return false;
        }
        definition.grantsEvidence().forEach(this::recordEvidence);
        return true;
    }

    /**
     * Files a single clue.
     *
     * @return false when it was already known
     */
    public boolean recordEvidence(String evidenceId) {
        if (!evidenceCatalogue.containsKey(evidenceId)) {
            return false;
        }
        return evidence.add(evidenceId);
    }

    public RevealDefinition reveal() {
        return reveal;
    }

    /** True once enough logs and clues are in hand for the conclusion to be drawn. */
    public boolean revealAvailable() {
        return foundLogs.size() >= reveal.requiredLogs()
                && evidence.containsAll(reveal.requiredEvidence());
    }

    public boolean revealSeen() {
        return revealSeen;
    }

    public void markRevealSeen() {
        this.revealSeen = true;
    }

    /** Clues still missing before the reveal unlocks; used by the journal's hint line. */
    public List<String> missingForReveal() {
        return reveal.requiredEvidence().stream()
                .filter(id -> !evidence.contains(id))
                .map(id -> Optional.ofNullable(evidenceCatalogue.get(id))
                        .map(EvidenceDefinition::title)
                        .orElse(id))
                .toList();
    }

    public Set<String> foundLogsSnapshot() {
        return Set.copyOf(foundLogs);
    }

    public Set<String> evidenceSnapshot() {
        return Set.copyOf(evidence);
    }

    public void restore(Set<String> logs, Set<String> knownEvidence, boolean revealSeen) {
        foundLogs.clear();
        logs.stream().filter(logCatalogue::containsKey).forEach(foundLogs::add);
        evidence.clear();
        knownEvidence.stream().filter(evidenceCatalogue::containsKey).forEach(evidence::add);
        this.revealSeen = revealSeen;
    }
}
