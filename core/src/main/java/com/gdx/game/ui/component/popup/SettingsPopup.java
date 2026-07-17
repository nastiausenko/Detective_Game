package com.gdx.game.ui.component.popup;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.gdx.game.infrastructure.GameContext;
import com.gdx.game.game.GameFlow;
import com.gdx.game.infrastructure.Assets;
import com.gdx.game.ui.style.UiLayout;
import com.gdx.game.ui.style.UiLayoutProfile;

public class SettingsPopup extends AbstractPopup {
    private final Texture settTexture;
    private final Image settImage;

    private final Image exitBtn;
    private final Image continueBtn;
    private final Image soundBtn;

    private final GameContext game;
    private final GameFlow flow;
    private SoundPopup soundPopup;

    public SettingsPopup(Stage stage, GameContext game, GameFlow flow) {
        super(stage);
        this.game = game;
        this.flow = flow;

        settTexture = new Texture(Assets.SETTINGS);
        settImage = new Image(settTexture);
        settImage.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                event.stop();
            }
        });

        continueBtn = game.buttonFactory.createButton(Assets.CONTINUE_BUTTON, 0, 0, this::remove);
        exitBtn = game.buttonFactory.createButton(Assets.EXIT_BUTTON, 0, 0, this::handleExit);
        soundBtn = game.buttonFactory.createButton(Assets.SOUND_SETTINGS_BUTTON, 0, 0, this::showSound);

        resize(stage.getViewport().getWorldWidth(), stage.getViewport().getWorldHeight());
    }

    private void handleExit() {
        flow.showMenu();
    }

    private void showSound() {
        if (soundPopup == null) {
            soundPopup = new SoundPopup(stage, game);
        }

        soundPopup.show();
    }

    public void resize(float screenWidth, float screenHeight) {
        UiLayoutProfile profile = UiLayout.current(screenWidth, screenHeight);
        resizeCentered(settImage, settTexture, screenWidth, screenHeight);

        float btnWidth = settImage.getWidth() * 0.75f;
        float btnHeight = settImage.getHeight() * 0.15f;
        float paddingBottom = settImage.getHeight() * 0.43f;
        float buttonGap = Math.max(settImage.getHeight() / 45f, profile.scale(8f));

        exitBtn.setSize(btnWidth, btnHeight);
        exitBtn.setPosition(settImage.getX() + (settImage.getWidth() - btnWidth) / 2f, settImage.getY() + paddingBottom);

        continueBtn.setSize(btnWidth, btnHeight);
        continueBtn.setPosition(
            settImage.getX() + (settImage.getWidth() - btnWidth) / 2f,
            exitBtn.getY() + btnHeight + buttonGap
        );

        float soundBtnWidth = settImage.getWidth() * (565f / 753f);
        float soundBtnHeight = settImage.getHeight() * (149f / 1024f);
        soundBtn.setSize(soundBtnWidth, soundBtnHeight);
        soundBtn.setPosition(
            settImage.getX() + (settImage.getWidth() - soundBtnWidth) / 2f,
            settImage.getY() + settImage.getHeight() * (96f / 1024f)
        );

        if (soundPopup != null) {
            soundPopup.resize(screenWidth, screenHeight);
        }
    }

    @Override
    public void show() {
        super.show();
        addPopupActors(settImage, continueBtn, exitBtn, soundBtn);
    }

    @Override
    public void remove() {
        if (soundPopup != null) {
            soundPopup.remove();
        }

        super.remove();
        removePopupActors(settImage, continueBtn, exitBtn, soundBtn);
    }

    public void dispose() {
        settTexture.dispose();
        if (soundPopup != null) {
            soundPopup.dispose();
        }
    }
}
