package com.gdx.game.app.overlay;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.gdx.game.app.model.GameContext;
import com.gdx.game.app.navigation.GameFlow;
import com.gdx.game.model.InvestigationState;
import com.gdx.game.widgets.popup.*;
import com.gdx.game.widgets.timer.GameTimer;
import com.gdx.game.shared.config.Assets;
import com.gdx.game.shared.config.UiLayout;
import com.gdx.game.shared.config.UiLayoutProfile;
import com.gdx.game.shared.lib.ScreenUtilsHelper;

public class UIOverlayManager {
    private final GameContext game;
    private final GameFlow flow;
    public final Stage uiStage;

    private final Image toggleButton;
    private final Image notesButton;
    private final Image dossierButton;
    private final Image settingsButton;
    private final Image accuseButton;
    private final Image homeButton;
    private final Image chatButton;

    private final Image dossierBadge;
    private final Image toggleBadge;
    private int newFactsCount = 0;

    private final Texture arrowDownTexture;
    private final Texture arrowUpTexture;

    private final GameTimer timer;
    private final PopupFactory popupFactory;

    private NotePopup notePopup;
    private DossierPopup dossierPopup;
    private AccusationPopup accusationPopup;
    private SettingsPopup settingsPopup;
    private ChatHistoryPopup chatHistoryPopup;
    private EpiloguePopup epiloguePopup;
    private TimeOverPopup timeOverPopup;
    private StoryPopup storyPopup;
    private TheEndPopup theEndPopup;
    private String currentNpcId;
    private String currentInteriorBuildingId;

    private boolean menuOpened = false;
    private boolean visible = true;
    private boolean inInterior = false;
    private boolean timeOverPopupShown = false;

    public UIOverlayManager(GameContext game, GameFlow flow) {
        this.game = game;
        this.flow = flow;
        this.uiStage = new Stage(new ScreenViewport(), game.batch);

        arrowDownTexture = new Texture(Assets.TOGGLE_BUTTON_DOWN);
        arrowUpTexture = new Texture(Assets.TOGGLE_BUTTON_UP);

        toggleButton = game.buttonFactory.createButton(Assets.TOGGLE_BUTTON_DOWN, 64, 64, this::toggleMenu);
        notesButton = game.buttonFactory.createButton(Assets.NOTE_ICON, 64, 64, this::showNotes);
        dossierButton = game.buttonFactory.createButton(Assets.DOSSIER_BUTTON, 64, 64, this::showDossier);
        settingsButton = game.buttonFactory.createButton(Assets.SETTINGS_BUTTON, 64, 64, this::showSettings);
        accuseButton = game.buttonFactory.createButton(Assets.ACCUSATION_BUTTON, 64, 64, this::showAccusation);
        homeButton = game.buttonFactory.createButton(Assets.HOME_BUTTON, 64, 64, this::backToMap, false);
        chatButton = game.buttonFactory.createButton(Assets.CHAT_BUTTON, 64, 64, this::showChatHistory);

        dossierBadge = new Image(new Texture(Assets.BADGE));
        toggleBadge  = new Image(new Texture(Assets.BADGE));
        dossierBadge.setVisible(false);
        toggleBadge.setVisible(false);

        notesButton.setVisible(false);
        dossierButton.setVisible(false);
        accuseButton.setVisible(false);

        uiStage.addActor(toggleButton);
        uiStage.addActor(notesButton);
        uiStage.addActor(dossierButton);
        uiStage.addActor(accuseButton);
        uiStage.addActor(settingsButton);

        uiStage.addActor(dossierBadge);
        uiStage.addActor(toggleBadge);

        popupFactory = new PopupFactory(uiStage, game, flow);
        timer = new GameTimer(uiStage, Assets.TOTAL_TIME);
    }

    private void toggleMenu() {
        menuOpened = !menuOpened;
        notesButton.setVisible(menuOpened);
        dossierButton.setVisible(menuOpened);
        accuseButton.setVisible(menuOpened);
        toggleButton.setDrawable(new Image(menuOpened ? arrowUpTexture : arrowDownTexture).getDrawable());

        updateBadgeVisibility();
    }

    private void backToMap() {
        if (!inInterior) return;

        flow.returnToMapFromInterior(currentInteriorBuildingId);
    }

    private void showNotes() {
        if (notePopup == null) notePopup = popupFactory.createNotePopup();
        notePopup.show();
    }

    private void showDossier() {
        if (dossierPopup == null)
            dossierPopup = popupFactory.createDossierPopup();

        dossierPopup.loadDatabase(game.dossierDb);
        dossierPopup.show();

        onDossierOpened();
    }

    public void showAccusation() {
        if (accusationPopup == null) accusationPopup = popupFactory.createAccusationPopup();
        accusationPopup.show();
    }

    public void setCurrentNpcId(String npcId) {
        this.currentNpcId = npcId;

        if (chatHistoryPopup != null) {
            chatHistoryPopup.remove();
            chatHistoryPopup = null;
        }

        chatButton.setVisible(inInterior && currentNpcId != null && !currentNpcId.isEmpty());
    }

