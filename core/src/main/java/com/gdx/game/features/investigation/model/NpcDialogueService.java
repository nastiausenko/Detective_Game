package com.gdx.game.features.investigation.model;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.IntArray;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.TimeUtils;
import com.gdx.game.model.DialogueHistory;
import com.gdx.game.model.DossierData;
import com.gdx.game.model.DossierDatabase;
import com.gdx.game.model.NpcState;
import com.gdx.game.model.LocationDescriptions;
import com.gdx.game.shared.api.LlmClient;

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

    private static final int MAX_HISTORY_PAIRS = 4;
    private static final int MAX_HISTORY_CHARS = 700;
    private static final int NPC_REPLY_MAX_TOKENS = 220;
    private static final int NPC_OPENAI_READ_TIMEOUT_MS = 12000;
    private static final int FACT_REVEAL_MAX_TOKENS = 220;
    private static final int FACT_RETRIEVAL_TOP_K = 3;
    private static final float FACT_RETRIEVAL_MIN_SIMILARITY = 0.28f;
    private static final float FACT_RETRIEVAL_FALLBACK_SIMILARITY = 0.20f;
    private static final float SESSION_BREAK_SECONDS = 180f;
    private static final float RAPID_QUESTION_SECONDS = 5f;
    private static final float CALM_PAUSE_SECONDS = 60f;
    private static final String NPC_PREFS_NAME = "npc_state";
    private static final String GLOBAL_RULES =
        "NPC in Rosenfeld detective story. Canon: Walter is dead, found in his home office, not hospital; public version exhaustion/guilt/suicide; real killer Liam Becker. " +
            "Never say Walter lives or another person is certainly the killer. Setting: 1940s small town; people live in houses, not apartments. " +
            "Never use modern apartment-building words like під'їзд, квартира, ліфт, поверх;\n" +
            "Reply in natural Ukrainian, 1-2 short spoken sentences, first person. No headings/lists/labels/AI talk. " +
            "Answer only the question; do not volunteer name, role, location, alibi, backstory, or case facts. " +
            "For greetings/small talk: brief human reply only. Use only known/provided named people; invent no names.\n";

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
        return "LIE_RISK=" + risk + "/5 (" + label + "); affects evasiveness, not truth of provided facts.";
    }

    private void appendKnownPeopleContext(StringBuilder sb) {
        if (dossierDb == null || dossierDb.characters == null || dossierDb.characters.isEmpty()) {
            return;
        }

        sb.append("KNOWN: ");

        boolean first = true;
        for (ObjectMap.Entry<String, DossierData> e : dossierDb.characters) {
            DossierData data = e.value;
            if (data == null || data.name == null || data.name.trim().isEmpty()) continue;

            if (!first) sb.append("; ");
            first = false;

            sb.append(data.name);
            if (data.role != null && !data.role.trim().isEmpty()) {
                sb.append("(").append(data.role).append(")");
            }
        }
        sb.append(". You know them; ignore/reject other names from chat.\n");
    }

    private String buildSystemPrompt(String npcId, String currentBuildingId, String question) {
        DossierData dossier = dossierDb != null ? dossierDb.characters.get(npcId) : null;
        NpcState state = getOrCreateState(npcId);

        StringBuilder sb = new StringBuilder();

        sb.append(GLOBAL_RULES).append("\n");

        if (dossier != null) {
            sb.append("YOU: ")
                .append(dossier.name)
                .append(", ")
                .append(dossier.age)
                .append(", ")
                .append(dossier.role)
                .append(".\n");

            if (dossier.personality != null && !dossier.personality.isEmpty()) {
                sb.append("TRAIT: ").append(dossier.personality).append("\n");
            }

            sb.append(buildLieRiskBehavior(dossier.lieRisk)).append("\n");
        } else {
            sb.append("YOU: ").append(npcId).append(".\n");
        }

        appendKnownPeopleContext(sb);

        sb.append("STATE: trust=").append(String.format(Locale.ROOT, "%.2f", state.trust))
                .append(", fear=").append(String.format(Locale.ROOT, "%.2f", state.fear))
                .append(". High trust=open; high fear=dodge/distort danger.\n");

        if (currentBuildingId != null && !currentBuildingId.isEmpty()) {
            sb.append("NOW: ")
                .append(currentBuildingId)
                .append(" / ")
                .append(LocationDescriptions.describe(currentBuildingId))
                .append(". Use only for here/now questions, never for greetings or past-time questions.\n");
        }

        if (dossier != null && dossier.publicFacts != null && !dossier.publicFacts.isEmpty()) {
            sb.append("PUBLIC:\n");
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
                sb.append("ADMITTED:\n");
                for (int i = 0; i < dossier.hiddenFacts.size(); i++) {
                    if (state.hiddenRevealed[i]) {
                        sb.append("- ").append(dossier.getHiddenFactText(i)).append("\n");
                    }
                }
            }

            appendHiddenTruths(sb, dossier, state);

            sb.append("SECRET_RULE: SECRET_TRUE facts are true and known by you. ")
                .append("If Q asks directly or asks about the same person/place/time/event, do not deny, move to another day, or contradict person/place/time. ")
                .append("If Q asks whether you saw anyone at a secret place/time, name the person from the secret instead of saying no one. ")
                .append("Give a brief guarded admission or partial truth containing the core fact. ")
                .append("Broad questions use PUBLIC/TRAIT/STATE/ADMITTED; low trust/fear may make you reluctant, but not say 'no' to a true secret.\n");
        }

        sb.append("JSON only {\"a\":\"answer\",\"rapport\":0,\"pressure\":0,\"hostility\":0}. ")
            .append("Rate current question: rapport -1..1, pressure/hostility 0..1. Ground a in facts.\n");

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

        String missingEntity = findMissingRequiredEntity(hiddenFact.text, question, answer);
        if (!missingEntity.isEmpty()) {
            return FactRevealDecision.no("required named entity missing from answer: " + missingEntity);
        }

        String missingConcept = findMissingCriticalConcept(hiddenFact.text, question, answer);
        if (!missingConcept.isEmpty()) {
            return FactRevealDecision.no("required concept missing from answer: " + missingConcept);
        }

        String system = "Strict textual-entailment dossier unlock classifier. Output JSON only.\n" +
            "Split FACT into core: subject(s), predicate/relation/action/attribute, object/result, time/place if present. " +
            "reveal=true only if NPC ANSWER entails every required core part. " +
            "A shared name, topic, location, suspicion, or vague association is never enough. QUESTION alone never counts; use it only when ANSWER confirms/adopts it. " +
            "False for denial, contradiction, moving the fact to another time/place/person, refusal, ambiguity, vague hint, greeting, topic change, or generic small talk. " +
            "Presence/sighting/suspicious behavior does not support identity/experiment/patient/project facts unless the ANSWER says that relation. " +
            "Match meaning across language, inflection, synonym, and paraphrase, but require the same core claim. Unsure=false.\n" +
            "Examples: FACT 'Ліам був його найуспішнішим експериментом' + ANSWER 'бачила Ліама вночі' => predicateSupported=false, reveal=false. " +
            "Same FACT + ANSWER 'Вальтер називав Ліама доказом успіху дослідів' => predicateSupported=true, reveal=true. " +
            "FACT 'приховував нічні візити Вальтера й гостей' + ANSWER 'сторожу лікарню по ночах, щоб зайві не шастали' => predicateSupported=false, reveal=false. " +
            "FACT 'Бачила Ліама біля будинку Вальтера після опівночі' + ANSWER 'вночі біля дому бачила Ліама' => reveal=true.\n" +
            "{\"reveal\":false,\"subjectSupported\":false,\"predicateSupported\":false,\"objectSupported\":false,\"timePlaceSupported\":true,\"evidenceSatisfied\":false,\"reason\":\"short reason\",\"matchedEvidence\":\"short evidence summary\"}";

        String user = "FACT: \"" + safeText(hiddenFact.text) + "\"\n"
            + "QUESTION: \"" + safeText(question) + "\"\n"
            + "ANSWER: \"" + safeText(answer) + "\"";

        String result = llmClient.ask(system, user, FACT_REVEAL_MAX_TOKENS, LlmClient.ModelTier.FAST);
        return parseFactRevealDecision(result);
    }

    private FactRevealDecision parseFactRevealDecision(String result) {
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
            boolean evidenceSatisfied = root.getBoolean("evidenceSatisfied", reveal);
            boolean subjectSupported = root.getBoolean("subjectSupported", reveal);
            boolean predicateSupported = root.getBoolean("predicateSupported", false);
            boolean objectSupported = root.getBoolean("objectSupported", reveal);
            boolean timePlaceSupported = root.getBoolean("timePlaceSupported", true);
            String reason = root.getString("reason", "");
            String matchedEvidence = root.getString("matchedEvidence", "");
            boolean coreSupported = subjectSupported && predicateSupported && objectSupported && timePlaceSupported;
            boolean finalReveal = (reveal || evidenceSatisfied) && coreSupported;
            if ((reveal || evidenceSatisfied) && !coreSupported && reason.isEmpty()) {
                reason = "core claim is not fully supported by answer";
            }
            return new FactRevealDecision(
                finalReveal,
                evidenceSatisfied && coreSupported,
                reason,
                matchedEvidence
            );
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

    private void appendHiddenTruths(
        StringBuilder sb,
        DossierData dossier,
        NpcState state
    ) {
        if (dossier == null || dossier.hiddenFacts == null || dossier.hiddenFacts.isEmpty()) return;

        boolean wroteHeader = false;
        for (int i = 0; i < dossier.hiddenFacts.size(); i++) {
            if (state.hiddenRevealed != null && i < state.hiddenRevealed.length && state.hiddenRevealed[i]) {
                continue;
            }

            String hiddenFact = dossier.getHiddenFactText(i);
            if (hiddenFact.isEmpty()) continue;

            if (!wroteHeader) {
                sb.append("SECRET_TRUE:\n");
                wroteHeader = true;
            }
            sb.append("- ").append(hiddenFact).append("\n");
        }
    }

    private boolean containsAnyEvidence(String answerText, List<String> evidence) {
        if (answerText == null || answerText.isEmpty() || evidence == null) return false;

        for (String item : evidence) {
            String marker = normalizeEvidenceText(item);
            if (marker.isEmpty()) continue;
            if (answerText.contains(marker)) {
                return true;
            }
        }

        return false;
    }

    private String normalizeEvidenceText(String text) {
        if (text == null) return "";

        return text
            .toLowerCase(Locale.ROOT)
            .replace('ґ', 'г')
            .replace('є', 'е')
            .replace('ї', 'і')
            .replace('й', 'и')
            .replace('’', '\'')
            .replace('ʼ', '\'')
            .replace('`', '\'')
            .replaceAll("[^а-яa-z0-9і'\\s]", " ")
            .replaceAll("\\s+", " ")
            .trim();
    }

    private String safeText(String text) {
        if (text == null) return "";
        return text.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private String findMissingRequiredEntity(String fact, String question, String answer) {
        if (dossierDb == null || dossierDb.characters == null || dossierDb.characters.isEmpty()) {
            return "";
        }

        String factText = normalizeEvidenceText(fact);
        String questionText = normalizeEvidenceText(question);
        String answerText = normalizeEvidenceText(answer);
        boolean answerAdoptsQuestion = answerAdoptsQuestion(questionText, answerText);

        for (ObjectMap.Entry<String, DossierData> e : dossierDb.characters) {
            DossierData data = e.value;
            if (data == null || data.name == null) continue;

            List<String> aliases = buildEntityAliases(data.name);
            if (!containsAnyPlain(factText, aliases)) continue;

            boolean answerHasEntity = containsAnyPlain(answerText, aliases);
            boolean questionHasAdoptedEntity = answerAdoptsQuestion && containsAnyPlain(questionText, aliases);
            if (!answerHasEntity && !questionHasAdoptedEntity) {
                return data.name;
            }
        }

        return "";
    }

    private List<String> buildEntityAliases(String name) {
        ArrayList<String> aliases = new ArrayList<>();
        String normalizedName = normalizeEvidenceText(name);
        if (normalizedName.isEmpty()) return aliases;

        aliases.add(normalizedName);
        String[] parts = normalizedName.split(" ");
        for (String part : parts) {
            if (part.length() < 3) continue;
            if ("доктор".equals(part) || "dr".equals(part)) continue;
            aliases.add(part);
        }
        return aliases;
    }

    private boolean containsAnyPlain(String text, List<String> aliases) {
        if (text == null || text.isEmpty() || aliases == null) return false;
        for (String alias : aliases) {
            if (alias == null || alias.isEmpty()) continue;
            if (text.contains(alias)) return true;
        }
        return false;
    }

    private boolean answerAdoptsQuestion(String questionText, String answerText) {
        if (questionText == null || questionText.isEmpty() || answerText == null || answerText.isEmpty()) {
            return false;
        }

        return answerText.equals("так")
            || answerText.startsWith("так ")
            || answerText.startsWith("авжеж")
            || answerText.startsWith("звісно")
            || answerText.startsWith("саме так")
            || answerText.startsWith("я це")
            || answerText.startsWith("це ")
            || questionText.startsWith("що за ")
            || questionText.startsWith("які саме ")
            || questionText.startsWith("розкажи про ")
            || questionText.startsWith("що це за ");
    }

    private String findMissingCriticalConcept(String fact, String question, String answer) {
        String factText = normalizeEvidenceText(fact);
        String questionText = normalizeEvidenceText(question);
        String answerText = normalizeEvidenceText(answer);
        boolean answerAdoptsQuestion = answerAdoptsQuestion(questionText, answerText);

        String[][] conceptGroups = {
            {"експеримент", "експеримент", "дослід", "дослідж", "project", "experiment"},
            {"діти/пацієнти", "дит", "діт", "пацієнт", "клінік", "child", "patient", "clinic"},
            {"Ліам", "ліам", "liam"},
            {"проєкт", "проєкт", "проект", "project"},
            {"психологічні тести", "тест", "психолог", "fear", "guilt", "empathy", "страх", "провин", "емпат"},
            {"записка", "записк", "лист", "note"},
            {"скальпель", "скальпел", "інструмент", "scalpel"}
        };

        for (String[] group : conceptGroups) {
            if (!containsConcept(factText, group)) continue;
            if (containsConcept(answerText, group)) continue;
            if (containsConcept(questionText, group) && answerProvidesSubstantiveDetail(answerText)) continue;
            return group[0];
        }

        return "";
    }

    private boolean answerProvidesSubstantiveDetail(String answerText) {
        if (answerText == null || answerText.isEmpty()) return false;

        String[] words = answerText.split(" ");
        if (words.length >= 6) return true;

        return answerText.contains("це ")
            || answerText.contains("там ")
            || answerText.contains("запис")
            || answerText.contains("результ")
            || answerText.contains("метод")
            || answerText.contains("протокол")
            || answerText.contains("спостереж")
            || answerText.contains("процедур");
    }

    private boolean containsConcept(String text, String[] conceptGroup) {
        if (text == null || text.isEmpty() || conceptGroup == null) return false;

        for (int i = 1; i < conceptGroup.length; i++) {
            String marker = normalizeEvidenceText(conceptGroup[i]);
            if (marker.isEmpty()) continue;
            if (text.contains(marker)) return true;
        }
        return false;
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

        addEvidenceMatchedCandidates(candidates, data, question, answer, npcId);

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

        float[][] embeddings;
        try {
            embeddings = llmClient.createEmbeddings(inputs);
        } catch (IOException ex) {
            Gdx.app.log("FACT_DEBUG", "Embedding retrieval failed; using non-embedding candidate fallback for npc=" + npcId);
            addEmbeddingFallbackCandidates(candidates, data, npcId);
            return candidates;
        }
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
            addUniqueCandidate(candidates, candidate.index);
        }

        return candidates;
    }

    private void addEmbeddingFallbackCandidates(IntArray candidates, DossierData data, String npcId) {
        if (data == null || data.hiddenFacts == null || data.hiddenFacts.isEmpty()) return;

        for (int i = 0; i < data.hiddenFacts.size(); i++) {
            String hiddenFact = data.getHiddenFactText(i);
            if (hiddenFact.isEmpty()) continue;
            if (isFactRevealed(npcId, i)) continue;

            Gdx.app.log("FACT_DEBUG", "EMBEDDING_FALLBACK_CANDIDATE (npc=" + npcId + ") fact #" + i);
            addUniqueCandidate(candidates, i);
        }
    }

    private void addEvidenceMatchedCandidates(
        IntArray candidates,
        DossierData data,
        String question,
        String answer,
        String npcId
    ) {
        String exchangeText = normalizeEvidenceText(
            (question != null ? question : "") + " " + (answer != null ? answer : "")
        );
        if (exchangeText.isEmpty()) return;

        for (int i = 0; i < data.hiddenFacts.size(); i++) {
            String hiddenFact = data.getHiddenFactText(i);
            if (hiddenFact.isEmpty()) continue;
            if (isFactRevealed(npcId, i)) continue;

            DossierData.HiddenFactData fact = data.getHiddenFact(i);
            if (!hasEvidenceRequirements(fact)) continue;
            if (!matchesAnyEvidenceRequirement(exchangeText, fact)) continue;

            Gdx.app.log("FACT_DEBUG", "EVIDENCE_CANDIDATE (npc=" + npcId + ") fact #" + i);
            addUniqueCandidate(candidates, i);
        }
    }

    private boolean matchesAnyEvidenceRequirement(String normalizedExchange, DossierData.HiddenFactData fact) {
        if (fact == null) return false;
        if (fact.requiredEvidenceAny != null && !fact.requiredEvidenceAny.isEmpty()) {
            return containsAnyEvidence(normalizedExchange, fact.requiredEvidenceAny);
        }
        if (fact.requiredEvidenceAllGroups == null || fact.requiredEvidenceAllGroups.isEmpty()) {
            return false;
        }

        int matchedGroups = 0;
        for (List<String> group : fact.requiredEvidenceAllGroups) {
            if (group == null || group.isEmpty()) continue;
            if (containsAnyEvidence(normalizedExchange, group)) {
                matchedGroups++;
            }
        }
        return matchedGroups >= Math.min(2, fact.requiredEvidenceAllGroups.size());
    }

    private void addUniqueCandidate(IntArray candidates, int index) {
        if (!candidates.contains(index)) {
            candidates.add(index);
        }
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

        String systemPrompt = buildSystemPrompt(npcId, currentBuildingId, question);
        String userMessage  = buildUserMessageWithHistory(npcId, question);

        NpcReply reply = parseNpcReply(
            llmClient.ask(
                systemPrompt,
                userMessage,
                NPC_REPLY_MAX_TOKENS,
                LlmClient.ModelTier.SMART,
                NPC_OPENAI_READ_TIMEOUT_MS
            )
        );

        updateStateAfterExchange(state, reply.tone);

        saveStateToPrefs(npcId, state);

        return reply.answer;
    }

    public void dispose() {
        npcQuestionExecutor.shutdownNow();
        factRevealExecutor.shutdownNow();
    }

    private String buildExchangeEmbeddingText(String question, String answer) {
        String q = question != null ? question.trim() : "";
        String a = answer != null ? answer.trim() : "";

        if (a.isEmpty()) return "";

        return "Відповідь персонажа, яка може розкрити прихований факт: " + a +
            "\nПитання лише як контекст, не як доказ: " + q;
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

    private static class NpcReply {
        final String answer;
        final ToneMetrics tone;

        NpcReply(String answer, ToneMetrics tone) {
            this.answer = answer != null ? answer : "";
            this.tone = tone;
        }
    }

    private static class ToneMetrics {
        final float rapport;
        final float pressure;
        final float hostility;

        ToneMetrics(float rapport, float pressure, float hostility) {
            this.rapport = rapport;
            this.pressure = pressure;
            this.hostility = hostility;
        }

        static ToneMetrics neutral() {
            return new ToneMetrics(0f, 0f, 0f);
        }
    }

    private NpcReply parseNpcReply(String result) {
        if (result == null || result.trim().isEmpty()) {
            return new NpcReply("", ToneMetrics.neutral());
        }

        String json = extractJsonObject(result);
        if (!json.isEmpty()) {
            try {
                JsonValue root = new JsonReader().parse(json);
                String answer = root.getString("a", "").trim();
                if (answer.isEmpty()) {
                    answer = root.getString("answer", "").trim();
                }
                ToneMetrics tone = new ToneMetrics(
                    root.getFloat("rapport", 0f),
                    root.getFloat("pressure", 0f),
                    root.getFloat("hostility", 0f)
                );
                return new NpcReply(answer.isEmpty() ? result.trim() : answer, tone);
            } catch (Exception ex) {
                Gdx.app.log("LLM", "Failed to parse NPC JSON reply: " + result);
            }
        }

        return new NpcReply(result.trim(), ToneMetrics.neutral());
    }

    private void updateStateAfterExchange(NpcState state, ToneMetrics tone) {
        ToneMetrics normalizedTone = normalizeTone(tone);
        float nowSec = TimeUtils.millis() / 1000f;
        float dt = (state.lastQuestionTime > 0f)
            ? (nowSec - state.lastQuestionTime)
            : 9999f;
        state.lastQuestionTime = nowSec;

        if (dt > SESSION_BREAK_SECONDS) {
            state.questionsAsked = 1;
        }

        applyPacingImpact(state, dt, normalizedTone);

        if (dt > CALM_PAUSE_SECONDS) {
            adjustTrustFear(state, 0.02f, -0.03f);
        }

        if (state.questionsAsked > 8 && state.questionsAsked <= 15) {
            adjustTrustFear(state, -0.004f, 0.008f);
        } else if (state.questionsAsked > 15) {
            adjustTrustFear(state, -0.008f, 0.015f);
        }

        applyToneImpact(state, normalizedTone);

        float jitterTrust = MathUtils.random(-0.003f, 0.003f);
        float jitterFear  = MathUtils.random(-0.003f, 0.003f);
        adjustTrustFear(state, jitterTrust, jitterFear);

        Gdx.app.log("NPC_STATE", " trust=" + state.trust + " fear=" + state.fear);
    }

    private ToneMetrics normalizeTone(ToneMetrics tone) {
        if (tone == null) {
            return ToneMetrics.neutral();
        }

        return new ToneMetrics(
            MathUtils.clamp(tone.rapport, -1f, 1f),
            MathUtils.clamp(tone.pressure, 0f, 1f),
            MathUtils.clamp(tone.hostility, 0f, 1f)
        );
    }

    private void applyPacingImpact(NpcState state, float dt, ToneMetrics tone) {
        if (dt >= RAPID_QUESTION_SECONDS) return;

        float stress = MathUtils.clamp(tone.pressure + tone.hostility * 1.5f, 0f, 1f);
        if (stress <= 0.15f) {
            return;
        }

        adjustTrustFear(state, -0.01f * stress, 0.025f * stress);
    }

    private void applyToneImpact(NpcState state, ToneMetrics tone) {
        if (tone == null) return;

        float rapport = tone.rapport;
        float pressure = tone.pressure;
        float hostility = tone.hostility;

        float dTrust = rapport * 0.04f - pressure * 0.025f - hostility * 0.08f;
        float dFear = -Math.max(rapport, 0f) * 0.025f + pressure * 0.05f + hostility * 0.09f;

        adjustTrustFear(state, dTrust, dFear);
    }

    private void adjustTrustFear(NpcState state, float dTrust, float dFear) {
        state.trust = clamp01(state.trust + dTrust);
        state.fear  = clamp01(state.fear  + dFear);
    }

    private float clamp01(float v) {
        if (v < 0f) return 0f;
        return Math.min(v, 1f);
    }

    private String buildUserMessageWithHistory(String npcId, String question) {
        String history = DialogueHistory.loadRecentForLlm(
            npcId,
            MAX_HISTORY_PAIRS,
            MAX_HISTORY_CHARS
        );

        StringBuilder sb = new StringBuilder();

        if (!history.isEmpty()) {
            sb.append("H:\n");
            sb.append(history).append("\n\n");
        }

        sb.append("Q: ").append(question);

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
