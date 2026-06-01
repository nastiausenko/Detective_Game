package com.gdx.game.features.investigation.model;

import com.badlogic.gdx.utils.Array;
import com.gdx.game.model.DossierData;
import com.gdx.game.model.DossierDatabase;
import com.gdx.game.model.DialogueHistory;
import com.gdx.game.model.InvestigationState;
import com.gdx.game.model.LoreDatabase;
import com.gdx.game.model.NpcState;
import com.gdx.game.shared.api.LlmClient;

import java.io.IOException;

public class EpilogueService {
    private static final int EPILOGUE_MAX_TOKENS = 900;
    private static final int CONFRONTATION_MAX_TOKENS = 500;
    private static final int SUFFICIENT_KILLER_EVIDENCE = 2;

    private final LlmClient llmClient;
    private final LoreDatabase loreDb;
    private final DossierDatabase dossierDb;
    private final NpcDialogueService npcService;
    private final Object epilogueCacheLock = new Object();
    private String epilogueCacheKey;
    private String epilogueCacheText;
    private IOException epilogueCacheError;
    private boolean epilogueGenerating;

    public EpilogueService(LlmClient llmClient, LoreDatabase loreDb, DossierDatabase dossierDb, NpcDialogueService npcService) {
        this.llmClient = llmClient;
        this.loreDb = loreDb;
        this.dossierDb = dossierDb;
        this.npcService = npcService;
    }

    public String generateEpilogue(InvestigationState inv) throws IOException {
        String key = buildEpilogueCacheKey(inv);

        synchronized (epilogueCacheLock) {
            if (key.equals(epilogueCacheKey) && epilogueCacheText != null) {
                return epilogueCacheText;
            }

            while (key.equals(epilogueCacheKey) && epilogueGenerating) {
                try {
                    epilogueCacheLock.wait();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Interrupted while waiting for epilogue generation", e);
                }
            }

            if (key.equals(epilogueCacheKey) && epilogueCacheText != null) {
                return epilogueCacheText;
            }
            if (key.equals(epilogueCacheKey) && epilogueCacheError != null) {
                throw epilogueCacheError;
            }
        }

        return generateAndCacheEpilogue(inv, key);
    }

    public void prewarmEpilogue(InvestigationState inv) {
        String key = buildEpilogueCacheKey(inv);

        synchronized (epilogueCacheLock) {
            if (key.equals(epilogueCacheKey) && (epilogueGenerating || epilogueCacheText != null)) {
                return;
            }
            epilogueCacheKey = key;
            epilogueCacheText = null;
            epilogueCacheError = null;
            epilogueGenerating = true;
        }

        new Thread(() -> {
            try {
                String text = generateEpilogueUncached(inv);
                synchronized (epilogueCacheLock) {
                    if (key.equals(epilogueCacheKey)) {
                        epilogueCacheText = text;
                        epilogueCacheError = null;
                    }
                }
            } catch (Exception e) {
                synchronized (epilogueCacheLock) {
                    if (key.equals(epilogueCacheKey)) {
                        epilogueCacheError = e instanceof IOException
                            ? (IOException)e
                            : new IOException("Epilogue prewarm failed", e);
                    }
                }
            } finally {
                synchronized (epilogueCacheLock) {
                    if (key.equals(epilogueCacheKey)) {
                        epilogueGenerating = false;
                    }
                    epilogueCacheLock.notifyAll();
                }
            }
        }, "epilogue-prewarm-worker").start();
    }

    public void clearCache() {
        synchronized (epilogueCacheLock) {
            epilogueCacheKey = null;
            epilogueCacheText = null;
            epilogueCacheError = null;
            epilogueGenerating = false;
            epilogueCacheLock.notifyAll();
        }
    }

    public String getCachedEpilogue(InvestigationState inv) {
        String key = buildEpilogueCacheKey(inv);
        synchronized (epilogueCacheLock) {
            if (key.equals(epilogueCacheKey)) {
                return epilogueCacheText;
            }
        }
        return null;
    }

