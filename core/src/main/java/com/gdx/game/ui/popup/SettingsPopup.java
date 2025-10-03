package com.gdx.game.ui.popup;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.gdx.game.DetectiveGame;
import com.gdx.game.screens.MenuScreen;
import com.gdx.game.utils.FadeTransition;

public class SettingsPopup {
    private final Stage stage;
    private final Texture settTexture;
    private final Image settImage;
    private final Image background;

    private final Image exitBtn;
    private final Image continueBtn;

    private final DetectiveGame game;
    private final FadeTransition transition;

    public SettingsPopup(Stage stage, String texturePath, DetectiveGame game, FadeTransition transition) {
        this.stage = stage;
        this.game = game;
        this.transition = transition;

        background = new Image(new Texture("background.png"));
        background.setColor(0, 0, 0, 0.5f);
        background.setSize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        settTexture = new Texture(texturePath);
        settImage = new Image(settTexture);
        settImage.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                event.stop();
            }
        });

        continueBtn = game.getButtonFactory().createButton(
            "menu/settings/continue_btn.png",
            0, 0,
            this::remove
        );

        exitBtn = game.getButtonFactory().createButton(
            "menu/settings/exit_btn.png",
            0, 0,
            this::handleExit
        );

        resize(stage.getViewport().getWorldWidth(), stage.getViewport().getWorldHeight());
    }

    private void handleExit() {
        if (!transition.isTransitioning()) {
            transition.startFadeOut(0.7f, () -> {
                game.setScreen(new MenuScreen(game));
                transition.startFadeIn(0.7f);
            });
        }
    }

    public void resize(float screenWidth, float screenHeight) {
        background.setSize(screenWidth, screenHeight);

        float maxWidth = screenWidth * 0.9f;
        float maxHeight = screenHeight * 0.9f;
        float aspect = settTexture.getWidth() / (float) settTexture.getHeight();

        float width = settTexture.getWidth();
        float height = settTexture.getHeight();

        if (width > maxWidth) {
            width = maxWidth;
            height = width / aspect;
        }
        if (height > maxHeight) {
            height = maxHeight;
            width = height * aspect;
        }

        settImage.setSize(width, height);
        settImage.setPosition((screenWidth - width) / 2f, (screenHeight - height) / 2f);

        float btnWidth = settImage.getWidth()*0.75f;
        float btnHeight = settImage.getHeight()*0.15f;
        float paddingBottom = settImage.getHeight() * 0.43f;

        exitBtn.setSize(btnWidth, btnHeight);
        exitBtn.setPosition(settImage.getX() + (width - btnWidth) / 2f, settImage.getY() + paddingBottom);

        continueBtn.setSize(btnWidth, btnHeight);
        continueBtn.setPosition(
            settImage.getX() + (width - btnWidth) / 2f,
            exitBtn.getY() + btnHeight + height / 45f
        );
    }

    public void show() {
        stage.addActor(background);
        stage.addActor(settImage);
        stage.addActor(continueBtn);
        stage.addActor(exitBtn);
    }

    public void remove() {
        settImage.remove();
        background.remove();
        continueBtn.remove();
        exitBtn.remove();
    }

    public void dispose() {
        settTexture.dispose();
    }
}
