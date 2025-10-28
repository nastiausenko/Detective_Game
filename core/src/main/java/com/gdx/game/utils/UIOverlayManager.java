package com.gdx.game.utils;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.gdx.game.DetectiveGame;
import com.gdx.game.screens.MapScreen;
import com.gdx.game.ui.popup.*;
import com.gdx.game.ui.timer.GameTimer;

public class UIOverlayManager {
    private final DetectiveGame game;
    public final Stage uiStage;

    private final Image toggleButton;
    private final Image notesButton;
    private final Image dossierButton;
    private final Image settingsButton;
    private final Image homeButton;

    private final Texture arrowDownTexture;
    private final Texture arrowUpTexture;

    private final GameTimer timer;
    private final PopupFactory popupFactory;

    private NotePopup notePopup;
    private DossierPopup dossierPopup;
    private SettingsPopup settingsPopup;

    private boolean menuOpened = false;
    private boolean visible = true;
    private boolean inInterior = false;

    public UIOverlayManager(DetectiveGame game) {
        this.game = game;
        this.uiStage = new Stage(new ScreenViewport(), game.batch);

        arrowDownTexture = new Texture(Assets.TOGGLE_BUTTON_DOWN);
        arrowUpTexture = new Texture(Assets.TOGGLE_BUTTON_UP);

        toggleButton = game.getButtonFactory().createButton(Assets.TOGGLE_BUTTON_DOWN, 64, 64, this::toggleMenu);
        notesButton = game.getButtonFactory().createButton(Assets.NOTE_ICON, 64, 64, this::showNotes);
        dossierButton = game.getButtonFactory().createButton(Assets.DOSSIER_BUTTON, 64, 64, this::showDossier);
        settingsButton = game.getButtonFactory().createButton(Assets.SETTINGS_BUTTON, 64, 64, this::showSettings);
        homeButton = game.getButtonFactory().createButton(Assets.HOME_BUTTON, 64, 64, this::backToMap);

        notesButton.setVisible(false);
        dossierButton.setVisible(false);

        uiStage.addActor(toggleButton);
        uiStage.addActor(notesButton);
        uiStage.addActor(dossierButton);
        uiStage.addActor(settingsButton);

        popupFactory = new PopupFactory(uiStage, game, game.getTransition());
        timer = new GameTimer(uiStage, 60 * 60f);
    }

    private void toggleMenu() {
        menuOpened = !menuOpened;
        notesButton.setVisible(menuOpened);
        dossierButton.setVisible(menuOpened);
        toggleButton.setDrawable(new Image(menuOpened ? arrowUpTexture : arrowDownTexture).getDrawable());
    }

    private void backToMap() {
        if (!inInterior) return;

        game.getTransition().startFadeOut(0.7f, () -> {
            game.setScreen(new MapScreen(game, game.getTransition()));
            game.getTransition().startFadeIn(0.7f);
        });
    }

    private void showNotes() {
        if (notePopup == null) notePopup = popupFactory.createNotePopup();
        notePopup.show();
    }

    private void showDossier() {
        if (dossierPopup == null) dossierPopup = popupFactory.createDossierPopup();
        dossierPopup.show();
    }

    private void showSettings() {
        if (settingsPopup == null) settingsPopup = popupFactory.createSettingsPopup();
        timer.saveTime();
        settingsPopup.show();
    }

    public void render(float delta) {
        if (!visible) return;

        uiStage.act(delta);
        uiStage.draw();

        if (settingsPopup != null && settingsPopup.isVisible()) {
            timer.pause();
        }

        timer.update(delta);
    }

    public void setInInterior(boolean value) {
        this.inInterior = value;
        updateUIVisibility();
    }

    private void updateUIVisibility() {
        if (inInterior) {
            if (!uiStage.getActors().contains(homeButton, true)) {
                uiStage.addActor(homeButton);
            }

            float margin = 10f;
            float size = toggleButton.getWidth();
            float y = toggleButton.getY();

            homeButton.setSize(size, size);
            homeButton.setPosition(toggleButton.getX() - size - margin, y);

        } else {
            homeButton.remove();
        }
    }

    // --- Масштабування ---
    public void resize(int width, int height) {
        uiStage.getViewport().update(width, height, true);

        float margin = 10f;
        float targetHeight = height * 0.12f;

        ScreenUtilsHelper.scaleAndPositionButton(toggleButton, targetHeight, margin,
                uiStage.getViewport().getWorldHeight() - targetHeight - margin);
        ScreenUtilsHelper.scaleAndPositionButton(notesButton, targetHeight, margin,
                toggleButton.getY() - targetHeight);
        ScreenUtilsHelper.scaleAndPositionButton(dossierButton, targetHeight, margin,
                notesButton.getY() - targetHeight);

        float aspectExit = settingsButton.getDrawable().getMinWidth() / settingsButton.getDrawable().getMinHeight();
        settingsButton.setSize(targetHeight * aspectExit, targetHeight);
        settingsButton.setPosition(
                uiStage.getViewport().getWorldWidth() - settingsButton.getWidth() - margin,
                uiStage.getViewport().getWorldHeight() - settingsButton.getHeight() - margin
        );

        if (inInterior) {
            ScreenUtilsHelper.scaleAndPositionButton(homeButton, targetHeight, toggleButton.getWidth() + margin,
                    uiStage.getViewport().getWorldHeight() - targetHeight - margin);
        }

        timer.setPositions(targetHeight);

        if (notePopup != null) notePopup.resize(width, height);
        if (dossierPopup != null) dossierPopup.resize(width, height);
        if (settingsPopup != null) settingsPopup.resize(width, height);
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
        uiStage.getRoot().setVisible(visible);
    }

    public Stage getStage() {
        return uiStage;
    }

    public void dispose() {
        uiStage.dispose();
        arrowDownTexture.dispose();
        arrowUpTexture.dispose();

        if (notePopup != null) notePopup.dispose();
        if (dossierPopup != null) dossierPopup.dispose();
        if (settingsPopup != null) settingsPopup.dispose();

        timer.saveTime();
    }

    public void hideAllPopups() {
        if (settingsPopup != null) settingsPopup.remove();
        if (notePopup != null) notePopup.remove();
        if (dossierPopup != null) dossierPopup.remove();
    }

    public void pauseTimer() {
        timer.pause();
    }

    public void resumeTimer() {
        timer.resume();
    }

    public void resetTimer() {
        timer.reset();
    }
}
