package com.gdx.game.game;

import com.badlogic.gdx.scenes.scene2d.Stage;
import com.gdx.game.infrastructure.GameContext;
import com.gdx.game.ui.component.UIOverlayManager;
import com.gdx.game.ui.component.GameTimer;

public class GameFlowController implements GameFlow {
    private final ScreenNavigator navigator;
    private final UIOverlayManager overlay;

    public GameFlowController(GameContext game, ScreenNavigator navigator) {
        this.navigator = navigator;
        this.overlay = new UIOverlayManager(game, this);
    }

    @Override
    public Stage overlayStage() {
        return overlay.getStage();
    }

    @Override
    public GameTimer timer() {
        return overlay.timer();
    }

    @Override
    public void setOverlayVisible(boolean visible) {
        overlay.setVisible(visible);
    }

    @Override
    public void setInInterior(boolean value) {
        overlay.setInInterior(value);
    }

    @Override
    public void setCurrentNpcId(String npcId) {
        overlay.setCurrentNpcId(npcId);
    }

    @Override
    public void setCurrentInteriorBuildingId(String buildingId) {
        overlay.setCurrentInteriorBuildingId(buildingId);
    }

    @Override
    public void renderOverlay(float delta) {
        overlay.render(delta);
    }

    @Override
    public void resizeOverlay(int width, int height) {
        overlay.resize(width, height);
    }

    @Override
    public void hideAllPopups() {
        overlay.hideAllPopups();
    }

    @Override
    public void resetForNewGame() {
        overlay.resetForNewGame();
    }

    @Override
    public void onNewFactsDiscovered(int count) {
        overlay.onNewFactsDiscovered(count);
    }

    @Override
    public void showPrologue() {
        overlay.showPrologue();
    }

    @Override
    public void showAccusation() {
        overlay.showAccusation();
    }

    @Override
    public void showEpilogue() {
        overlay.showEpilogue();
    }

    @Override
    public void showTheEnd() {
        overlay.showTheEnd();
    }

    @Override
    public void resumeOrStartGame() {
        navigator.resumeOrStartGame(this);
    }

    @Override
    public void startNewGame() {
        navigator.startNewGame(this);
    }

    @Override
    public void showMenu() {
        navigator.showMenu(this);
    }

    @Override
    public void returnToMapFromInterior(String buildingId) {
        navigator.returnToMapFromInterior(buildingId, this);
    }

    @Override
    public void enterInterior(String backgroundPath, String npcId, String fullBodyPath, String buildingId) {
        navigator.enterInterior(backgroundPath, npcId, fullBodyPath, buildingId, this);
    }

    @Override
    public void enterAccusationConfrontation(String npcId) {
        navigator.enterAccusationConfrontation(npcId, this);
    }

    @Override
    public void returnToMapThenShowEpilogue() {
        navigator.returnToMapThenShowEpilogue(this);
    }

    public void dispose() {
        overlay.dispose();
    }
}
