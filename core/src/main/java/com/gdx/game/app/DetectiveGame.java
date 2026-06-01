package com.gdx.game.app;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.Json;
import com.gdx.game.model.DossierDatabase;
import com.gdx.game.model.InvestigationState;
import com.gdx.game.features.investigation.model.CrimeSceneService;
import com.gdx.game.model.LoreDatabase;
import com.gdx.game.features.investigation.model.FactRevealService;
import com.gdx.game.shared.api.LlmClient;
import com.gdx.game.features.investigation.model.NpcDialogueService;
import com.gdx.game.shared.audio.AudioManager;
import com.gdx.game.app.model.GameContext;
import com.gdx.game.shared.ui.effects.FadeTransition;
import com.gdx.game.app.navigation.GameFlowController;
import com.gdx.game.app.navigation.ScreenNavigator;
import com.gdx.game.screens.menu.MenuScreen;
import com.gdx.game.shared.ui.UIButtonFactory;
import com.gdx.game.features.investigation.model.EpilogueService;
import com.gdx.game.features.worldmap.model.NpcLocationService;

public class DetectiveGame extends Game {
    private SpriteBatch batch;
    private GameFlowController flow;
    private AudioManager audioManager;

    private UIButtonFactory buttonFactory;
    private FadeTransition transition;
    private GameContext gameContext;

    private DossierDatabase dossierDb;
    private LoreDatabase loreDb;
    private LlmClient llmClient;
    private NpcDialogueService npcDialogueService;
    private InvestigationState investigationState;
    private EpilogueService epilogueService;
    private NpcLocationService npcLocationService;
    private CrimeSceneService crimeSceneService;
    private FactRevealService factRevealService;

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
        audioManager = new AudioManager();
        audioManager.load();

        buttonFactory = new UIButtonFactory(audioManager);

        Json json = new Json();
        dossierDb = json.fromJson(DossierDatabase.class, Gdx.files.internal("dossier_ukr.json"));
        loreDb = json.fromJson(LoreDatabase.class, Gdx.files.internal("lore.json"));

        if (openAiKey == null || openAiKey.isEmpty()) {
            Gdx.app.error("LlmClient", "OPENAI_API_KEY is missing");
        }

        if (groqKey == null || groqKey.isEmpty()) {
            Gdx.app.error("LlmClient", "GROQ_API_KEY is missing");
        }

        llmClient = new LlmClient(openAiKey, groqKey);

        npcDialogueService = new NpcDialogueService(llmClient, dossierDb);
        investigationState = new InvestigationState();
        epilogueService = new EpilogueService(llmClient, loreDb, dossierDb, npcDialogueService);
        npcLocationService = new NpcLocationService();
        crimeSceneService = new CrimeSceneService(loreDb, npcDialogueService);
        factRevealService = new FactRevealService(npcDialogueService, dossierDb, crimeSceneService);

        gameContext = new GameContext(
            batch,
            buttonFactory,
            audioManager,
            dossierDb,
            loreDb,
            npcDialogueService,
            investigationState,
            epilogueService,
            npcLocationService,
            crimeSceneService,
            factRevealService
        );

        ScreenNavigator navigator = new ScreenNavigator(this, transition, gameContext);
        flow = new GameFlowController(gameContext, navigator);

        setScreen(new MenuScreen(gameContext, flow));
    }

    @Override
    public void render() {
        super.render();
        transition.update(Gdx.graphics.getDeltaTime());
        transition.render();
    }

    @Override
    public void dispose() {
        if (flow != null) flow.dispose();
        if (npcDialogueService != null) npcDialogueService.dispose();
        if (batch != null) batch.dispose();
        if (audioManager != null) audioManager.dispose();
    }
}