    public void setCurrentInteriorBuildingId(String buildingId) {
        this.currentInteriorBuildingId = buildingId;
    }

    private void showChatHistory() {
        if (chatHistoryPopup == null) chatHistoryPopup = popupFactory.createChatHistoryPopup(currentNpcId);
        chatHistoryPopup.show();
    }

    private void showSettings() {
        if (settingsPopup == null) settingsPopup = popupFactory.createSettingsPopup();
        timer.saveTime();
        settingsPopup.show();
    }

    public void showEpilogue() {
        if (epiloguePopup == null) {
            epiloguePopup = popupFactory.createEpiloguePopup();
        }

        final InvestigationState inv = game.investigationState;
        String cachedText = game.epilogueService.getCachedEpilogue(inv);
        epiloguePopup.setFullText(cachedText != null ? cachedText : "…");
        epiloguePopup.show();

        if (cachedText != null) {
            return;
        }

        new Thread(() -> {
            String text;
            try {
                text = game.epilogueService.generateEpilogue(inv);
            } catch (Exception e) {
                e.printStackTrace();
                text = "Щось пішло не так з епілогом. "
                    + "Але місто Розенфельд усе одно пам'ятатиме цю справу.";
            }

            final String finalText = text;
            Gdx.app.postRunnable(() -> {
                epiloguePopup.setFullText(finalText);
            });
        }).start();
    }

    public void showTheEnd() {
        if (theEndPopup == null) theEndPopup = popupFactory.createTheEndPopup();
        theEndPopup.show();
    }

    public void showPrologue() {
        Preferences prefs = Gdx.app.getPreferences("game_data");
        boolean isFirstRun = prefs.getBoolean("isFirstRun", true);
        if (isFirstRun) {
            storyPopup = popupFactory.createStoryPopup();
            storyPopup.show();
            prefs.putBoolean("isFirstRun", false);
            timer.reset();
            prefs.flush();
        }
    }

    public void render(float delta) {
        if (!visible) return;

        uiStage.act(delta);
        uiStage.draw();

        boolean hasBlockingPopup = (settingsPopup != null && settingsPopup.isVisible()) ||
                (storyPopup != null && storyPopup.isVisible());

        if (hasBlockingPopup) {
            timer.pause();
        } else {
            timer.resume();
        }

        if (epiloguePopup != null) {
            epiloguePopup.update(delta);
        }

        if (storyPopup != null) {
            storyPopup.update(delta);
        }

        timer.update(delta);
        game.npcLocationService.updateForGameMinutes(timer.getElapsedGameMinutes());

        checkTimeOverPopup();
    }

    private void checkTimeOverPopup() {
        if (timeOverPopupShown) return;
        if (!timer.isTimeOver()) return;

        InvestigationState inv = game.investigationState;
        if (inv != null && inv.accusationDone) return;
        if (epiloguePopup != null && epiloguePopup.isVisible()) return;

        timeOverPopupShown = true;
        timer.pause();

        showTimeOver();
    }

    private void showTimeOver() {
        timeOverPopup = popupFactory.createTimeOverPopup();
        timeOverPopup.show();
    }

    public void setInInterior(boolean value) {
        this.inInterior = value;
        if (!value) {
            currentInteriorBuildingId = null;
        }
        updateUIVisibility();
    }

    private void updateUIVisibility() {
        if (inInterior) {
            if (!uiStage.getActors().contains(homeButton, true)) {
                uiStage.addActor(homeButton);
                uiStage.addActor(chatButton);
                timer.showGameTimeLabel(false);
            }
            layoutInteriorButtons(
                UiLayout.current(),
                uiStage.getViewport().getWorldWidth(),
                uiStage.getViewport().getWorldHeight()
            );
            chatButton.setVisible(currentNpcId != null && !currentNpcId.isEmpty());

        } else {
            timer.showGameTimeLabel(true);
            homeButton.remove();
            chatButton.remove();
        }
    }

