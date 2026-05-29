package com.gdx.game.widgets.popup;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.gdx.game.shared.ui.FontScaler;

public final class ConfirmPopupLayout {
    private ConfirmPopupLayout() {
    }

    public static void resize(
        AbstractPopup popup,
        Image backgroundImage,
        Texture backgroundTexture,
        Label messageLabel,
        Image yesButton,
        Image noButton,
        float screenWidth,
        float screenHeight
    ) {
        popup.resizeCentered(backgroundImage, backgroundTexture, screenWidth, screenHeight);

        float width = backgroundImage.getWidth();
        float height = backgroundImage.getHeight();

        messageLabel.setWidth(width * 0.7f);
        messageLabel.setPosition(
            backgroundImage.getX() + width * 0.15f,
            backgroundImage.getY() + height * 0.52f
        );
        FontScaler.applyScale(messageLabel.getStyle().font);

        float btnWidth = width * 0.2f;
        float btnHeight = height * 0.2f;
        float paddingBottom = height * 0.2f;

        yesButton.setSize(btnWidth, btnHeight);
        noButton.setSize(btnWidth, btnHeight);

        yesButton.setPosition(
            backgroundImage.getX() + width / 2f - btnWidth * 1.2f,
            backgroundImage.getY() + paddingBottom
        );

        noButton.setPosition(
            yesButton.getX() + btnWidth * 1.4f,
            backgroundImage.getY() + paddingBottom
        );
    }

    public static void show(AbstractPopup popup, Stage stage, Image backgroundImage, Label messageLabel,
                            Image yesButton, Image noButton) {
        popup.addPopupActors(backgroundImage, messageLabel, yesButton, noButton);
        popup.resize(stage.getViewport().getWorldWidth(), stage.getViewport().getWorldHeight());
    }

    public static void remove(AbstractPopup popup, Image backgroundImage, Label messageLabel,
                              Image yesButton, Image noButton) {
        popup.removePopupActors(backgroundImage, messageLabel, yesButton, noButton);
    }
}
