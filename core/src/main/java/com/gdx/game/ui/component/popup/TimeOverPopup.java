package com.gdx.game.ui.component.popup;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.utils.Align;
import com.gdx.game.infrastructure.GameContext;
import com.gdx.game.game.GameFlow;
import com.gdx.game.domain.investigation.InvestigationState;
import com.gdx.game.infrastructure.Assets;
import com.gdx.game.ui.style.UiStyles;

public class TimeOverPopup extends AbstractPopup {

    private final Texture backgroundTexture;
    private final Image backgroundImage;

    private final Label messageLabel;
    private final Image yesButton;
    private final Image noButton;

    public TimeOverPopup(Stage stage, GameContext game, GameFlow flow) {
        super(stage);

        backgroundTexture = new Texture(Assets.TIME_OVER_POPUP);
        backgroundImage = new Image(backgroundTexture);

        Label.LabelStyle style = UiStyles.label(skin, UiStyles.parchmentText());
        messageLabel = new Label("Час закінчився.\nПерейти до звинувачення?", style);
        messageLabel.setAlignment(Align.center);
        messageLabel.setWrap(true);

        yesButton = game.buttonFactory.createButton(Assets.YES_BUTTON, 60, 60, () -> {
            remove();
            flow.showAccusation();
        });

        noButton = game.buttonFactory.createButton(Assets.NO_BUTTON, 60, 60, () -> {
            remove();

            InvestigationState inv = game.investigationState;
            if (inv != null) {
                inv.accusedNpcId = null;
                inv.accusationDone = true;
            }

            flow.showEpilogue();
        });

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
