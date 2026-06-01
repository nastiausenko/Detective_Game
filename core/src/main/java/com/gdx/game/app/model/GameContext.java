package com.gdx.game.app.model;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.gdx.game.shared.audio.AudioManager;
import com.gdx.game.shared.ui.UIButtonFactory;
import com.gdx.game.features.investigation.model.EpilogueService;
import com.gdx.game.features.investigation.model.FactRevealService;
import com.gdx.game.features.investigation.model.NpcDialogueService;
import com.gdx.game.model.DossierDatabase;
import com.gdx.game.model.InvestigationState;
import com.gdx.game.model.LoreDatabase;
import com.gdx.game.features.investigation.model.CrimeSceneService;
import com.gdx.game.features.worldmap.model.NpcLocationService;

public class GameContext {
    public final SpriteBatch batch;
    public final UIButtonFactory buttonFactory;
    public final AudioManager audioManager;
    public final DossierDatabase dossierDb;
    public final LoreDatabase loreDb;
    public final NpcDialogueService npcDialogueService;
    public final InvestigationState investigationState;
    public final EpilogueService epilogueService;
    public final NpcLocationService npcLocationService;
    public final CrimeSceneService crimeSceneService;
    public final FactRevealService factRevealService;

    public GameContext(
        SpriteBatch batch,
        UIButtonFactory buttonFactory,
        AudioManager audioManager,
        DossierDatabase dossierDb,
        LoreDatabase loreDb,
        NpcDialogueService npcDialogueService,
        InvestigationState investigationState,
        EpilogueService epilogueService,
        NpcLocationService npcLocationService,
        CrimeSceneService crimeSceneService,
        FactRevealService factRevealService
    ) {
        this.batch = batch;
        this.buttonFactory = buttonFactory;
        this.audioManager = audioManager;
        this.dossierDb = dossierDb;
        this.loreDb = loreDb;
        this.npcDialogueService = npcDialogueService;
        this.investigationState = investigationState;
        this.epilogueService = epilogueService;
        this.npcLocationService = npcLocationService;
        this.crimeSceneService = crimeSceneService;
        this.factRevealService = factRevealService;
    }
}
