package com.gdx.game;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.Json;
import com.gdx.game.data.DossierDatabase;
import com.gdx.game.npc.LlmClient;
import com.gdx.game.npc.NpcDialogueService;
import com.gdx.game.npc.NpcStateManager;
import com.gdx.game.screens.MenuScreen;
import com.gdx.game.ui.GdxResourceProvider;
import com.gdx.game.ui.UIButtonFactory;
import com.gdx.game.utils.FadeTransition;
import com.gdx.game.utils.UIOverlayManager;

public class DetectiveGame extends Game {
    public SpriteBatch batch;
    public UIOverlayManager overlay;

    private UIButtonFactory buttonFactory;
    private FadeTransition transition;

    private DossierDatabase dossierDb;
    private NpcDialogueService npcDialogueService;
    private NpcStateManager npcStateManager;

    @Override
    public void create() {
        batch = new SpriteBatch();
        transition = new FadeTransition();
        buttonFactory = new UIButtonFactory(new GdxResourceProvider());

        Json json = new Json();
        dossierDb = json.fromJson(DossierDatabase.class, Gdx.files.internal("dossier.json"));

        String apiKey = "API_KEY";
        LlmClient llmClient = new LlmClient(apiKey);

        npcDialogueService = new NpcDialogueService(llmClient, dossierDb);
        npcStateManager = new NpcStateManager();

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

    public NpcStateManager getNpcStateManager() {
        return npcStateManager;
    }
}
