package com.gdx.game.ui.popup;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.gdx.game.DetectiveGame;
import com.gdx.game.screens.MenuScreen;
import com.gdx.game.utils.FadeTransition;

public class SettingsPopup extends AbstractPopup {
    private final Texture settTexture;
    private final Image settImage;

    private final Image exitBtn;
    private final Image continueBtn;

    private final DetectiveGame game;
    private final FadeTransition transition;

    public SettingsPopup(Stage stage, String texturePath, DetectiveGame game, FadeTransition transition) {
        super(stage);
        this.game = game;
        this.transition = transition;

        settTexture = new Texture(texturePath);
        settImage = new Image(settTexture);
        settImage.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                event.stop();
            }
        });

        continueBtn = game.getButtonFactory().createButton("menu/settings/continue_btn.png", 0, 0, this::remove);
        exitBtn = game.getButtonFactory().createButton("menu/settings/exit_btn.png", 0, 0, this::handleExit);

        resize(stage.getViewport().getWorldWidth(), stage.getViewport().getWorldHeight());
    }

    private void handleExit() {
        if (!transition.isTransitioning()) {
            transition.startFadeOut(0.7f, () -> {
                game.setScreen(new MenuScreen(game, transition));
                transition.startFadeIn(0.7f);
            });
        }
    }

    public void resize(float screenWidth, float screenHeight) {
        background.setSize(screenWidth, screenHeight);
        resizeCentered(settImage, settTexture, screenWidth, screenHeight);

        float btnWidth = settImage.getWidth() * 0.75f;
        float btnHeight = settImage.getHeight() * 0.15f;
        float paddingBottom = settImage.getHeight() * 0.43f;

        exitBtn.setSize(btnWidth, btnHeight);
        exitBtn.setPosition(settImage.getX() + (settImage.getWidth() - btnWidth) / 2f, settImage.getY() + paddingBottom);

        continueBtn.setSize(btnWidth, btnHeight);
        continueBtn.setPosition(
            settImage.getX() + (settImage.getWidth() - btnWidth) / 2f,
            exitBtn.getY() + btnHeight + settImage.getHeight() / 45f
        );
    }

    @Override
    public void show() {
        super.show();
        stage.addActor(settImage);
        stage.addActor(continueBtn);
        stage.addActor(exitBtn);
    }

    @Override
    public void remove() {
        super.remove();
        settImage.remove();
        continueBtn.remove();
        exitBtn.remove();
    }

    public void dispose() {
        settTexture.dispose();
    }
}
