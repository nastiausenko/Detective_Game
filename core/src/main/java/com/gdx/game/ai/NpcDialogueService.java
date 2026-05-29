package com.gdx.game.ai;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.IntArray;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.TimeUtils;
import com.gdx.game.domain.investigation.DialogueHistory;
import com.gdx.game.domain.character.DossierData;
import com.gdx.game.domain.character.DossierDatabase;
import com.gdx.game.domain.character.NpcState;
import com.gdx.game.domain.world.LocationDescriptions;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class NpcDialogueService {
    public interface AsyncCallback<T> {
        void onComplete(T result, Throwable error);
    }

    public interface AsyncTask<T> {
        T run() throws Exception;
    }

    private static final int MAX_HISTORY_PAIRS = 6;
    private static final int MAX_HISTORY_CHARS = 1200;
    private static final int FACT_RETRIEVAL_TOP_K = 2;
    private static final float FACT_RETRIEVAL_MIN_SIMILARITY = 0.28f;
    private static final float FACT_RETRIEVAL_FALLBACK_SIMILARITY = 0.20f;
    private static final float SESSION_BREAK_SECONDS = 180f;
    private static final String NPC_PREFS_NAME = "npc_state";
    private static final String GLOBAL_RULES =
            "You are an NPC in a detective game.\n" +
                    "Setting: town Rosenfeld, quiet European town, moral thriller tone.\n" +
                    "Canon murder (do NOT contradict):\n" +
                    "- Victim: Dr. Adrian Walter, found dead at night in the private office of his own house.\n" +
                    "- Initial public knowledge: the death happened in Walter's home office, but detailed crime scene clues " +
                    "should be treated as investigation discoveries, not common gossip.\n" +
                    "- Official story: exhaustion, guilt, or possible suicide; the town avoids the idea of murder.\n" +
                    "- Real killer in this canonical story: Liam Becker (Walter's student). " +
                    "NPCs may suspect others or be wrong, but must NOT claim with certainty that someone else is the true killer " +
                    "or that Walter is alive.\n" +
                    "Treat canon as a consistency guard; reveal details only when your identity and facts make it plausible. " +
                    "You may improvise feelings, wording and small memories, but keep these canon facts consistent. " +
                    "Do not invent time travel, other murders or resurrections.\n" +
                    "Always answer in Ukrainian, 1–3 short sentences, in first person (\"я\", \"мені\", \"мене\"). " +
                    "Do NOT introduce yourself by name or job unless the detective directly asks. " +
                    "Start your answer right away.\n";

    private final LlmClient llmClient;
    private final DossierDatabase dossierDb;
    private final ObjectMap<String, NpcState> npcStates = new ObjectMap<>();
    private final ObjectMap<String, float[]> factEmbeddingCache = new ObjectMap<>();
    private final ExecutorService npcQuestionExecutor = createExecutor("npc-question-worker");
    private final ExecutorService factRevealExecutor = createExecutor("npc-fact-worker");

    public NpcDialogueService(LlmClient llmClient, DossierDatabase dossierDb) {
        this.llmClient = llmClient;
        this.dossierDb = dossierDb;
    }

    private static ExecutorService createExecutor(String threadName) {
        return Executors.newSingleThreadExecutor(r -> {
            Thread thread = new Thread(r, threadName);
            thread.setDaemon(true);
            return thread;
        });
    }

    public NpcState getStateForUi(String npcId) {
        return getOrCreateState(npcId);
    }

    private void loadStateFromPrefs(String npcId, NpcState state, int hiddenCount) {
        Preferences prefs = Gdx.app.getPreferences(NPC_PREFS_NAME);

        state.trust = prefs.getFloat(npcId + ".trust", 0.5f);
        state.fear  = prefs.getFloat(npcId + ".fear", 0.2f);
        state.questionsAsked = prefs.getInteger(npcId + ".questionsTotal", 0);

        String hiddenStr = prefs.getString(npcId + ".hidden", "");

        if (state.hiddenRevealed == null || state.hiddenRevealed.length != hiddenCount) {
            state.hiddenRevealed = new boolean[hiddenCount];
        }

        if (!hiddenStr.isEmpty()) {
            String[] parts = hiddenStr.split(",");
            for (int i = 0; i < parts.length && i < state.hiddenRevealed.length; i++) {
                state.hiddenRevealed[i] = parts[i].trim().equals("1");
            }
        }

        state.lastQuestionTime = 0f;
    }

    private void saveStateToPrefs(String npcId, NpcState state) {
        Preferences prefs = Gdx.app.getPreferences(NPC_PREFS_NAME);

        prefs.putFloat(npcId + ".trust", state.trust);
        prefs.putFloat(npcId + ".fear",  state.fear);
        prefs.putInteger(npcId + ".questionsTotal", state.questionsAsked);

        if (state.hiddenRevealed != null) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < state.hiddenRevealed.length; i++) {
                if (i > 0) sb.append(",");
                sb.append(state.hiddenRevealed[i] ? "1" : "0");
            }
            prefs.putString(npcId + ".hidden", sb.toString());
        }

        prefs.flush();
    }

    private NpcState getOrCreateState(String npcId) {
        NpcState state = npcStates.get(npcId);
        if (state == null) {
            DossierData dossier = dossierDb != null ? dossierDb.characters.get(npcId) : null;
            int hiddenCount = (dossier != null && dossier.hiddenFacts != null)
                ? dossier.hiddenFacts.size()
                : 0;
            state = new NpcState(hiddenCount);
            state.id = npcId;

            loadStateFromPrefs(npcId, state, hiddenCount);

            npcStates.put(npcId, state);
        }
        return state;
    }

    private static final String[] LIE_LABELS = {
            "very rarely lies",
            "rarely lies",
            "lies moderately",
            "lies often",
            "almost always lies"
    };

    private String buildLieRiskBehavior(Integer lieRisk) {
        int risk = (lieRisk == null) ? 3 : Math.max(1, Math.min(5, lieRisk));
        String label = LIE_LABELS[risk - 1];
        return "LIE_RISK=" + risk + "/5 (" + label + ").";
    }

    private String buildSystemPrompt(String npcId, String currentBuildingId) {
        DossierData dossier = dossierDb != null ? dossierDb.characters.get(npcId) : null;
        NpcState state = getOrCreateState(npcId);

        StringBuilder sb = new StringBuilder();

        sb.append(GLOBAL_RULES).append("\n");

        if (dossier != null) {
            sb.append("NPC_IDENTITY:\n")
                    .append("name: ").append(dossier.name).append("\n")
                    .append("age: ").append(dossier.age).append("\n")
                    .append("role: ").append(dossier.role).append("\n");

            if (dossier.personality != null && !dossier.personality.isEmpty()) {
                sb.append("personality: ").append(dossier.personality).append("\n");
            }

            sb.append(buildLieRiskBehavior(dossier.lieRisk)).append("\n");
        } else {
            sb.append("NPC_IDENTITY:\n- name: ").append(npcId).append(" (keep consistent).\n");
        }

        sb.append("NPC_STATE: trust=").append(String.format(Locale.ROOT, "%.2f", state.trust))
                .append(", fear=").append(String.format(Locale.ROOT, "%.2f", state.fear))
                .append(" (0..1). If trust>0.7 and fear<0.4, be more open and honest. ")
                .append("If fear>0.7, avoid direct answers or distort truth, especially about dangerous topics.\n");

        if (currentBuildingId != null && !currentBuildingId.isEmpty()) {
            sb.append("LOCATION_CONTEXT:\n")
                .append("- current_building_id: ").append(currentBuildingId).append("\n")
                .append("- current_location_uk: ").append(LocationDescriptions.describe(currentBuildingId)).append("\n")
                .append("Treat this as your physical location right now. ")
                .append("If the detective asks where you are, why you are here, or what is around you, ")
                .append("answer consistently with this location. Do not claim to be in another building ")
                .append("unless you are describing a past event.\n");
        }

        if (dossier != null && dossier.publicFacts != null && !dossier.publicFacts.isEmpty()) {
            sb.append("FACTS_PUBLIC (you may mention freely when relevant):\n");
            for (String f : dossier.publicFacts) {
                sb.append("- ").append(f).append("\n");
            }
        }

        if (dossier != null && dossier.hiddenFacts != null && !dossier.hiddenFacts.isEmpty()) {
            if (state.hiddenRevealed == null || state.hiddenRevealed.length < dossier.hiddenFacts.size()) {
                state.hiddenRevealed = new boolean[dossier.hiddenFacts.size()];
            }

            boolean hasRevealed = false;
            for (int i = 0; i < dossier.hiddenFacts.size(); i++) {
                if (state.hiddenRevealed[i]) {
                    hasRevealed = true;
                    break;
                }
            }

            if (hasRevealed) {
                sb.append("FACTS_REVEALED (already clearly admitted, you cannot deny them):\n");
                for (int i = 0; i < dossier.hiddenFacts.size(); i++) {
                    if (state.hiddenRevealed[i]) {
                        sb.append("- ").append(dossier.getHiddenFactText(i)).append("\n");
                    }
                }
            }

            sb.append("FACTS_SECRET (do NOT state plainly unless the detective clearly presses you ")
                    .append("or you feel morally forced to confess; you may hint, dodge or partially admit):\n");
            sb.append("For greetings, small talk, or generic prompts, do not introduce secret topics yourself.\n");
            for (int i = 0; i < dossier.hiddenFacts.size(); i++) {
                if (!state.hiddenRevealed[i]) {
                    sb.append("- ").append(dossier.getHiddenFactText(i)).append("\n");
                }
            }
        }

        sb.append("Style: keep answers grounded in these facts, avoid long monologues, no meta-talk about being an AI.\n");

        return sb.toString();
    }

    public FactRevealDecision shouldRevealFactFromExchange(
        String question,
        String answer,
        DossierData.HiddenFactData hiddenFact
    ) throws IOException {
        if (hiddenFact == null || hiddenFact.text == null || hiddenFact.text.trim().isEmpty()) {
            return FactRevealDecision.no("empty hidden fact");
        }

        String system = "You are a strict fact-reveal classifier for a detective game.\n" +
            "Decide whether an NPC answer should unlock a hidden dossier fact.\n" +
            "You receive the hidden FACT, the detective QUESTION, the NPC ANSWER, and optional REQUIRED_EVIDENCE rules.\n" +
            "Rules:\n" +
            "- Return reveal=true only when the NPC ANSWER explicitly says or clearly paraphrases the FACT's key idea.\n" +
            "- The QUESTION may provide context only if the ANSWER clearly confirms, adopts, or admits it.\n" +
            "- The QUESTION alone must never unlock a fact.\n" +
            "- If the ANSWER merely invites more questions, changes topic, hints vaguely, greets, or says generic small talk, return false.\n" +
            "- REQUIRED_EVIDENCE_ANY means at least one item must be semantically present in the admitted evidence.\n" +
            "- REQUIRED_EVIDENCE_ALL_GROUPS means every group must be semantically satisfied by at least one item from that group.\n" +
            "- Evidence items are Ukrainian stems or phrase hints; match by meaning, inflection, synonym, and clear paraphrase, not by exact text.\n" +
            "- If the ANSWER denies, refuses, or is ambiguous, do not use the QUESTION as evidence.\n" +
            "- Example false: QUESTION='привіт', ANSWER='Питайте про Вальтера чи ту ніч' for a fact about seeing Walter after midnight.\n" +
            "- If you are not sure, return false.\n" +
            "Reply with JSON only, no markdown: " +
            "{\"reveal\":false,\"evidenceSatisfied\":false,\"reason\":\"short reason\",\"matchedEvidence\":\"short evidence summary\"}";

        String user = "FACT: \"" + safeText(hiddenFact.text) + "\"\n"
            + "QUESTION: \"" + safeText(question) + "\"\n"
            + "ANSWER: \"" + safeText(answer) + "\"\n"
            + "REQUIRED_EVIDENCE_ANY: " + formatEvidenceAny(hiddenFact.requiredEvidenceAny) + "\n"
            + "REQUIRED_EVIDENCE_ALL_GROUPS: " + formatEvidenceGroups(hiddenFact.requiredEvidenceAllGroups) + "\n"
            + "Decide whether to unlock the fact.";

        String result = llmClient.ask(system, user);
        return parseFactRevealDecision(result, hasEvidenceRequirements(hiddenFact));
    }

    private FactRevealDecision parseFactRevealDecision(String result, boolean hasEvidenceRequirements) {
        if (result == null || result.trim().isEmpty()) {
            return FactRevealDecision.no("empty classifier response");
        }

        String json = extractJsonObject(result);
        if (json.isEmpty()) {
            return FactRevealDecision.no("classifier returned no json");
        }

        try {
            JsonValue root = new JsonReader().parse(json);
            boolean reveal = root.getBoolean("reveal", false);
            boolean evidenceSatisfied = root.getBoolean("evidenceSatisfied", !hasEvidenceRequirements);
            String reason = root.getString("reason", "");
            String matchedEvidence = root.getString("matchedEvidence", "");
            return new FactRevealDecision(reveal && evidenceSatisfied, evidenceSatisfied, reason, matchedEvidence);
        } catch (Exception ex) {
            Gdx.app.log("FACT_DEBUG", "Failed to parse fact reveal classifier JSON: " + result);
            return FactRevealDecision.no("invalid classifier json");
        }
    }

    private String extractJsonObject(String text) {
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start < 0 || end <= start) return "";
        return text.substring(start, end + 1);
    }

    private boolean hasEvidenceRequirements(DossierData.HiddenFactData hiddenFact) {
        return hiddenFact != null && (
            (hiddenFact.requiredEvidenceAny != null && !hiddenFact.requiredEvidenceAny.isEmpty())
                || (hiddenFact.requiredEvidenceAllGroups != null && !hiddenFact.requiredEvidenceAllGroups.isEmpty())
        );
    }

    private String formatEvidenceAny(List<String> values) {
        if (values == null || values.isEmpty()) return "[]";
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append("\"").append(safeText(values.get(i))).append("\"");
        }
        sb.append("]");
        return sb.toString();
    }

    private String formatEvidenceGroups(List<List<String>> groups) {
        if (groups == null || groups.isEmpty()) return "[]";
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < groups.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(formatEvidenceAny(groups.get(i)));
        }
        sb.append("]");
        return sb.toString();
    }

    private String safeText(String text) {
        if (text == null) return "";
        return text.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    public static class FactRevealDecision {
        public final boolean reveal;
        public final boolean evidenceSatisfied;
        public final String reason;
        public final String matchedEvidence;

        FactRevealDecision(boolean reveal, boolean evidenceSatisfied, String reason, String matchedEvidence) {
            this.reveal = reveal;
            this.evidenceSatisfied = evidenceSatisfied;
            this.reason = reason != null ? reason : "";
            this.matchedEvidence = matchedEvidence != null ? matchedEvidence : "";
        }

        static FactRevealDecision no(String reason) {
            return new FactRevealDecision(false, false, reason, "");
        }

        String debugSuffix() {
            StringBuilder sb = new StringBuilder();
            sb.append(" evidence=").append(evidenceSatisfied);
            if (!matchedEvidence.isEmpty()) {
                sb.append(" matched=\"").append(matchedEvidence).append("\"");
            }
            if (!reason.isEmpty()) {
                sb.append(" reason=\"").append(reason).append("\"");
            }
            return sb.toString();
        }
    }

    public IntArray findRelevantHiddenFacts(
        String npcId,
        DossierData data,
        String question,
        String answer
    ) throws IOException {
        IntArray candidates = new IntArray();
        if (npcId == null || data == null || data.hiddenFacts == null || data.hiddenFacts.isEmpty()) {
            return candidates;
        }

        String exchangeText = buildExchangeEmbeddingText(question, answer);
        if (exchangeText.isEmpty()) {
            return candidates;
        }

        ArrayList<String> inputs = new ArrayList<>();
        ArrayList<String> missingCacheKeys = new ArrayList<>();
        int unrevealedFactCount = 0;

        inputs.add(exchangeText);

        for (int i = 0; i < data.hiddenFacts.size(); i++) {
            String hiddenFact = data.getHiddenFactText(i);
            if (hiddenFact.isEmpty()) continue;
            if (isFactRevealed(npcId, i)) continue;

            unrevealedFactCount++;
            String cacheKey = buildFactEmbeddingCacheKey(npcId, i, hiddenFact);
            if (!factEmbeddingCache.containsKey(cacheKey)) {
                missingCacheKeys.add(cacheKey);
                inputs.add(buildFactEmbeddingText(data, hiddenFact));
            }
        }

        if (unrevealedFactCount == 0) {
            return candidates;
        }

        float[][] embeddings = llmClient.createEmbeddings(inputs);
        float[] exchangeEmbedding = embeddings[0];

        for (int i = 0; i < missingCacheKeys.size(); i++) {
            factEmbeddingCache.put(missingCacheKeys.get(i), embeddings[i + 1]);
        }

        List<FactCandidate> ranked = new ArrayList<>();
        for (int i = 0; i < data.hiddenFacts.size(); i++) {
            String hiddenFact = data.getHiddenFactText(i);
            if (hiddenFact.isEmpty()) continue;
            if (isFactRevealed(npcId, i)) continue;

            String cacheKey = buildFactEmbeddingCacheKey(npcId, i, hiddenFact);
            float[] factEmbedding = factEmbeddingCache.get(cacheKey);
            if (factEmbedding == null) continue;

            float similarity = cosineSimilarity(exchangeEmbedding, factEmbedding);
            Gdx.app.log("FACT_DEBUG",
                "SEMANTIC_SCORE (npc=" + npcId + ") fact #" + i + " -> "
                    + String.format(Locale.ROOT, "%.3f", similarity));

            ranked.add(new FactCandidate(i, similarity));
        }

        ranked.sort((a, b) -> Float.compare(b.similarity, a.similarity));

        int maxCandidates = Math.min(FACT_RETRIEVAL_TOP_K, ranked.size());
        for (int i = 0; i < maxCandidates; i++) {
            FactCandidate candidate = ranked.get(i);
            boolean aboveMainThreshold = candidate.similarity >= FACT_RETRIEVAL_MIN_SIMILARITY;
            boolean usefulFallback = candidates.size == 0
                && candidate.similarity >= FACT_RETRIEVAL_FALLBACK_SIMILARITY;

            if (!aboveMainThreshold && !usefulFallback) {
                continue;
            }

            Gdx.app.log("FACT_DEBUG",
                "SEMANTIC_CANDIDATE (npc=" + npcId + ") fact #" + candidate.index + " -> "
                    + String.format(Locale.ROOT, "%.3f", candidate.similarity));
            candidates.add(candidate.index);
        }

        return candidates;
    }

    public boolean isFactRevealed(String npcId, int factIndex) {
        if (npcId == null || factIndex < 0) return false;

        NpcState state = getOrCreateState(npcId);
        return state.hiddenRevealed != null
            && factIndex < state.hiddenRevealed.length
            && state.hiddenRevealed[factIndex];
    }

    public void markFactsRevealed(String npcId, IntArray factIndexes) {
        if (factIndexes == null || factIndexes.size == 0) return;

        NpcState state = getOrCreateState(npcId);
        for (int i = 0; i < factIndexes.size; i++) {
            int idx = factIndexes.get(i);
            if (idx >= 0 && idx < state.hiddenRevealed.length) {
                state.hiddenRevealed[idx] = true;
            }
        }

        saveStateToPrefs(npcId, state);
    }

    public String askNpcSync(String npcId, String question) throws IOException {
        return askNpcSync(npcId, question, null);
    }

    public void askNpcAsync(
        String npcId,
        String question,
        String currentBuildingId,
        AsyncCallback<String> callback
    ) {
        npcQuestionExecutor.submit(() -> {
            try {
                callback.onComplete(askNpcSync(npcId, question, currentBuildingId), null);
            } catch (Throwable error) {
                callback.onComplete(null, error);
            }
        });
    }

    public <T> void runFactRevealCheckAsync(AsyncTask<T> task, AsyncCallback<T> callback) {
        factRevealExecutor.submit(() -> {
            try {
                callback.onComplete(task.run(), null);
            } catch (Throwable error) {
                callback.onComplete(null, error);
            }
        });
    }

    public String askNpcSync(String npcId, String question, String currentBuildingId) throws IOException {
        NpcState state = getOrCreateState(npcId);
        state.questionsAsked += 1;

        String systemPrompt = buildSystemPrompt(npcId, currentBuildingId);
        String userMessage  = buildUserMessageWithHistory(npcId, question);

        String answer = llmClient.ask(systemPrompt, userMessage);

        updateStateAfterExchange(state, question);

        saveStateToPrefs(npcId, state);

        return answer;
    }

    public void dispose() {
        npcQuestionExecutor.shutdownNow();
        factRevealExecutor.shutdownNow();
    }

    private String buildExchangeEmbeddingText(String question, String answer) {
        String q = question != null ? question.trim() : "";
        String a = answer != null ? answer.trim() : "";
        if (q.isEmpty() && a.isEmpty()) return "";

        return "Питання детектива: " + q + "\nВідповідь персонажа: " + a;
    }

    private String buildFactEmbeddingText(DossierData data, String hiddenFact) {
        String name = data.name != null ? data.name : "";
        String role = data.role != null ? data.role : "";
        return "Прихований факт персонажа. Ім'я: " + name
            + ". Роль: " + role
            + ". Факт: " + hiddenFact;
    }

    private String buildFactEmbeddingCacheKey(String npcId, int factIndex, String hiddenFact) {
        return npcId + ":" + factIndex + ":" + hiddenFact.hashCode();
    }

    private float cosineSimilarity(float[] a, float[] b) {
        if (a == null || b == null) return 0f;

        int length = Math.min(a.length, b.length);
        if (length == 0) return 0f;

        double dot = 0;
        double aNorm = 0;
        double bNorm = 0;

        for (int i = 0; i < length; i++) {
            dot += a[i] * b[i];
            aNorm += a[i] * a[i];
            bNorm += b[i] * b[i];
        }

        if (aNorm == 0 || bNorm == 0) return 0f;
        return (float) (dot / (Math.sqrt(aNorm) * Math.sqrt(bNorm)));
    }

    private static class FactCandidate {
        final int index;
        final float similarity;

        FactCandidate(int index, float similarity) {
            this.index = index;
            this.similarity = similarity;
        }
    }

    private void updateStateAfterExchange(NpcState state, String question) {
            if (question == null) question = "";

            String qLower = question.toLowerCase(Locale.ROOT);

            float nowSec = TimeUtils.millis() / 1000f;
            float dt = (state.lastQuestionTime > 0f)
                ? (nowSec - state.lastQuestionTime)
                : 9999f;
            state.lastQuestionTime = nowSec;

            if (dt > SESSION_BREAK_SECONDS) {
                state.questionsAsked = 1;
            }

            if (dt < 5f) {
                adjustTrustFear(state, -0.03f, 0.06f);
            } else if (dt < 20f) {
                adjustTrustFear(state, 0.0f, 0.0f);
            } else if (dt > 60f) {
                adjustTrustFear(state, 0.02f, -0.03f);
            }

            if (state.questionsAsked > 8 && state.questionsAsked <= 15) {
                adjustTrustFear(state, -0.01f, 0.02f);
            } else if (state.questionsAsked > 15) {
                adjustTrustFear(state, -0.02f, 0.04f);
            }

            if (containsAny(qLower, RUDE_WORDS)) {
                adjustTrustFear(state, -0.10f, 0.12f);
            }

            if (containsAny(qLower, POLITE_WORDS)) {
                adjustTrustFear(state, 0.06f, -0.04f);
            }

            if (containsAny(qLower, ACCUSATION_WORDS)) {
                adjustTrustFear(state, -0.04f, 0.08f);
            }

            float jitterTrust = MathUtils.random(-0.01f, 0.01f);
            float jitterFear  = MathUtils.random(-0.01f, 0.01f);
            adjustTrustFear(state, jitterTrust, jitterFear);

            Gdx.app.log("NPC_STATE", " trust=" + state.trust + " fear=" + state.fear);
        }

        private void adjustTrustFear(NpcState state, float dTrust, float dFear) {
            state.trust = clamp01(state.trust + dTrust);
            state.fear  = clamp01(state.fear  + dFear);
        }

        private float clamp01(float v) {
            if (v < 0f) return 0f;
            return Math.min(v, 1f);
        }

        private boolean containsAny(String textLower, String[] patterns) {
            if (textLower == null || textLower.isEmpty() || patterns == null) return false;
            for (String p : patterns) {
                if (p == null || p.isEmpty()) continue;
                if (textLower.contains(p)) return true;
            }
            return false;
        }

        private static final String[] RUDE_WORDS = {
            "дур", "тупа", "тупий", "брехун", "брешеш", "заткнись", "ненормальна"
        };

        private static final String[] POLITE_WORDS = {
            "дякую", "спасибі", "будь ласка", "перепрошую", "вибач",
            "дякую тобі", "дякую вам"
        };

        private static final String[] ACCUSATION_WORDS = {
            "ти винна", "ти винен", "це ти зробила", "це ти зробив",
            "ти вбила", "ти вбив", "ти щось приховуєш", "збрехала", "збрехав"
        };

        private String buildUserMessageWithHistory(String npcId, String question) {
            String history = DialogueHistory.loadRecentForLlm(
                npcId,
                MAX_HISTORY_PAIRS,
                MAX_HISTORY_CHARS
            );

            StringBuilder sb = new StringBuilder();

            if (!history.isEmpty()) {
                sb.append("A brief fragment from the previous chat between the detective and you:\n");
                sb.append(history).append("\n\n");
            }

            sb.append("Current question: ").append(question);

            return sb.toString();
        }

        public int getTotalRevealedFacts() {
            int total = 0;
            for (ObjectMap.Entry<String, NpcState> e : npcStates) {
                boolean[] arr = e.value.hiddenRevealed;
                if (arr == null) continue;
                for (boolean b : arr) {
                    if (b) total++;
                }
            }
            return total;
        }

    public void resetAllNpcState() {
        npcStates.clear();
    }
}
