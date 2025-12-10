package com.gdx.game;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.Json;
import com.gdx.game.data.DossierDatabase;
import com.gdx.game.data.InvestigationState;
import com.gdx.game.data.LoreDatabase;
import com.gdx.game.npc.LlmClient;
import com.gdx.game.npc.NpcDialogueService;
import com.gdx.game.screens.MenuScreen;
import com.gdx.game.ui.GdxResourceProvider;
import com.gdx.game.ui.UIButtonFactory;
import com.gdx.game.utils.EpilogueService;
import com.gdx.game.utils.FadeTransition;
import com.gdx.game.utils.UIOverlayManager;
import io.github.cdimascio.dotenv.Dotenv;

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

    @Override
    public void create() {
        batch = new SpriteBatch();
        transition = new FadeTransition();
        buttonFactory = new UIButtonFactory(new GdxResourceProvider());

        Json json = new Json();
        dossierDb = json.fromJson(DossierDatabase.class, Gdx.files.internal("dossier_ukr.json"));
        loreDb = json.fromJson(LoreDatabase.class, Gdx.files.internal("lore.json"));

        Dotenv dotenv = Dotenv.configure()
            .ignoreIfMalformed()
            .ignoreIfMissing()
            .load();

        String apiKey = dotenv.get("OPENAI_API_KEY");
        if (apiKey == null || apiKey.isEmpty()) {
            apiKey = System.getenv("OPENAI_API_KEY");
        }

        if (apiKey == null || apiKey.isEmpty()) {
            Gdx.app.error("LlmClient", "OPENAI_API_KEY не знайдено ні в .env, ні в системних змінних");
            throw new IllegalStateException("OPENAI_API_KEY is missing");
        }

        LlmClient llmClient = new LlmClient(apiKey);

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
