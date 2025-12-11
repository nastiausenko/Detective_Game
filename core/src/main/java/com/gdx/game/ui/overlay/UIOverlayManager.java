package com.gdx.game.ui.overlay;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.gdx.game.DetectiveGame;
import com.gdx.game.domain.investigation.InvestigationState;
import com.gdx.game.ui.component.popup.*;
import com.gdx.game.ui.screens.MapScreen;
import com.gdx.game.ui.component.timer.GameTimer;
import com.gdx.game.infra.assets.Assets;
import com.gdx.game.utils.ScreenUtilsHelper;

public class UIOverlayManager {
    private final DetectiveGame game;
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

    private boolean menuOpened = false;
    private boolean visible = true;
    private boolean inInterior = false;
    private boolean timeOverPopupShown = false;

    public UIOverlayManager(DetectiveGame game) {
        this.game = game;
        this.uiStage = new Stage(new ScreenViewport(), game.batch);

        arrowDownTexture = new Texture(Assets.TOGGLE_BUTTON_DOWN);
        arrowUpTexture = new Texture(Assets.TOGGLE_BUTTON_UP);

        toggleButton = game.getButtonFactory().createButton(Assets.TOGGLE_BUTTON_DOWN, 64, 64, this::toggleMenu);
        notesButton = game.getButtonFactory().createButton(Assets.NOTE_ICON, 64, 64, this::showNotes);
        dossierButton = game.getButtonFactory().createButton(Assets.DOSSIER_BUTTON, 64, 64, this::showDossier);
        settingsButton = game.getButtonFactory().createButton(Assets.SETTINGS_BUTTON, 64, 64, this::showSettings);
        accuseButton = game.getButtonFactory().createButton(Assets.ACCUSATION_BUTTON, 64, 64, this::showAccusation);
        homeButton = game.getButtonFactory().createButton(Assets.HOME_BUTTON, 64, 64, this::backToMap);
        chatButton = game.getButtonFactory().createButton(Assets.CHAT_BUTTON, 64, 64, this::showChatHistory);

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

        popupFactory = new PopupFactory(uiStage, game, game.getTransition());
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
        if (dossierPopup == null)
            dossierPopup = popupFactory.createDossierPopup();

        dossierPopup.loadDatabase(game.getDossierDb());
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

    private void showEpilogue() {
        if (epiloguePopup == null) {
            epiloguePopup = popupFactory.createEpiloguePopup();
        }

        epiloguePopup.setFullText("…");
        epiloguePopup.show();

        final InvestigationState inv = game.getInvestigationState();

        new Thread(() -> {
            String text;
            try {
                text = game.getEpilogueService().generateEpilogue(inv);
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

    public void showEpiloguePublic() {
        showEpilogue();
    }

    public void showTheEndPublic() {
        if (theEndPopup == null) theEndPopup = popupFactory.createTheEndPopup();
        theEndPopup.show();
    }

    private void showPrologue() {
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

    public void showProloguePublic() {
        showPrologue();
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

        checkTimeOverPopup();
    }

    private void checkTimeOverPopup() {
        if (timeOverPopupShown) return;
        if (!timer.isTimeOver()) return;

        InvestigationState inv = game.getInvestigationState();
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
        updateUIVisibility();
    }

    private void updateUIVisibility() {
        if (inInterior) {
            if (!uiStage.getActors().contains(homeButton, true)) {
                uiStage.addActor(homeButton);
                uiStage.addActor(chatButton);
                timer.showGameTimeLabel(false);
            }

            float margin = 10f;
            float size = toggleButton.getWidth();
            float y = toggleButton.getY();

            homeButton.setSize(size, size);
            homeButton.setPosition(toggleButton.getX() - size - margin, y);

            chatButton.setSize(size, size);
            chatButton.setPosition(settingsButton.getX() - size - margin, settingsButton.getY() - margin);

        } else {
            timer.showGameTimeLabel(true);
            homeButton.remove();
            chatButton.remove();
        }
    }

    public void resize(int width, int height) {
        uiStage.getViewport().update(width, height, true);

        float margin = 10f;
        float worldWidth = uiStage.getViewport().getWorldWidth();
        float worldHeight = uiStage.getViewport().getWorldHeight();
        float targetHeight = height * 0.12f;

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
            ScreenUtilsHelper.scaleButton(homeButton, targetHeight, uiStage);
            ScreenUtilsHelper.scaleButton(chatButton, targetHeight, uiStage);

            homeButton.setPosition(
                toggleButton.getX() + toggleButton.getWidth() + margin,
                worldHeight - homeButton.getHeight() - margin
            );

            chatButton.setPosition(settingsButton.getX() - chatButton.getWidth() - margin, settingsButton.getY());
        }

        float badgeSize = toggleButton.getWidth() * 0.2f;
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

        timer.setPositions(targetHeight);

        if (notePopup != null) notePopup.resize(width, height);
        if (dossierPopup != null) dossierPopup.resize(width, height);
        if (settingsPopup != null) settingsPopup.resize(width, height);
        if (accusationPopup != null) accusationPopup.resize(width, height);
        if (chatHistoryPopup != null) chatHistoryPopup.resize(width, height);
        if (epiloguePopup != null) epiloguePopup.resize(width, height);
        if (timeOverPopup != null) timeOverPopup.resize(width, height);
        if (storyPopup != null) storyPopup.resize(width, height);
        if (theEndPopup != null) theEndPopup.resize(width, height);

        updateBadgeVisibility();
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
        if (accusationPopup != null) accusationPopup.dispose();
        if (chatHistoryPopup != null) chatHistoryPopup.dispose();
        if (timeOverPopup != null) timeOverPopup.dispose();
        if (settingsPopup != null) settingsPopup.dispose();
        if (theEndPopup != null) theEndPopup.dispose();

        timer.saveTime();
    }

    public void hideAllPopups() {
        if (settingsPopup != null) settingsPopup.remove();
        if (notePopup != null) notePopup.remove();
        if (dossierPopup != null) dossierPopup.remove();
        if (accusationPopup != null) accusationPopup.remove();
        if (chatHistoryPopup != null) chatHistoryPopup.remove();
        if (epiloguePopup != null) epiloguePopup.remove();
        if (timeOverPopup != null) timeOverPopup.remove();
        if (storyPopup != null) storyPopup.remove();
        if (theEndPopup != null) theEndPopup.remove();
    }

    public void resetTimer() {
        timer.reset();
        timeOverPopupShown = false;

        if (timeOverPopup != null) {
            timeOverPopup.remove();
            timeOverPopup = null;
        }
    }

    public GameTimer getTimer() {
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
