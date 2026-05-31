package com.gdx.game.features.investigation.model;

import com.badlogic.gdx.utils.ObjectMap;
import com.gdx.game.model.DossierData;
import com.gdx.game.model.DossierDatabase;
import com.gdx.game.model.DialogueHistory;
import com.gdx.game.model.InvestigationState;
import com.gdx.game.model.LoreDatabase;
import com.gdx.game.model.NpcState;
import com.gdx.game.shared.api.LlmClient;

import java.io.IOException;

public class EpilogueService {
    private static final int EPILOGUE_MAX_TOKENS = 520;
    private static final int CONFRONTATION_MAX_TOKENS = 280;
    private static final int SUFFICIENT_KILLER_EVIDENCE = 2;

    private final LlmClient llmClient;
    private final LoreDatabase loreDb;
    private final DossierDatabase dossierDb;
    private final NpcDialogueService npcService;

    public EpilogueService(LlmClient llmClient, LoreDatabase loreDb, DossierDatabase dossierDb, NpcDialogueService npcService) {
        this.llmClient = llmClient;
        this.loreDb = loreDb;
        this.dossierDb = dossierDb;
        this.npcService = npcService;
    }

    public String generateEpilogue(InvestigationState inv) throws IOException {
        String accusedId = inv != null ? inv.accusedNpcId : null;
        String realKillerId = (loreDb != null && loreDb.murder != null)
            ? loreDb.murder.killerId
            : null;

        boolean correct = accusedId != null && accusedId.equals(realKillerId);
        EvidenceSummary evidence = buildEvidenceSummary(realKillerId);

        String systemPrompt = buildSystemPrompt();
        String userMessage  = buildUserMessage(accusedId, correct, evidence);

        return llmClient.ask(
            systemPrompt,
            userMessage,
            EPILOGUE_MAX_TOKENS,
            LlmClient.ModelTier.SMART
        );
    }

    public String generateAccusationConfrontation(InvestigationState inv) throws IOException {
        String accusedId = inv != null ? inv.accusedNpcId : null;
        String realKillerId = (loreDb != null && loreDb.murder != null)
            ? loreDb.murder.killerId
            : null;
        boolean correct = accusedId != null && accusedId.equals(realKillerId);
        EvidenceSummary evidence = buildEvidenceSummary(realKillerId);

        String system =
            "Write the accused character's direct response to the final accusation in Ukrainian. " +
                "Return 2-4 short first-person speech bubbles separated only by |||. " +
                "No narration, no labels, no new facts. Use only FINAL, DISCOVERED, KEY_DIALOGUE. " +
                "Wrong accusation: deny and react personally, without naming/implying true killer. " +
                "Correct weak evidence: controlled denial or partial crack, no confession. " +
                "Correct enough evidence: a clear confrontation response; confession only if supported by KEY_DIALOGUE/DISCOVERED.";

        String user = buildConfrontationUserMessage(accusedId, correct, evidence);
        return llmClient.ask(system, user, CONFRONTATION_MAX_TOKENS, LlmClient.ModelTier.SMART);
    }

    private String buildSystemPrompt() {
        StringBuilder sb = new StringBuilder();

        sb.append("Write a short Ukrainian ending, 3 compact paragraphs max.\n")
            .append("No game/meta words. No hidden truth unless it is in DISCOVERED/KEY_DIALOGUE.\n")
            .append("Show concrete consequences for: town, accused person, key affected people.\n")
            .append("If wrong or no accusation: do not name/imply the real killer; show flawed justice/unfinished truth.\n")
            .append("If correct but evidence is weak: accusation lands, but no confession or full public truth.\n")
            .append("If correct and evidence is enough: write a clear accusation outcome; confession is optional only if KEY_DIALOGUE supports it.\n")
            .append("Keep it direct, not lyrical or long.\n");

        return sb.toString();
    }

