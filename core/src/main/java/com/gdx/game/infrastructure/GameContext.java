package com.gdx.game.infrastructure;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.gdx.game.ai.EpilogueService;
import com.gdx.game.ai.FactRevealService;
import com.gdx.game.ai.NpcDialogueService;
import com.gdx.game.domain.character.DossierDatabase;
import com.gdx.game.domain.investigation.InvestigationState;
import com.gdx.game.domain.world.CrimeSceneService;
import com.gdx.game.ui.component.chars.NpcLocationService;
import com.gdx.game.ui.navigation.ScreenNavigator;
import com.gdx.game.ui.overlay.UIOverlayManager;

public class GameContext {
    public final SpriteBatch batch;
    public final UIOverlayManager overlay;

    private final UIButtonFactory buttonFactory;
    private final AudioManager audioManager;
    private final DossierDatabase dossierDb;
    private final NpcDialogueService npcDialogueService;
    private final InvestigationState investigationState;
    private final EpilogueService epilogueService;
    private final NpcLocationService npcLocationService;
    private final CrimeSceneService crimeSceneService;
    private final FactRevealService factRevealService;
    private final ScreenNavigator navigator;

    public GameContext(
        SpriteBatch batch,
        UIOverlayManager overlay,
        UIButtonFactory buttonFactory,
        AudioManager audioManager,
        DossierDatabase dossierDb,
        NpcDialogueService npcDialogueService,
        InvestigationState investigationState,
        EpilogueService epilogueService,
        NpcLocationService npcLocationService,
        CrimeSceneService crimeSceneService,
        FactRevealService factRevealService,
        ScreenNavigator navigator
    ) {
        this.batch = batch;
        this.overlay = overlay;
        this.buttonFactory = buttonFactory;
        this.audioManager = audioManager;
        this.dossierDb = dossierDb;
        this.npcDialogueService = npcDialogueService;
        this.investigationState = investigationState;
        this.epilogueService = epilogueService;
        this.npcLocationService = npcLocationService;
        this.crimeSceneService = crimeSceneService;
        this.factRevealService = factRevealService;
        this.navigator = navigator;
    }

    public UIButtonFactory getButtonFactory() {
        return buttonFactory;
    }

    public AudioManager getAudioManager() {
        return audioManager;
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

    public NpcLocationService getNpcLocationService() {
        return npcLocationService;
    }

    public CrimeSceneService getCrimeSceneService() {
        return crimeSceneService;
    }

    public FactRevealService getFactRevealService() {
        return factRevealService;
    }

    public ScreenNavigator getNavigator() {
        return navigator;
    }
}
