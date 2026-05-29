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

        String systemPrompt = buildSystemPrompt();
        String userMessage  = buildUserMessage(accusedId, correct);

        return llmClient.ask(systemPrompt, userMessage);
    }

    private String buildSystemPrompt() {
        StringBuilder sb = new StringBuilder();

        sb.append("You are the literary narrator of a psychological thriller.\n")
            .append("Write the epilogue in Ukrainian, 3–5 paragraphs, each 2–4 short sentences.\n")
            .append("Style: quiet European town, moral tension, no cheap horror or gore.\n")
            .append("Do NOT mention game mechanics, player, NPCs, models, or ChatGPT — this is just a novel ending.\n\n")
            .append("About the killer:\n")
            .append("- You may know the true killer as internal information.\n")
            .append("- If the accusation is WRONG, do NOT name the real killer, ")
            .append("do NOT reveal their role, and do NOT give direct hints like \"it was actually someone else\".\n")
            .append("- In that case, only convey a feeling that the real killer is still free, ")
            .append("and the town lives with a half-truth.\n")
            .append("- If the accusation is CORRECT, you may artistically describe the killer's inner logic, ")
            .append("but still without talking about the game.\n\n")
            .append("Very important:\n")
            .append("- Use ONLY information that is explicitly present in PUBLIC FACTS, DISCOVERED SECRETS, ")
            .append("and DIALOGUE EXCERPTS below.\n")
            .append("- Do NOT mention secret experiments, hidden crimes or backstory details ")
            .append("unless they appear in those sections.\n");

        return sb.toString();
    }

    private String buildUserMessage(String accusedId,
                                    boolean correct) {

        StringBuilder sb = new StringBuilder();

        if (loreDb != null && loreDb.setting != null) {
            sb.append("CITY:\n");
            sb.append("Name: ").append(loreDb.setting.townName).append("\n");
            sb.append("Tone: ").append(loreDb.setting.tone).append("\n\n");
        }

        sb.append("DETECTIVE FINAL DECISION:\n");
        sb.append("Accused npc id: ").append(accusedId != null ? accusedId : "none").append("\n");
        sb.append("Accusation_correct: ").append(correct ? "YES" : "NO").append("\n\n");

        sb.append("PUBLIC FACTS (these are safe, known things in the town):\n");
        sb.append(buildPublicFactsSummary());
        sb.append("\n");

        sb.append("DISCOVERED SECRETS (these hidden facts were clearly uncovered by the detective):\n");
        sb.append(buildDiscoveredFactsSummary());
        sb.append("\n");

        if (accusedId != null) {
            String accusedHistory = DialogueHistory.loadRecentForLlm(accusedId, 6, 900);
            if (!accusedHistory.isEmpty()) {
                sb.append("KEY DIALOGUES WITH THE ACCUSED:\n");
                sb.append(accusedHistory).append("\n\n");
            }
        }

        sb.append("TASK:\n");
        sb.append("Using ONLY the information in CITY, PUBLIC FACTS, DISCOVERED SECRETS and KEY DIALOGUES, ")
            .append("write the epilogue of Rosenfeld story.\n")
            .append("Show how the town changes, what happens to the accused, ")
            .append("and what trace Walter and his death leave behind.\n")
            .append("If the accusation is YES (correct) — this is a story about the price of truth and ")
            .append("confrontation with a 'new kind of human'.\n")
            .append("If the accusation is NO (wrong) — this is a story about flawed justice, ")
            .append("a town living with a half-truth, and the sense that the real killer is still out there.\n")
            .append("Do NOT invent new secret experiments or crimes that are not explicitly listed above.\n");

        return sb.toString();
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