    private String generateAndCacheEpilogue(InvestigationState inv, String key) throws IOException {
        synchronized (epilogueCacheLock) {
            epilogueCacheKey = key;
            epilogueCacheText = null;
            epilogueCacheError = null;
            epilogueGenerating = true;
        }

        try {
            String text = generateEpilogueUncached(inv);
            synchronized (epilogueCacheLock) {
                if (key.equals(epilogueCacheKey)) {
                    epilogueCacheText = text;
                    epilogueCacheError = null;
                }
                return text;
            }
        } catch (IOException e) {
            synchronized (epilogueCacheLock) {
                if (key.equals(epilogueCacheKey)) {
                    epilogueCacheError = e;
                }
            }
            throw e;
        } finally {
            synchronized (epilogueCacheLock) {
                if (key.equals(epilogueCacheKey)) {
                    epilogueGenerating = false;
                }
                epilogueCacheLock.notifyAll();
            }
        }
    }

    private String generateEpilogueUncached(InvestigationState inv) throws IOException {
        String accusedId = inv != null ? inv.accusedNpcId : null;
        String realKillerId = (loreDb != null && loreDb.murder != null)
            ? loreDb.murder.killerId
            : null;

        boolean correct = accusedId != null && accusedId.equals(realKillerId);
        EvidenceSummary evidence = buildEvidenceSummary(realKillerId);

        String systemPrompt = buildSystemPrompt();
        String userMessage  = buildUserMessage(accusedId, correct, evidence);

        try {
            return llmClient.ask(
                systemPrompt,
                userMessage,
                EPILOGUE_MAX_TOKENS,
                LlmClient.ModelTier.SMART
            );
        } catch (IOException e) {
            return buildFallbackEpilogue(accusedId, correct, evidence);
        }
    }

    private String buildEpilogueCacheKey(InvestigationState inv) {
        String accusedId = inv != null && inv.accusedNpcId != null ? inv.accusedNpcId : "none";
        int revealedCount = inv != null ? inv.revealedHiddenFacts : -1;
        return accusedId + ":" + revealedCount + ":" + buildRevealedFactsFingerprint();
    }

    public String generateAccusationConfrontation(InvestigationState inv) throws IOException {
        String accusedId = inv != null ? inv.accusedNpcId : null;
        String realKillerId = (loreDb != null && loreDb.murder != null)
            ? loreDb.murder.killerId
            : null;
        boolean correct = accusedId != null && accusedId.equals(realKillerId);
        EvidenceSummary evidence = buildEvidenceSummary(realKillerId);

        String system =
            "Write the accused character's direct response in Ukrainian.\n" +
                "Format: 2-3 first-person speech bubbles, one sentence each, separated only by |||.\n" +
                "Style: plain spoken Ukrainian, like a person talking aloud, not translated literary prose.\n" +
                "No labels, narration, metaphors, melodrama, bureaucratic phrasing, or new facts.\n" +
                "Wrong accusation: deny it personally; never hint at the real killer.\n" +
                "Correct but weak evidence: evade or answer dryly; no direct confession.\n" +
                "Correct with enough evidence: react sharply; confess only if DISCOVERED or KEY_DIALOGUE supports it.\n" +
                "Avoid Ukrainian phrases equivalent to: burden of truth, my deeds, fate, shadows, emptiness.";

        String user = buildConfrontationUserMessage(accusedId, correct, evidence);
        return llmClient.ask(system, user, CONFRONTATION_MAX_TOKENS, LlmClient.ModelTier.SMART);
    }

    private String buildSystemPrompt() {
        StringBuilder sb = new StringBuilder();

        sb.append("Write a short final epilogue in Ukrainian: 2 paragraphs, 45-75 words total.\n")
            .append("Use plain natural Ukrainian and short sentences. Do not sound poetic or translated.\n")
            .append("Use WORLD and CAST only as grounding; do not recite known dossier facts as discoveries.\n")
            .append("Write consequences only: what happens to the accused, the town, and people affected by DISCOVERED facts.\n")
            .append("Use only WORLD, CAST, FINAL, DISCOVERED, DIRECT_EVIDENCE, and KEY_DIALOGUE. Invent nothing.\n")
            .append("Wrong or no accusation: do not name or imply the real killer; show incomplete justice.\n")
            .append("Correct but weak evidence: suspicion remains, with no public confession or full truth.\n")
            .append("Correct with enough evidence: make the accusation outcome clear; mention confession only if supported.\n")
            .append("Ban metaphors, symbolic imagery, invented scenes, and Ukrainian equivalents of shadows/light/square/windows/burden/puddle/paint.\n");

        return sb.toString();
    }

