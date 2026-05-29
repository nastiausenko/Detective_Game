package com.gdx.game.ui.navigation;

import com.gdx.game.DetectiveGame;
import com.gdx.game.GameData;
import com.gdx.game.domain.investigation.InvestigationState;
import com.gdx.game.infrastructure.GameContext;
import com.gdx.game.ui.overlay.FadeTransition;
import com.gdx.game.ui.screens.CharacterInteriorScreen;
import com.gdx.game.ui.screens.MapScreen;
import com.gdx.game.ui.screens.MenuScreen;

public class ScreenNavigator {
    private static final float DEFAULT_FADE_SECONDS = 0.7f;

    private final DetectiveGame game;
    private final FadeTransition transition;
    private GameContext context;

    public ScreenNavigator(DetectiveGame game, FadeTransition transition) {
        this.game = game;
        this.transition = transition;
    }

    public void setContext(GameContext context) {
        this.context = context;
    }

    public void resumeOrStartGame() {
        if (transition.isTransitioning()) return;

        transition.startFadeOut(DEFAULT_FADE_SECONDS, () -> {
            if (game.overlay.getTimer().isTimeOver()) {
                resetNewGameState();
            }
            showMapAfterFadeOut();
        });
    }

    public void startNewGame() {
        if (transition.isTransitioning()) return;

        transition.startFadeOut(DEFAULT_FADE_SECONDS, () -> {
            resetNewGameState();
            showMapAfterFadeOut();
        });
    }

    public void showMenu() {
        if (transition.isTransitioning()) return;

        transition.startFadeOut(DEFAULT_FADE_SECONDS, () -> {
            game.setScreen(new MenuScreen(context));
            transition.startFadeIn(DEFAULT_FADE_SECONDS);
        });
    }

    public void returnToMapFromInterior(String buildingId) {
        if (transition.isTransitioning()) return;

        if (game.getAudioManager() != null) {
            game.getAudioManager().playLocationTransition(buildingId);
        }

        transition.startFadeOut(DEFAULT_FADE_SECONDS, this::showMapAfterFadeOut);
    }

    public void enterInterior(
        String backgroundPath,
        String npcId,
        String fullBodyPath,
        String buildingId
    ) {
        if (transition.isTransitioning()) return;

        if (game.getAudioManager() != null) {
            game.getAudioManager().stopAmbience();
            game.getAudioManager().playLocationTransition(buildingId);
        }

        transition.startFadeOut(DEFAULT_FADE_SECONDS, () -> {
            game.setScreen(new CharacterInteriorScreen(
                context,
                backgroundPath,
                npcId,
                fullBodyPath,
                buildingId
            ));
            transition.startFadeIn(DEFAULT_FADE_SECONDS);
        });
    }

    private void showMapAfterFadeOut() {
        game.setScreen(new MapScreen(context));
        transition.startFadeIn(DEFAULT_FADE_SECONDS);
    }

    private void resetNewGameState() {
        GameData.clearAll();
        game.getNpcDialogueService().resetAllNpcState();
        game.getNpcLocationService().reset();
        game.getCrimeSceneService().reset();
        game.overlay.resetForNewGame();

        InvestigationState inv = game.getInvestigationState();
        if (inv != null) {
            inv.accusationDone = false;
            inv.accusedNpcId = null;
        }
    }
}
