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

public class StoryPopup extends AbstractPopup {
    private final Image storyImage;
    private final Texture storyTexture;
    private final Label storyLabel;
    private final Image continueButton;

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

    public StoryPopup(Stage stage, DetectiveGame game) {
        super(stage);
        this.game = game;

        storyTexture = new Texture(Assets.PROLOGUE);
        storyImage = new Image(storyTexture);

        skin = new Skin(Gdx.files.internal("ui/uiskin.json"));

        Label.LabelStyle labelStyle = new Label.LabelStyle();
        labelStyle.font = skin.getFont("default-font");
        labelStyle.fontColor = new Color(154 / 255f, 109 / 255f, 69 / 255f, 1f);

        storyLabel = new Label("", labelStyle);
        storyLabel.setWrap(true);
        storyLabel.setAlignment(Align.center);

        continueButton = game.getButtonFactory().createButton(
            Assets.CONTINUE_BUTTON, 60, 60,
            () -> {
                if (finished) this.remove();
                else finishText();
            }
        );

    }

    public void update(float delta) {
        if (finished) return;

        charTimer += delta;
        if (charTimer >= charDelay && charIndex < fullText.length()) {
            sb.append(fullText.charAt(charIndex));
            charIndex++;
            storyLabel.setText(sb.toString());
            charTimer = 0f;
        }

        if (charIndex >= fullText.length()) finished = true;
    }

    public void resize(float screenWidth, float screenHeight) {
        storyImage.setWidth(screenWidth);
        background.setSize(screenWidth, screenHeight);
        resizeCentered(storyImage, storyTexture, screenWidth, screenHeight);

        float btnWidth = storyImage.getWidth() * 0.5f;
        float btnHeight = storyImage.getHeight() * 0.1f;
        float paddingBottom = storyImage.getHeight() * 0.1f;


        storyLabel.setWidth(storyImage.getWidth() * 0.7f);
        storyLabel.setPosition(
                storyImage.getX() + storyImage.getWidth() * 0.15f,
                storyImage.getY() + storyImage.getHeight() * 0.52f
        );

        FontScaler.applyScale(skin.getFont("default-font"));

        continueButton.setSize(btnWidth, btnHeight);
        continueButton.setPosition(storyImage.getX() + (storyImage.getWidth() - btnWidth) / 2f, storyImage.getY() + paddingBottom);
    }

    private void finishText() {
        sb.setLength(0);
        sb.append(fullText);
        storyLabel.setText(fullText);
        finished = true;
    }

    @Override
    public void show() {
        super.show();
        stage.addActor(storyImage);
        stage.addActor(storyLabel);
        stage.addActor(continueButton);

    }

    @Override
    public void remove() {
        super.remove();
        storyImage.remove();
        storyLabel.remove();
        continueButton.remove();
    }
}