    private String buildUserMessage(String accusedId,
                                    boolean correct,
                                    EvidenceSummary evidence) {

        StringBuilder sb = new StringBuilder();

        if (loreDb != null && loreDb.setting != null) {
            sb.append("CITY:\n");
            sb.append("Name: ").append(loreDb.setting.townName).append("\n");
            sb.append("Tone: ").append(loreDb.setting.tone).append("\n\n");
        }

        sb.append("FINAL:\n");
        sb.append("Accused: ").append(formatNpcName(accusedId)).append("\n");
        sb.append("Accusation: ").append(buildAccusationOutcome(accusedId, correct, evidence)).append("\n");
        if (correct) {
            sb.append("Evidence for this accusation: ").append(evidence.killerEvidenceCount).append("\n");
        }
        sb.append("Confession rule: do not write a confession unless KEY_DIALOGUE explicitly contains one.\n\n");

        sb.append("PUBLIC:\n");
        sb.append(buildPublicFactsSummary());
        sb.append("\n");

        sb.append("DISCOVERED:\n");
        sb.append(buildDiscoveredFactsSummary());
        sb.append("\n");

        if (correct && !evidence.killerEvidence.isEmpty()) {
            sb.append("DIRECT EVIDENCE FOR THIS ACCUSATION:\n");
            sb.append(evidence.killerEvidence).append("\n");
        }

        if (accusedId != null) {
            String accusedHistory = DialogueHistory.loadRecentForLlm(accusedId, 4, 650);
            if (!accusedHistory.isEmpty()) {
                sb.append("KEY_DIALOGUE:\n");
                sb.append(accusedHistory).append("\n\n");
            }
        }

        sb.append("TASK: Write only from PUBLIC, DISCOVERED, KEY_DIALOGUE and FINAL. ")
            .append("Do not add unrevealed secrets, unseen motives, new crimes, or new facts.\n");

        return sb.toString();
    }

    private String buildAccusationOutcome(String accusedId, boolean correct, EvidenceSummary evidence) {
        if (accusedId == null) {
            return "NO_ACCUSATION";
        }
        if (!correct) {
            return "WRONG_ACCUSATION";
        }
        if (evidence.killerEvidenceCount >= SUFFICIENT_KILLER_EVIDENCE) {
            return "CORRECT_WITH_ENOUGH_EVIDENCE";
        }
        return "CORRECT_BUT_WEAK_EVIDENCE";
    }

    private String buildConfrontationUserMessage(String accusedId, boolean correct, EvidenceSummary evidence) {
        StringBuilder sb = new StringBuilder();
        sb.append("FINAL:\n");
        sb.append("Accused: ").append(formatNpcName(accusedId)).append("\n");
        sb.append("Accusation: ").append(buildAccusationOutcome(accusedId, correct, evidence)).append("\n");
        if (correct) {
            sb.append("Evidence for this accusation: ").append(evidence.killerEvidenceCount).append("\n");
        }
        sb.append("\nDISCOVERED:\n").append(buildDiscoveredFactsSummary()).append("\n");

        if (correct && !evidence.killerEvidence.isEmpty()) {
            sb.append("DIRECT EVIDENCE:\n").append(evidence.killerEvidence).append("\n");
        }

        if (accusedId != null) {
            String accusedHistory = DialogueHistory.loadRecentForLlm(accusedId, 4, 650);
            if (!accusedHistory.isEmpty()) {
                sb.append("KEY_DIALOGUE:\n").append(accusedHistory).append("\n");
            }
        }

        return sb.toString();
    }

    private String formatNpcName(String npcId) {
        if (npcId == null) return "none";
        if (dossierDb != null && dossierDb.characters != null) {
            DossierData data = dossierDb.characters.get(npcId);
            if (data != null && data.name != null && !data.name.isEmpty()) {
                return data.name + " (" + npcId + ")";
            }
        }
        return npcId;
    }