    private String buildUserMessage(String accusedId,
                                    boolean correct,
                                    EvidenceSummary evidence) {

        StringBuilder sb = new StringBuilder();

        if (loreDb != null && loreDb.setting != null) {
            sb.append("WORLD:\n");
            sb.append(buildWorldContextSummary()).append("\n");
        }

        sb.append("CAST:\n");
        sb.append(buildCastContextSummary()).append("\n");

        sb.append("FINAL:\n");
        sb.append("Accused: ").append(formatNpcName(accusedId)).append("\n");
        sb.append("Accusation: ").append(buildAccusationOutcome(accusedId, correct, evidence)).append("\n");
        if (correct) {
            sb.append("Evidence for this accusation: ").append(evidence.killerEvidenceCount).append("\n");
        }
        sb.append("Confession rule: do not write a confession unless KEY_DIALOGUE explicitly contains one.\n\n");

        sb.append("DISCOVERED:\n");
        sb.append(buildDiscoveredFactsSummary());
        sb.append("\n");

        if (correct && !evidence.killerEvidence.isEmpty()) {
            sb.append("DIRECT EVIDENCE FOR THIS ACCUSATION:\n");
            sb.append(evidence.killerEvidence).append("\n");
        }

        if (accusedId != null) {
            String accusedHistory = DialogueHistory.loadRecentForLlm(accusedId, 3, 450);
            if (!accusedHistory.isEmpty()) {
                sb.append("KEY_DIALOGUE:\n");
                sb.append(accusedHistory).append("\n\n");
            }
        }

        sb.append("TASK: Return only the epilogue prose. No list. No unrevealed secrets, new motives, new crimes, or new facts.\n");

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

    private String buildWorldContextSummary() {
        if (loreDb == null || loreDb.setting == null) {
            return "- Setting: 1940s Rosenfeld, a small town shaped by medical-school authority and rumor.\n";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("- Town: ").append(nonEmpty(loreDb.setting.townName, "Rosenfeld")).append(".\n");
        if (loreDb.setting.summary != null && !loreDb.setting.summary.isEmpty()) {
            sb.append("- Setting summary: ").append(loreDb.setting.summary).append("\n");
        }
        if (loreDb.setting.tone != null && !loreDb.setting.tone.isEmpty()) {
            sb.append("- Tone: ").append(loreDb.setting.tone).append("\n");
        }
        if (loreDb.murder != null) {
            sb.append("- Case: Walter was found dead in his home office");
            if (loreDb.murder.timeApprox != null && !loreDb.murder.timeApprox.isEmpty()) {
                sb.append(" around ").append(loreDb.murder.timeApprox);
            }
            if (loreDb.murder.weapon != null && !loreDb.murder.weapon.isEmpty()) {
                sb.append("; weapon: ").append(loreDb.murder.weapon);
            }
            sb.append(".\n");
            if (loreDb.murder.officialStory != null && !loreDb.murder.officialStory.isEmpty()) {
                sb.append("- Public story: ").append(loreDb.murder.officialStory).append("\n");
            }
        }
        return sb.toString();
    }

    private String buildCastContextSummary() {
        if (dossierDb == null || dossierDb.characters == null || dossierDb.characters.isEmpty()) {
            return "- No cast data.\n";
        }

        StringBuilder sb = new StringBuilder();
        Array<String> ids = dossierDb.characters.orderedKeys();
        for (int i = 0; i < ids.size; i++) {
            String id = ids.get(i);
            DossierData data = dossierDb.characters.get(id);
            if (data == null) continue;

            sb.append("- ").append(nonEmpty(data.name, id));
            if (data.role != null && !data.role.isEmpty()) {
                sb.append(": ").append(data.role);
            }
            sb.append(".\n");
        }
        return sb.toString();
    }

    private String nonEmpty(String value, String fallback) {
        return value != null && !value.isEmpty() ? value : fallback;
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
            String accusedHistory = DialogueHistory.loadRecentForLlm(accusedId, 3, 450);
            if (!accusedHistory.isEmpty()) {
                sb.append("KEY_DIALOGUE:\n").append(accusedHistory).append("\n");
            }
        }

        sb.append("TASK: Return only the speech bubbles separated by |||. Do not wrap the whole answer in quotes.\n");

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

        Array<String> ids = dossierDb.characters.orderedKeys();
        for (int idx = 0; idx < ids.size; idx++) {
            String npcId = ids.get(idx);
            DossierData data = dossierDb.characters.get(npcId);
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

    private String buildFallbackEpilogue(String accusedId, boolean correct, EvidenceSummary evidence) {
        String accused = formatNpcDisplayName(accusedId);

        if (accusedId == null) {
            return "Справу закрили без обвинувачення. Частину документів передали на перевірку, але ніхто не взяв на себе провину.\n\n"
                + "У Розенфельді стали обережнішими. Люди менше говорили про Вальтера, та недовіра залишилася.";
        }

        if (!correct) {
            return accused + " тимчасово відсторонили, але обвинувачення не втрималося. Доказів проти " + accusativePronoun(accusedId) + " не вистачило.\n\n"
                + "Місто не отримало чіткої відповіді. Перевірки тривали, а люди ще довго згадували цю справу пошепки.";
        }

        if (evidence != null && evidence.killerEvidenceCount >= SUFFICIENT_KILLER_EVIDENCE) {
            return accused + " затримали, а зібрані матеріали передали суду. Після цього архіви й старі записи переглянули заново.\n\n"
                + "Розенфельд не заспокоївся одразу. Та після обвинувачення люди нарешті побачили, що справу не замели.";
        }

        return accused + " залишився під підозрою, але доказів для вироку було замало. Публічного зізнання не сталося.\n\n"
            + "Місто чекало нових перевірок. Ті, кого зачепили розкриті факти, стали обережнішими й менше говорили з чужими.";
    }

    private String formatNpcDisplayName(String npcId) {
        if (npcId == null) return "Ніхто";
        if (dossierDb != null && dossierDb.characters != null) {
            DossierData data = dossierDb.characters.get(npcId);
            if (data != null && data.name != null && !data.name.isEmpty()) {
                return data.name;
            }
        }
        return npcId;
    }

    private String accusativePronoun(String npcId) {
        if ("clara".equals(npcId) || "elena".equals(npcId) || "mara".equals(npcId)) {
            return "неї";
        }
        return "нього";
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

        Array<String> ids = dossierDb.characters.orderedKeys();
        for (int idx = 0; idx < ids.size; idx++) {
            String npcId = ids.get(idx);
            DossierData data = dossierDb.characters.get(npcId);
            if (data == null) continue;
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

        Array<String> ids = dossierDb.characters.orderedKeys();
        for (int idx = 0; idx < ids.size; idx++) {
            String npcId = ids.get(idx);
            DossierData data = dossierDb.characters.get(npcId);
            if (data == null) continue;
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

    private String buildRevealedFactsFingerprint() {
        if (dossierDb == null || dossierDb.characters == null) {
            return "none";
        }

        StringBuilder sb = new StringBuilder();
        Array<String> ids = dossierDb.characters.orderedKeys();
        for (int idx = 0; idx < ids.size; idx++) {
            String npcId = ids.get(idx);
            DossierData data = dossierDb.characters.get(npcId);
            if (data == null || data.hiddenFacts == null || data.hiddenFacts.isEmpty()) continue;

            NpcState state = npcService.getStateForUi(npcId);
            if (state == null || state.hiddenRevealed == null) continue;

            sb.append(npcId).append(":");
            for (int i = 0; i < state.hiddenRevealed.length && i < data.hiddenFacts.size(); i++) {
                if (state.hiddenRevealed[i]) {
                    sb.append(i).append(",");
                }
            }
            sb.append(";");
        }
        return sb.toString();
    }
}
