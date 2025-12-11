package com.gdx.game.ui.component.popup;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.Align;
import com.gdx.game.DetectiveGame;
import com.gdx.game.domain.investigation.InvestigationState;
import com.gdx.game.infra.assets.Assets;
import com.gdx.game.infra.assets.FontScaler;

public class TimeOverPopup extends AbstractPopup {

    private final Texture backgroundTexture;
    private final Image backgroundImage;

    private final Label messageLabel;
    private final Image yesButton;
    private final Image noButton;

    private final Skin skin;

    public TimeOverPopup(Stage stage, DetectiveGame game) {
        super(stage);

        backgroundTexture = new Texture(Assets.TIME_OVER_POPUP);
        backgroundImage = new Image(backgroundTexture);

        skin = new Skin(Gdx.files.internal("ui/uiskin.json"));

        Label.LabelStyle style = new Label.LabelStyle();
        style.font = skin.getFont("default-font");
        style.fontColor = new Color(154 / 255f, 109 / 255f, 69 / 255f, 1f);

        FontScaler.applyScale(style.font);

        messageLabel = new Label("Час закінчився.\nПерейти до звинувачення?", style);
        messageLabel.setAlignment(Align.center);
        messageLabel.setWrap(true);

        yesButton = game.getButtonFactory().createButton(Assets.YES_BUTTON, 60, 60, () -> {
            remove();
            game.overlay.showAccusation();
        });

        noButton = game.getButtonFactory().createButton(Assets.NO_BUTTON, 60, 60, () -> {
            remove();

            InvestigationState inv = game.getInvestigationState();
            if (inv != null) {
                inv.accusedNpcId = null;
                inv.accusationDone = true;
            }

            game.overlay.showEpiloguePublic();
        });

        resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
    }

    public void resize(float screenWidth, float screenHeight) {
        background.setSize(screenWidth, screenHeight);
        resizeCentered(backgroundImage, backgroundTexture, screenWidth, screenHeight);

        float height = backgroundImage.getHeight();
        float width = backgroundImage.getWidth();

        messageLabel.setWidth(backgroundImage.getWidth() * 0.7f);
        messageLabel.setPosition(
            backgroundImage.getX() + width * 0.15f,
            backgroundImage.getY() + height * 0.52f
        );

        FontScaler.applyScale(skin.getFont("default-font"));

        float btnWidth = width * 0.2f;
        float btnHeight = height * 0.2f;
        float paddingBottom = height * 0.2f;

        yesButton.setSize(btnWidth, btnHeight);
        noButton.setSize(btnWidth, btnHeight);

        yesButton.setPosition(
            backgroundImage.getX() + width/2 - btnWidth * 1.2f,
            backgroundImage.getY() + paddingBottom
        );

        noButton.setPosition(
            yesButton.getX() + btnWidth * 1.4f,
            backgroundImage.getY() + paddingBottom
        );
    }

    @Override
    public void show() {
        super.show();
        stage.addActor(backgroundImage);
        stage.addActor(messageLabel);
        stage.addActor(yesButton);
        stage.addActor(noButton);

        resize(stage.getViewport().getWorldWidth(), stage.getViewport().getWorldHeight());
    }

    @Override
    public void remove() {
        super.remove();
        backgroundImage.remove();
        messageLabel.remove();
        yesButton.remove();
        noButton.remove();
    }

    public void dispose() {
        backgroundTexture.dispose();
        skin.dispose();
    }
}