    public void resize(int width, int height) {
        uiStage.getViewport().update(width, height, true);

        float worldWidth = uiStage.getViewport().getWorldWidth();
        float worldHeight = uiStage.getViewport().getWorldHeight();
        UiLayoutProfile profile = UiLayout.current(worldWidth, worldHeight);
        float margin = profile.scale(10f);
        float targetHeight = height * profile.getOverlayButtonHeightRatio();

        ScreenUtilsHelper.scaleButton(toggleButton, targetHeight, uiStage);
        ScreenUtilsHelper.scaleButton(notesButton, targetHeight, uiStage);
        ScreenUtilsHelper.scaleButton(dossierButton, targetHeight, uiStage);
        ScreenUtilsHelper.scaleButton(accuseButton, targetHeight, uiStage);
        ScreenUtilsHelper.scaleButton(settingsButton, targetHeight, uiStage);

        toggleButton.setPosition(margin, worldHeight - toggleButton.getHeight() - margin);
        notesButton.setPosition(margin, toggleButton.getY() - notesButton.getHeight());
        dossierButton.setPosition(margin, notesButton.getY() - dossierButton.getHeight());
        accuseButton.setPosition(margin, dossierButton.getY() - accuseButton.getHeight());

        settingsButton.setPosition(
            worldWidth - settingsButton.getWidth() - margin,
            worldHeight - settingsButton.getHeight() - margin
        );

        if (inInterior) {
            layoutInteriorButtons(profile, worldWidth, worldHeight);
        }

        float badgeSize = toggleButton.getWidth() * profile.getBadgeSizeRatio();
        dossierBadge.setSize(badgeSize, badgeSize);
        toggleBadge.setSize(badgeSize, badgeSize);

        dossierBadge.setPosition(
            dossierButton.getX() + dossierButton.getWidth() - badgeSize * 1.5f,
            dossierButton.getY() + dossierButton.getHeight() - badgeSize * 0.9f
        );

        toggleBadge.setPosition(
            toggleButton.getX() + toggleButton.getWidth() - badgeSize * 1.5f,
            toggleButton.getY() + toggleButton.getHeight() - badgeSize * 0.9f
        );

        timer.setPositions(targetHeight, profile);

        resizePopup(notePopup, width, height);
        resizePopup(dossierPopup, width, height);
        resizePopup(settingsPopup, width, height);
        resizePopup(accusationPopup, width, height);
        resizePopup(chatHistoryPopup, width, height);
        resizePopup(epiloguePopup, width, height);
        resizePopup(timeOverPopup, width, height);
        resizePopup(storyPopup, width, height);
        resizePopup(theEndPopup, width, height);

        updateBadgeVisibility();
    }

    private void resizePopup(AbstractPopup popup, int width, int height) {
        if (popup != null) {
            popup.resize(width, height);
        }
    }

    private void layoutInteriorButtons(UiLayoutProfile profile, float worldWidth, float worldHeight) {
        float margin = profile.scale(10f);
        float targetHeight = worldHeight * profile.getOverlayButtonHeightRatio();

        ScreenUtilsHelper.scaleButton(homeButton, targetHeight, uiStage);
        ScreenUtilsHelper.scaleButton(chatButton, targetHeight, uiStage);

        homeButton.setPosition(
            toggleButton.getX() + toggleButton.getWidth() + margin,
            worldHeight - homeButton.getHeight() - margin
        );

        chatButton.setPosition(
            worldWidth - settingsButton.getWidth() - chatButton.getWidth() - margin * 2f,
            settingsButton.getY()
        );
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

        disposePopup(notePopup);
        disposePopup(dossierPopup);
        disposePopup(settingsPopup);
        disposePopup(accusationPopup);
        disposePopup(chatHistoryPopup);
        disposePopup(epiloguePopup);
        disposePopup(timeOverPopup);
        disposePopup(storyPopup);
        disposePopup(theEndPopup);

        timer.saveTime();
    }

    public void hideAllPopups() {
        removePopup(settingsPopup);
        removePopup(notePopup);
        removePopup(dossierPopup);
        removePopup(accusationPopup);
        removePopup(chatHistoryPopup);
        removePopup(epiloguePopup);
        removePopup(timeOverPopup);
        removePopup(storyPopup);
        removePopup(theEndPopup);
    }

    private void removePopup(AbstractPopup popup) {
        if (popup != null) {
            popup.remove();
        }
    }

    private void disposePopup(AbstractPopup popup) {
        if (popup != null) {
            popup.dispose();
        }
    }

    public void resetTimer() {
        timer.reset();
        timeOverPopupShown = false;

        if (timeOverPopup != null) {
            timeOverPopup.remove();
            timeOverPopup = null;
        }
    }

    public void resetForNewGame() {
        resetTimer();

        currentNpcId = null;
        currentInteriorBuildingId = null;
        newFactsCount = 0;
        menuOpened = false;
        inInterior = false;

        notesButton.setVisible(false);
        dossierButton.setVisible(false);
        accuseButton.setVisible(false);
        toggleButton.setDrawable(new Image(arrowDownTexture).getDrawable());

        if (notePopup != null) {
            removePopup(notePopup);
            disposePopup(notePopup);
            notePopup = null;
        }

        if (chatHistoryPopup != null) {
            removePopup(chatHistoryPopup);
            chatHistoryPopup = null;
        }

        updateUIVisibility();
        updateBadgeVisibility();
    }

    public GameTimer timer() {
        return timer;
    }

    public void onNewFactsDiscovered(int count) {
        if (count <= 0) return;
        newFactsCount += count;
        updateBadgeVisibility();
    }

    private void onDossierOpened() {
        newFactsCount = 0;
        updateBadgeVisibility();
    }

    private void updateBadgeVisibility() {
        boolean hasNewFacts = newFactsCount > 0;

        dossierBadge.setVisible(hasNewFacts && menuOpened);

        toggleBadge.setVisible(hasNewFacts && !menuOpened);
    }
}
