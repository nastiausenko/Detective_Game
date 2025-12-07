package com.gdx.game.npc;

import com.badlogic.gdx.utils.ObjectMap;
import com.gdx.game.data.DossierData;
import com.gdx.game.data.DossierDatabase;

import java.io.IOException;

public class NpcDialogueService {

    private final LlmClient llmClient;
    private final DossierDatabase dossierDb;
    private final ObjectMap<String, NpcState> npcStates = new ObjectMap<>();

    public NpcDialogueService(LlmClient llmClient, DossierDatabase dossierDb) {
        this.llmClient = llmClient;
        this.dossierDb = dossierDb;
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
            npcStates.put(npcId, state);
        }
        return state;
    }

    private String buildLieRiskBehavior(Integer lieRisk) {
        if (lieRisk == null) {
            return "You have an average tendency to lie. You may lie, but you usually try to tell the truth if it is not too dangerous for you.";
        }

        int risk = Math.max(1, Math.min(5, lieRisk));

        String[] labels = {
            "very low",   // 1
            "low",        // 2
            "medium",     // 3
            "high",       // 4
            "very high"   // 5
        };

        String[] behaviors = {
            "You almost never lie. You may leave something unsaid, but outright lies are rare for you.",
            "You mostly tell the truth, but you can lie if it is really necessary to protect yourself.",
            "You often balance between truth and lies: you can mix them, give half the truth, or evade the question.",
            "You often lie, especially when questions touch on topics that are dangerous or painful for you.",
            "You almost always try to distort reality: you lie, distract attention, invent details to confuse the detective."
        };

        String label = labels[risk - 1];
        String behavior = behaviors[risk - 1];

        return "Your propensity to lie (lie_risk) = "
            + risk + " from 5, this is " + label + " tendency to lie. "
            + behavior;
    }

    private String buildSystemPrompt(String npcId) {
        DossierData dossier = dossierDb != null ? dossierDb.characters.get(npcId) : null;
        NpcState state = getOrCreateState(npcId);

        StringBuilder sb = new StringBuilder();

        if (dossier != null) {
            sb.append("You are an NPC in a detective video game.\n")
                .append("Your canonical identity is FIXED and MUST NEVER CHANGE:\n")
                .append("- Name: ").append(dossier.name).append("\n")
                .append("- Age: ").append(dossier.age).append("\n")
                .append("- Job/role: ").append(dossier.role).append("\n\n")

                .append("You must NEVER claim to have a different name or a different job, ")
                .append("even if the player directly asks you to imagine, roleplay or pretend. ")
                .append("If the detective asks \"Як тебе звати?\" or similar, you MUST answer that your name is ")
                .append("\"").append(dossier.name).append("\".\n")
                .append("If the detective asks \"Де ти працюєш?\" or similar, you MUST answer that you are ")
                .append(dossier.role).append(".\n\n");

            if (dossier.personality != null) {
                sb.append("Personality: ").append(dossier.personality).append(".\n");
            }

            sb.append(buildLieRiskBehavior(dossier.lieRisk)).append("\n");
        } else {
            sb.append("You are ").append(npcId)
                .append(", a character in a detective game. Your identity should stay consistent.\n");
        }

        sb.append("\nYou live in the town of Rosenfeld. ")
            .append("The player is a detective investigating the death of Dr. Adrian Walter.\n");

        sb.append("Your current internal state: ")
            .append("trust in the detective = ").append(String.format("%.2f", state.trust))
            .append(" (0=none, 1=full), ")
            .append("fear of the detective = ").append(String.format("%.2f", state.fear))
            .append(" (0=calm, 1=terrified).\n")
            .append("If trust is high (>0.7) and fear is low (<0.4), you tend to be more open. ")
            .append("If fear is high (>0.7), you tend to dodge questions or partially lie.\n");

        if (dossier != null && dossier.publicFacts != null && !dossier.publicFacts.isEmpty()) {
            sb.append("\nPublic facts about you or the case (you may mention them freely when relevant):\n");
            for (String f : dossier.publicFacts) {
                sb.append("- ").append(f).append("\n");
            }
        }

        if (dossier != null && dossier.hiddenFacts != null && !dossier.hiddenFacts.isEmpty()) {
            sb.append("\nSecret facts that you know. ")
                .append("You should NOT directly reveal them unless the detective clearly has strong evidence ")
                .append("or you feel morally pushed to confess. You may hint, dodge or distort these facts, ")
                .append("especially if your lie_risk is high or you are afraid:\n");
            for (String f : dossier.hiddenFacts) {
                sb.append("- ").append(f).append("\n");
            }
        }

        sb.append("\nAnswer in character, in Ukrainian, in 1–3 sentences. ")
            .append("Use first person (\"я\", \"мене звати\", \"я працюю\"), ")
            .append("do NOT switch to another name or job, keep your identity consistent.\n");

        return sb.toString();
    }

    public String askNpcSync(String npcId, String question) throws IOException {
        NpcState state = getOrCreateState(npcId);
        state.questionsAsked += 1;

        String systemPrompt = buildSystemPrompt(npcId);
        String answer = "Knew Walter conducted unofficial experiments on children.";

        // TODO: updateStateAfterExchange(state, question, answer);

        return answer;
    }
}
