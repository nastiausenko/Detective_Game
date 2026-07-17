package com.gdx.game.game;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.gdx.game.domain.character.CharacterData;
import com.gdx.game.service.save.GameData;
import com.gdx.game.domain.investigation.InvestigationState;
import com.gdx.game.infrastructure.GameContext;
import com.gdx.game.ui.effect.FadeTransition;
import com.gdx.game.screen.CharacterInteriorScreen;
import com.gdx.game.screen.MapScreen;
import com.gdx.game.screen.MenuScreen;

public class ScreenNavigator {
    private static final float DEFAULT_FADE_SECONDS = 0.7f;

    private final Game game;
    private final FadeTransition transition;
    private final GameContext context;

    public ScreenNavigator(Game game, FadeTransition transition, GameContext context) {
        this.game = game;
        this.transition = transition;
        this.context = context;
    }

    public void resumeOrStartGame(GameFlow flow) {
        if (transition.isTransitioning()) return;

        transition.startFadeOut(DEFAULT_FADE_SECONDS, () -> {
            if (shouldStartFreshGame(flow)) {
                resetNewGameState(flow);
            }
            showMapAfterFadeOut(flow);
        });
    }

    public void startNewGame(GameFlow flow) {
        if (transition.isTransitioning()) return;

        transition.startFadeOut(DEFAULT_FADE_SECONDS, () -> {
            resetNewGameState(flow);
            showMapAfterFadeOut(flow);
        });
    }

    public void showMenu(GameFlow flow) {
        if (transition.isTransitioning()) return;

        transition.startFadeOut(DEFAULT_FADE_SECONDS, () -> {
            game.setScreen(new MenuScreen(context, flow));
            transition.startFadeIn(DEFAULT_FADE_SECONDS);
        });
    }

    public void returnToMapFromInterior(String buildingId, GameFlow flow) {
        if (transition.isTransitioning()) return;

        if (context.audioManager != null) {
            context.audioManager.playLocationTransition(buildingId);
        }

        transition.startFadeOut(DEFAULT_FADE_SECONDS, () -> showMapAfterFadeOut(flow));
    }

    public void enterInterior(String npcId, String buildingId, GameFlow flow) {
        if (transition.isTransitioning()) return;

        if (context.audioManager != null) {
            context.audioManager.stopAmbience();
            context.audioManager.playLocationTransition(buildingId);
        }

        transition.startFadeOut(DEFAULT_FADE_SECONDS, () -> {
            game.setScreen(new CharacterInteriorScreen(
                context,
                flow,
                npcId,
                buildingId
            ));
            transition.startFadeIn(DEFAULT_FADE_SECONDS);
        });
    }

    public void enterAccusationConfrontation(String npcId, GameFlow flow) {
        if (transition.isTransitioning()) return;

        CharacterData character = context.worldLookupService.getCharacter(npcId);
        if (character == null) {
            flow.showEpilogue();
            return;
        }

        String buildingId = context.npcLocationService != null
            ? context.npcLocationService.getCurrentBuildingId(npcId)
            : null;
        if (buildingId == null || buildingId.isEmpty()) {
            buildingId = character.buildingId;
        }

        if (!context.worldLookupService.hasInteriorBackground(buildingId)) {
            buildingId = character.buildingId;
        }

        final String finalBuildingId = buildingId;

        if (context.audioManager != null) {
            context.audioManager.stopAmbience();
            context.audioManager.playLocationTransition(finalBuildingId);
        }

        transition.startFadeOut(DEFAULT_FADE_SECONDS, () -> {
            game.setScreen(new CharacterInteriorScreen(
                context,
                flow,
                character.id,
                finalBuildingId,
                true
            ));
            transition.startFadeIn(DEFAULT_FADE_SECONDS);
        });
    }

    public void returnToMapThenShowEpilogue(GameFlow flow) {
        if (transition.isTransitioning()) {
            Gdx.app.postRunnable(() -> returnToMapThenShowEpilogue(flow));
            return;
        }

        transition.startFadeOut(DEFAULT_FADE_SECONDS, () -> {
            showMapAfterFadeOut(flow);
            Gdx.app.postRunnable(flow::showEpilogue);
        });
    }

    private void showMapAfterFadeOut(GameFlow flow) {
        game.setScreen(new MapScreen(context, flow));
        transition.startFadeIn(DEFAULT_FADE_SECONDS);
    }

    private void resetNewGameState(GameFlow flow) {
        GameData.clearAll();
        context.npcDialogueService.resetAllNpcState();
        context.npcLocationService.reset();
        context.crimeSceneService.reset();
        context.epilogueService.clearCache();
        flow.resetForNewGame();

        InvestigationState inv = context.investigationState;
        if (inv != null) {
            inv.accusationDone = false;
            inv.accusedNpcId = null;
        }
    }

    private boolean shouldStartFreshGame(GameFlow flow) {
        InvestigationState inv = context.investigationState;
        return flow.timer().isTimeOver()
            || (inv != null && inv.accusationDone);
    }
}
