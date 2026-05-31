package com.gdx.game.app.navigation;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.Json;
import com.gdx.game.app.DetectiveGame;
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

    public void enterAccusationConfrontation(String npcId) {
        if (transition.isTransitioning()) return;

        CharacterData character = findCharacter(npcId);
        if (character == null) {
            game.overlay.showEpilogue();
            return;
        }

        String buildingId = game.getNpcLocationService() != null
            ? game.getNpcLocationService().getCurrentBuildingId(npcId)
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

        if (game.getAudioManager() != null) {
            game.getAudioManager().stopAmbience();
            game.getAudioManager().playLocationTransition(finalBuildingId);
        }

        transition.startFadeOut(DEFAULT_FADE_SECONDS, () -> {
            game.setScreen(new CharacterInteriorScreen(
                context,
                finalBackgroundPath,
                character.id,
                character.fullBody,
                finalBuildingId,
                true
            ));
            transition.startFadeIn(DEFAULT_FADE_SECONDS);
        });
    }

    public void returnToMapThenShowEpilogue() {
        if (transition.isTransitioning()) {
            Gdx.app.postRunnable(this::returnToMapThenShowEpilogue);
            return;
        }

        transition.startFadeOut(DEFAULT_FADE_SECONDS, () -> {
            showMapAfterFadeOut();
            Gdx.app.postRunnable(() -> game.overlay.showEpilogue());
        });
    }

    private void showMapAfterFadeOut() {
        game.setScreen(new MapScreen(context));
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
