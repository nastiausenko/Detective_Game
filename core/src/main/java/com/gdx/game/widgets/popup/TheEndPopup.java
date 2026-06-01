package com.gdx.game.widgets.popup;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.utils.Align;
import com.gdx.game.app.model.GameContext;
import com.gdx.game.app.navigation.GameFlow;
import com.gdx.game.shared.config.Assets;
import com.gdx.game.shared.ui.UiStyles;

public class TheEndPopup extends AbstractPopup {

    private final Texture backgroundTexture;
    private final Image backgroundImage;

    private final Label messageLabel;
    private final Image yesButton;
    private final Image noButton;

    public TheEndPopup(Stage stage, GameContext game, GameFlow flow) {
        super(stage);

        backgroundTexture = new Texture(Assets.TIME_OVER_POPUP);
        backgroundImage = new Image(backgroundTexture);

        Label.LabelStyle style = UiStyles.label(skin, UiStyles.parchmentText());
        messageLabel = new Label("Почати нову гру?", style);
        messageLabel.setAlignment(Align.center);
        messageLabel.setWrap(true);

        yesButton = game.buttonFactory.createButton(
            Assets.YES_BUTTON, 60, 60,
            () -> {
                remove();
                flow.startNewGame();
            }
        );

        noButton = game.buttonFactory.createButton(
            Assets.NO_BUTTON, 60, 60,
            () -> {
                remove();
                flow.showMenu();
            }
        );


        resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
    }

    public void resize(float screenWidth, float screenHeight) {
        ConfirmPopupLayout.resize(this, backgroundImage, backgroundTexture, messageLabel, yesButton, noButton,
            screenWidth, screenHeight);
    }

    @Override
    public void show() {
        super.show();
        ConfirmPopupLayout.show(this, stage, backgroundImage, messageLabel, yesButton, noButton);
    }

    @Override
    public void remove() {
        super.remove();
        ConfirmPopupLayout.remove(this, backgroundImage, messageLabel, yesButton, noButton);
    }

    public void dispose() {
        backgroundTexture.dispose();
        skin.dispose();
    }
}
