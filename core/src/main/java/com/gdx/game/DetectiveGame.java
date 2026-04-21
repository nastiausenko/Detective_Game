package com.gdx.game;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.Json;
import com.gdx.game.domain.character.DossierDatabase;
import com.gdx.game.domain.investigation.InvestigationState;
import com.gdx.game.domain.world.LoreDatabase;
import com.gdx.game.ai.LlmClient;
import com.gdx.game.ai.NpcDialogueService;
import com.gdx.game.ui.overlay.FadeTransition;
import com.gdx.game.ui.screens.MenuScreen;
import com.gdx.game.infrastructure.UIButtonFactory;
import com.gdx.game.ai.EpilogueService;
import com.gdx.game.ui.overlay.UIOverlayManager;

public class DetectiveGame extends Game {
    public SpriteBatch batch;
    public UIOverlayManager overlay;

    private UIButtonFactory buttonFactory;
    private FadeTransition transition;

    private DossierDatabase dossierDb;
    private LoreDatabase loreDb;
    private NpcDialogueService npcDialogueService;
    private InvestigationState investigationState;
    private EpilogueService epilogueService;

    private final String openAiKey;
    private final String groqKey;

    public DetectiveGame(String openAiKey, String groqKey) {
        this.openAiKey = openAiKey;
        this.groqKey = groqKey;
    }

    @Override
    public void create() {
        batch = new SpriteBatch();
        transition = new FadeTransition();
        buttonFactory = new UIButtonFactory();

        Json json = new Json();
        dossierDb = json.fromJson(DossierDatabase.class, Gdx.files.internal("dossier_ukr.json"));
        loreDb = json.fromJson(LoreDatabase.class, Gdx.files.internal("lore.json"));

        if (openAiKey == null || openAiKey.isEmpty()) {
            Gdx.app.error("LlmClient", "OPENAI_API_KEY is missing");
        }

        if (groqKey == null || groqKey.isEmpty()) {
            Gdx.app.error("LlmClient", "GROQ_API_KEY is missing");
        }

        LlmClient llmClient = new LlmClient(openAiKey, groqKey);

        npcDialogueService = new NpcDialogueService(llmClient, dossierDb);
        investigationState = new InvestigationState();
        epilogueService = new EpilogueService(llmClient, loreDb, dossierDb, npcDialogueService);

        overlay = new UIOverlayManager(this);

        setScreen(new MenuScreen(this, transition));
    }

    @Override
    public void render() {
        super.render();
        transition.update(Gdx.graphics.getDeltaTime());
        transition.render();
    }

    @Override
    public void dispose() {
        if (overlay != null) overlay.dispose();
        if (batch != null) batch.dispose();
    }

    public UIButtonFactory getButtonFactory() {
        return buttonFactory;
    }

    public FadeTransition getTransition() {
        return transition;
    }

    public DossierDatabase getDossierDb() {
        return dossierDb;
    }

    public NpcDialogueService getNpcDialogueService() {
        return npcDialogueService;
    }

    public InvestigationState getInvestigationState() {
        return investigationState;
    }

    public EpilogueService getEpilogueService() {
        return epilogueService;
    }
}
