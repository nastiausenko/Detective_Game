package com.gdx.game.npc;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.IntArray;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.TimeUtils;
import com.gdx.game.data.DialogueHistory;
import com.gdx.game.data.DossierData;
import com.gdx.game.data.DossierDatabase;

import java.io.IOException;
import java.util.Locale;

//TODO add lore
public class NpcDialogueService {
    private static final int MAX_HISTORY_PAIRS = 6;
    private static final int MAX_HISTORY_CHARS = 1200;
    private static final float SESSION_BREAK_SECONDS = 180f;
    private static final String NPC_PREFS_NAME = "npc_state";

    private final LlmClient llmClient;
    private final DossierDatabase dossierDb;
    private final ObjectMap<String, NpcState> npcStates = new ObjectMap<>();

    public NpcDialogueService(LlmClient llmClient, DossierDatabase dossierDb) {
        this.llmClient = llmClient;
        this.dossierDb = dossierDb;
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

    private String buildSystemPrompt(String npcId) {
        DossierData dossier = dossierDb != null ? dossierDb.characters.get(npcId) : null;
        NpcState state = getOrCreateState(npcId);

        StringBuilder sb = new StringBuilder();

        sb.append("You are one NPC in a detective game.\n");

        if (dossier != null) {
            sb.append("IDENTITY (must always stay the same):\n")
                    .append("name: ").append(dossier.name).append("\n")
                    .append("age: ").append(dossier.age).append("\n")
                    .append("role: ").append(dossier.role).append("\n");

            if (dossier.personality != null) {
                sb.append("personality: ").append(dossier.personality).append("\n");
            }

            sb.append(buildLieRiskBehavior(dossier.lieRisk)).append("\n");
        } else {
            sb.append("IDENTITY: ").append(npcId).append(" (keep it consistent).\n");
        }

        sb.append("WORLD: town Rosenfeld, you are questioned by a detective about the death of Dr. Adrian Walter.\n");

        sb.append("STATE: trust=").append(String.format(Locale.ROOT, "%.2f", state.trust))
                .append(", fear=").append(String.format(Locale.ROOT, "%.2f", state.fear))
                .append(" (0..1). Be more open when trust>0.7 & fear<0.4; avoid or distort answers when fear>0.7.\n");

        if (dossier != null && dossier.publicFacts != null && !dossier.publicFacts.isEmpty()) {
            sb.append("PUBLIC_FACTS (you may mention freely when relevant):\n");
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
                sb.append("REVEALED_SECRETS (already clearly admitted, you cannot deny them):\n");
                for (int i = 0; i < dossier.hiddenFacts.size(); i++) {
                    if (state.hiddenRevealed[i]) {
                        sb.append("- ").append(dossier.hiddenFacts.get(i)).append("\n");
                    }
                }
            }

            sb.append("STILL_SECRET_FACTS (do NOT state them plainly unless the detective clearly asks ")
                    .append("or you feel morally forced to confess; you may hint or dodge):\n");
            for (int i = 0; i < dossier.hiddenFacts.size(); i++) {
                if (!state.hiddenRevealed[i]) {
                    sb.append("- ").append(dossier.hiddenFacts.get(i)).append("\n");
                }
            }
        }

        sb.append("Answer in Ukrainian, 1–3 short sentences, first person (\"я\", \"мене звати\", \"я працюю\"). ")
                .append("Stay in character and keep your identity consistent.\n");

        return sb.toString();
    }

    //TODO fix classifier to reveal npc facts and facts about doctor
    public boolean isQuestionLogicalForHiddenFact(String npcId,
                                                  String question,
                                                  String hiddenFact) throws IOException {

        String system = "You are a binary classifier in a detective game. "
                + "Decide if a detective's QUESTION essentially refers to a given HIDDEN FACT. "
                + "Answer ONLY with YES or NO.";

        String user = "HIDDEN FACT: \"" + hiddenFact + "\"\n"
                + "QUESTION: \"" + question + "\"\n"
                + "Answer only YES or NO.";

        String result = llmClient.ask(system, user);
        if (result == null) return false;

        result = result.trim().toUpperCase(Locale.ROOT);
        return result.startsWith("YES");
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
        NpcState state = getOrCreateState(npcId);
        state.questionsAsked += 1;

        String systemPrompt = buildSystemPrompt(npcId);
        String userMessage  = buildUserMessageWithHistory(npcId, question);

        String answer = llmClient.ask(systemPrompt, userMessage);

        updateStateAfterExchange(state, question);

        saveStateToPrefs(npcId, state);

        return answer;
    }

    private void updateStateAfterExchange(NpcState state,
                                          String question) {
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
}
