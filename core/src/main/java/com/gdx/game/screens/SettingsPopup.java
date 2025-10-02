package com.gdx.game.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.gdx.game.DetectiveGame;
import com.gdx.game.utils.FadeTransition;

public class SettingsPopup {
    private final Texture settTexture;
    private final Image settImage;
    private final Image background;
    private final Stage stage;

    private final Image exitBtn;
    private final Image continueBtn;
    private final Texture exitBtnTexture;
    private final Texture continueBtnTexture;

    public SettingsPopup(Stage stage, String texturePath, DetectiveGame game, FadeTransition transition) {
        this.stage = stage;

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

        continueBtnTexture = new Texture("menu/settings/continue_btn.png");
        continueBtn = new Image(continueBtnTexture);
        continueBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                remove();
            }
        });

        exitBtnTexture = new Texture("menu/settings/exit_btn.png");
        exitBtn = new Image(exitBtnTexture);
        exitBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (!transition.isTransitioning()) {
                    transition.startFadeOut(0.7f, () -> {
                        game.setScreen(new MenuScreen(game));
                        transition.startFadeIn(0.7f);
                    });
                }
            }
        });

        resize(stage.getViewport().getWorldWidth(), stage.getViewport().getWorldHeight());
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

        continueBtn.setSize(btnWidth, btnHeight);
        continueBtn.setPosition(
            settImage.getX() + (settImage.getWidth() - btnWidth) / 2f,
            exitBtn.getY() + btnHeight + settImage.getHeight() * 0.5f
        );

        exitBtn.setSize(btnWidth, btnHeight);
        exitBtn.setPosition(
            settImage.getX() + (settImage.getWidth() - btnWidth) / 2f,
            settImage.getY() + paddingBottom
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
        exitBtnTexture.dispose();
        continueBtnTexture.dispose();
    }
}
