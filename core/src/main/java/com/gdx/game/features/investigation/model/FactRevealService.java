package com.gdx.game.features.investigation.model;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.IntArray;
import com.gdx.game.model.DossierData;
import com.gdx.game.model.DossierDatabase;

public class FactRevealService {
    public interface ProgressCallback {
        void onFactRevealed(int count);
    }

    private static final String DOCTOR_ID = "walter";
    private static final int EXCHANGE_LOG_MAX_CHARS = 280;

    private final NpcDialogueService npcService;
    private final DossierDatabase dossierDb;
    private final CrimeSceneService crimeSceneService;

    public FactRevealService(
        NpcDialogueService npcService,
        DossierDatabase dossierDb,
        CrimeSceneService crimeSceneService
    ) {
        this.npcService = npcService;
        this.dossierDb = dossierDb;
        this.crimeSceneService = crimeSceneService;
    }

    public void revealFactsAfterExchangeAsync(
        String npcId,
        String question,
        String answer,
        NpcDialogueService.AsyncCallback<Integer> callback
    ) {
        revealFactsAfterExchangeAsync(npcId, question, answer, null, callback);
    }

    public void revealFactsAfterExchangeAsync(
        String npcId,
        String question,
        String answer,
        ProgressCallback progressCallback,
        NpcDialogueService.AsyncCallback<Integer> callback
    ) {
        npcService.runFactRevealCheckAsync(
            () -> revealFactsAfterExchange(npcId, question, answer, progressCallback),
            callback
        );
    }

    private int revealFactsAfterExchange(
        String npcId,
        String question,
        String answer,
        ProgressCallback progressCallback
    ) {
        DossierData npcData = getDossier(npcId);
        DossierData doctorData = getDossier(DOCTOR_ID);

        Gdx.app.log("FACT_DEBUG",
            "EXCHANGE (npc=" + safeForLog(npcId) + ") Q=\""
                + compactForLog(question) + "\" A=\"" + compactForLog(answer) + "\"");

        int newlyRevealed = 0;
        newlyRevealed += revealFactsForNpc(npcId, npcData, question, answer, "npc=" + npcId, progressCallback);
        newlyRevealed += revealFactsForNpc(DOCTOR_ID, doctorData, question, answer, "DOCTOR", progressCallback);

        return newlyRevealed;
    }

    private DossierData getDossier(String npcId) {
        return dossierDb != null && dossierDb.characters != null
            ? dossierDb.characters.get(npcId)
            : null;
    }

    private int revealFactsForNpc(
        String npcId,
        DossierData data,
        String question,
        String answer,
        String debugPrefix,
        ProgressCallback progressCallback
    ) {
        IntArray candidateIndexes = collectSemanticCandidateFacts(npcId, data, question, answer, debugPrefix);
        if (data == null || data.hiddenFacts == null || data.hiddenFacts.isEmpty()) return 0;
        if (candidateIndexes == null || candidateIndexes.size == 0) return 0;

        int newlyRevealed = 0;

        for (int i = 0; i < candidateIndexes.size; i++) {
            int idx = candidateIndexes.get(i);
            if (idx < 0 || idx >= data.hiddenFacts.size()) continue;

            DossierData.HiddenFactData hiddenFact = data.getHiddenFact(idx);
            String hidden = data.getHiddenFactText(idx);
            if (hidden.isEmpty()) continue;

            NpcDialogueService.FactRevealDecision decision =
                NpcDialogueService.FactRevealDecision.no("not checked");
            try {
                decision = npcService.shouldRevealFactFromExchange(question, answer, hiddenFact);
            } catch (Exception ex) {
                ex.printStackTrace();
            }

            Gdx.app.log("FACT_DEBUG",
                "REVEAL_CHECK (" + debugPrefix + ") fact #" + idx + " -> "
                    + decision.reveal + decision.debugSuffix());

            if (decision.reveal) {
                IntArray oneFact = new IntArray();
                oneFact.add(idx);
                npcService.markFactsRevealed(npcId, oneFact);
                newlyRevealed++;

                if (crimeSceneService != null) {
                    crimeSceneService.syncUnlockedHints();
                }
                if (progressCallback != null) {
                    progressCallback.onFactRevealed(1);
                }
            }
        }

        return newlyRevealed;
    }

    private IntArray collectSemanticCandidateFacts(
        String npcId,
        DossierData data,
        String question,
        String answer,
        String debugPrefix
    ) {
        try {
            IntArray candidates = npcService.findRelevantHiddenFacts(npcId, data, question, answer);
            if (candidates.size == 0) {
                Gdx.app.log("FACT_DEBUG", "No semantic candidates (" + debugPrefix + ")");
            }
            return candidates;
        } catch (Exception ex) {
            ex.printStackTrace();
            Gdx.app.log("FACT_DEBUG",
                "Semantic retrieval failed; skipping fact check (" + debugPrefix + ")");
            return new IntArray();
        }
    }

    private String compactForLog(String text) {
        if (text == null) return "";

        String compact = text
            .replace('\n', ' ')
            .replace('\r', ' ')
            .replace('\t', ' ')
            .replaceAll("\\s+", " ")
            .trim();

        if (compact.length() <= EXCHANGE_LOG_MAX_CHARS) {
            return safeForLog(compact);
        }

        return safeForLog(compact.substring(0, EXCHANGE_LOG_MAX_CHARS - 3) + "...");
    }

    private String safeForLog(String text) {
        return text == null ? "" : text.replace("\"", "'");
    }
}
