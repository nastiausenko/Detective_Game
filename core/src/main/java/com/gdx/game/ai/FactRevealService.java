package com.gdx.game.ai;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.IntArray;
import com.gdx.game.domain.character.DossierData;
import com.gdx.game.domain.character.DossierDatabase;
import com.gdx.game.domain.world.CrimeSceneService;

public class FactRevealService {
    private static final String DOCTOR_ID = "walter";

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
        npcService.runFactRevealCheckAsync(
            () -> revealFactsAfterExchange(npcId, question, answer),
            callback
        );
    }

    private int revealFactsAfterExchange(String npcId, String question, String answer) {
        DossierData npcData = getDossier(npcId);
        DossierData doctorData = getDossier(DOCTOR_ID);

        int newlyRevealed = 0;
        newlyRevealed += revealFactsForNpc(npcId, npcData, question, answer, "npc=" + npcId);
        newlyRevealed += revealFactsForNpc(DOCTOR_ID, doctorData, question, answer, "DOCTOR");

        if (newlyRevealed > 0 && crimeSceneService != null) {
            crimeSceneService.syncUnlockedHints();
        }

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
        String debugPrefix
    ) {
        IntArray candidateIndexes = collectSemanticCandidateFacts(npcId, data, question, answer, debugPrefix);
        if (data == null || data.hiddenFacts == null || data.hiddenFacts.isEmpty()) return 0;
        if (candidateIndexes == null || candidateIndexes.size == 0) return 0;

        IntArray toReveal = new IntArray();

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
                toReveal.add(idx);
            }
        }

        if (toReveal.size > 0) {
            npcService.markFactsRevealed(npcId, toReveal);
        }

        return toReveal.size;
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
}
