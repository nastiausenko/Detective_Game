package com.gdx.game.app.navigation;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.Json;
import com.gdx.game.model.BuildingData;
import com.gdx.game.model.CharacterData;
import com.gdx.game.app.model.GameData;
import com.gdx.game.model.InvestigationState;
import com.gdx.game.app.model.GameContext;
import com.gdx.game.shared.ui.effects.FadeTransition;
import com.gdx.game.screens.interior.CharacterInteriorScreen;
import com.gdx.game.screens.map.MapScreen;
import com.gdx.game.screens.menu.MenuScreen;

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

    public void enterInterior(
        String backgroundPath,
        String npcId,
        String fullBodyPath,
        String buildingId,
        GameFlow flow
    ) {
        if (transition.isTransitioning()) return;

        if (context.audioManager != null) {
            context.audioManager.stopAmbience();
            context.audioManager.playLocationTransition(buildingId);
        }

        transition.startFadeOut(DEFAULT_FADE_SECONDS, () -> {
            game.setScreen(new CharacterInteriorScreen(
                context,
                flow,
                backgroundPath,
                npcId,
                fullBodyPath,
                buildingId
            ));
            transition.startFadeIn(DEFAULT_FADE_SECONDS);
        });
    }

    public void enterAccusationConfrontation(String npcId, GameFlow flow) {
        if (transition.isTransitioning()) return;

        CharacterData character = findCharacter(npcId);
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

        String backgroundPath = findInteriorBackground(buildingId);
        if (backgroundPath == null || backgroundPath.isEmpty()) {
            backgroundPath = findInteriorBackground(character.buildingId);
        }

        final String finalBuildingId = buildingId;
        final String finalBackgroundPath = backgroundPath;

        if (context.audioManager != null) {
            context.audioManager.stopAmbience();
            context.audioManager.playLocationTransition(finalBuildingId);
        }

        transition.startFadeOut(DEFAULT_FADE_SECONDS, () -> {
            game.setScreen(new CharacterInteriorScreen(
                context,
                flow,
                finalBackgroundPath,
                character.id,
                character.fullBody,
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

    private CharacterData findCharacter(String npcId) {
        if (npcId == null || npcId.isEmpty()) return null;

        Json json = new Json();
        CharacterData[] characters = json.fromJson(CharacterData[].class, Gdx.files.internal("characters.json"));
        if (characters == null) return null;

        for (CharacterData character : characters) {
            if (character != null && npcId.equals(character.id)) {
                return character;
            }
        }
        return null;
    }

    private String findInteriorBackground(String buildingId) {
        if (buildingId == null || buildingId.isEmpty()) return "";

        Json json = new Json();
        BuildingData[] buildings = json.fromJson(BuildingData[].class, Gdx.files.internal("buildings.json"));
        if (buildings == null) return "";

        for (BuildingData building : buildings) {
            if (building != null && buildingId.equals(building.id)) {
                return building.interiorBackground;
            }
        }
        return "";
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
