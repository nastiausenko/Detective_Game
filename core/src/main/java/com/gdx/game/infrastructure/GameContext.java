package com.gdx.game.infrastructure;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.gdx.game.infrastructure.AudioManager;
import com.gdx.game.ui.component.UIButtonFactory;
import com.gdx.game.service.investigation.EpilogueService;
import com.gdx.game.service.dialogue.FactRevealService;
import com.gdx.game.service.dialogue.NpcDialogueService;
import com.gdx.game.domain.character.DossierDatabase;
import com.gdx.game.domain.investigation.InvestigationState;
import com.gdx.game.domain.world.LoreDatabase;
import com.gdx.game.service.investigation.CrimeSceneService;
import com.gdx.game.service.world.NpcLocationService;

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
