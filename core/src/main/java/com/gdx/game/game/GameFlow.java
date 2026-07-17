package com.gdx.game.game;

import com.badlogic.gdx.scenes.scene2d.Stage;
import com.gdx.game.ui.component.GameTimer;

public interface GameFlow {
    Stage overlayStage();
    GameTimer timer();

    void setOverlayVisible(boolean visible);
    void setInInterior(boolean value);
    void setCurrentNpcId(String npcId);
    void setCurrentInteriorBuildingId(String buildingId);
    void renderOverlay(float delta);
    void resizeOverlay(int width, int height);
    void hideAllPopups();
    void resetForNewGame();
    void onNewFactsDiscovered(int count);

    void showPrologue();
    void showAccusation();
    void showEpilogue();
    void showTheEnd();

    void resumeOrStartGame();
    void startNewGame();
    void showMenu();
    void returnToMapFromInterior(String buildingId);
    void enterInterior(String npcId, String buildingId);
    void enterAccusationConfrontation(String npcId);
    void returnToMapThenShowEpilogue();
}
