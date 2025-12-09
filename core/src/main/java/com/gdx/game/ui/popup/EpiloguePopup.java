package com.gdx.game.ui.popup;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.Align;
import com.gdx.game.DetectiveGame;
import com.gdx.game.utils.Assets;
import com.gdx.game.utils.FontScaler;

public class EpiloguePopup extends AbstractPopup {
    private final Image epilogueImage;
    private final Texture epilogueTexture;
    private final Label epilogueLabel;
    private final Image continueButton;

    // TODO: epilogue llm service
    private final String fullText = "This is the prologue of the story. Your adventure begins now... " +
        "This is the prologue of the story. Your adventure begins now..." +
        "This is the prologue of the story. Your adventure begins now..." +
        "This is the prologue of the story. Your adventure begins now...";
    private final StringBuilder sb = new StringBuilder();
    private float charTimer = 0f;
    private final float charDelay = 0.05f;
    private int charIndex = 0;
    private boolean finished = false;

    private final DetectiveGame game;
    private final Skin skin;

    public EpiloguePopup(Stage stage, DetectiveGame game) {
        super(stage);
        this.game = game;

        epilogueTexture = new Texture(Assets.EPILOGUE);
        epilogueImage = new Image(epilogueTexture);

        skin = new Skin(Gdx.files.internal("ui/uiskin.json"));

        Label.LabelStyle labelStyle = new Label.LabelStyle();
        labelStyle.font = skin.getFont("default-font");
        labelStyle.fontColor = new Color(154 / 255f, 109 / 255f, 69 / 255f, 1f);

        epilogueLabel = new Label("", labelStyle);
        epilogueLabel.setWrap(true);
        epilogueLabel.setAlignment(Align.center);

        continueButton = game.getButtonFactory().createButton(
            Assets.CONTINUE_BUTTON, 60, 60,
            () -> {
                if (finished) this.remove();
                else finishText();
            }
        );

        resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
    }

    public void update(float delta) {
        if (finished) return;

        charTimer += delta;
        if (charTimer >= charDelay && charIndex < fullText.length()) {
            sb.append(fullText.charAt(charIndex));
            charIndex++;
            epilogueLabel.setText(sb.toString());
            charTimer = 0f;
        }

        if (charIndex >= fullText.length()) finished = true;
    }

    public void resize(float screenWidth, float screenHeight) {
        epilogueImage.setWidth(screenWidth);
        background.setSize(screenWidth, screenHeight);
        resizeCentered(epilogueImage, epilogueTexture, screenWidth, screenHeight);

        float btnWidth = epilogueImage.getWidth() * 0.5f;
        float btnHeight = epilogueImage.getHeight() * 0.1f;
        float paddingBottom = epilogueImage.getHeight() * 0.1f;

        epilogueLabel.setWidth(epilogueImage.getWidth() * 0.7f);
        epilogueLabel.setPosition(
            epilogueImage.getX() + epilogueImage.getWidth() * 0.15f,
            epilogueImage.getY() + epilogueImage.getHeight() * 0.52f
        );

        FontScaler.applyScale(skin.getFont("default-font"));

        continueButton.setSize(btnWidth, btnHeight);
        continueButton.setPosition(epilogueImage.getX() + (epilogueImage.getWidth() - btnWidth) / 2f, epilogueImage.getY() + paddingBottom);
    }

    private void finishText() {
        sb.setLength(0);
        sb.append(fullText);
        epilogueLabel.setText(fullText);
        finished = true;
    }

    @Override
    public void show() {
        super.show();
        stage.addActor(epilogueImage);
        stage.addActor(epilogueLabel);
        stage.addActor(continueButton);

    }

    @Override
    public void remove() {
        super.remove();
        epilogueImage.remove();
        epilogueLabel.remove();
        continueButton.remove();
    }
}