    private EvidenceSummary buildEvidenceSummary(String realKillerId) {
        EvidenceSummary evidence = new EvidenceSummary();
        if (realKillerId == null || dossierDb == null || dossierDb.characters == null) {
            return evidence;
        }

        DossierData killerData = dossierDb.characters.get(realKillerId);
        String killerName = killerData != null && killerData.name != null ? killerData.name : realKillerId;
        String killerFirstName = firstWord(killerName);

        StringBuilder direct = new StringBuilder();

        for (ObjectMap.Entry<String, DossierData> e : dossierDb.characters) {
            String npcId = e.key;
            DossierData data = e.value;
            if (data == null || data.hiddenFacts == null || data.hiddenFacts.isEmpty()) continue;

            NpcState state = npcService.getStateForUi(npcId);
            if (state == null || state.hiddenRevealed == null) continue;

            for (int i = 0; i < state.hiddenRevealed.length && i < data.hiddenFacts.size(); i++) {
                if (!state.hiddenRevealed[i]) continue;

                String fact = data.getHiddenFactText(i);
                if (!pointsToKiller(npcId, fact, realKillerId, killerName, killerFirstName)) continue;

                evidence.killerEvidenceCount++;
                direct.append("- ")
                    .append(data.name != null ? data.name : npcId)
                    .append(": ")
                    .append(fact)
                    .append("\n");
            }
        }

        evidence.killerEvidence = direct.toString();
        return evidence;
    }

    private boolean pointsToKiller(
        String factOwnerId,
        String fact,
        String killerId,
        String killerName,
        String killerFirstName
    ) {
        if (killerId.equals(factOwnerId)) {
            return true;
        }

        String text = (fact != null ? fact : "").toLowerCase();
        return containsLower(text, killerId)
            || containsLower(text, killerName)
            || containsLower(text, killerFirstName);
    }

    private boolean containsLower(String textLower, String value) {
        return value != null && !value.isEmpty() && textLower.contains(value.toLowerCase());
    }

    private String firstWord(String text) {
        if (text == null) return "";
        String trimmed = text.trim();
        int space = trimmed.indexOf(' ');
        return space >= 0 ? trimmed.substring(0, space) : trimmed;
    }

    private static class EvidenceSummary {
        int killerEvidenceCount;
        String killerEvidence = "";
    }

    private String buildPublicFactsSummary() {
        if (dossierDb == null || dossierDb.characters == null) {
            return "No public facts available.\n";
        }

        StringBuilder sb = new StringBuilder();

        for (ObjectMap.Entry<String, DossierData> e : dossierDb.characters) {
            String npcId = e.key;
            DossierData data = e.value;
            if (data.publicFacts == null || data.publicFacts.isEmpty()) continue;

            sb.append((data.name != null ? data.name : npcId)).append(":\n");
            for (String f : data.publicFacts) {
                sb.append("  - ").append(f).append("\n");
            }
        }

        if (sb.length() == 0) {
            sb.append("No public facts available.\n");
        }

        return sb.toString();
    }

    private String buildDiscoveredFactsSummary() {
        if (dossierDb == null || dossierDb.characters == null) {
            return "No significant secrets were uncovered.\n";
        }

        StringBuilder sb = new StringBuilder();

        for (ObjectMap.Entry<String, DossierData> e : dossierDb.characters) {
            String npcId = e.key;
            DossierData data = e.value;
            if (data.hiddenFacts == null || data.hiddenFacts.isEmpty()) continue;

            NpcState state = npcService.getStateForUi(npcId);
            if (state == null || state.hiddenRevealed == null) continue;

            boolean anyForNpc = false;
            StringBuilder npcPart = new StringBuilder();

            for (int i = 0; i < state.hiddenRevealed.length && i < data.hiddenFacts.size(); i++) {
                if (state.hiddenRevealed[i]) {
                    if (!anyForNpc) {
                        anyForNpc = true;
                        npcPart.append((data.name != null ? data.name : npcId)).append(":\n");
                    }
                    npcPart.append("  - ").append(data.getHiddenFactText(i)).append("\n");
                }
            }

            if (anyForNpc) {
                sb.append(npcPart);
            }
        }

        if (sb.length() == 0) {
            sb.append("No significant secrets were uncovered.\n");
        }

        return sb.toString();
    }
}
