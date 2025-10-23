package com.gdx.game.ui.popup;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.Align;
import com.gdx.game.DetectiveGame;
import com.gdx.game.utils.Assets;

public class StoryPopup extends AbstractPopup {
    private final Image storyImage;
    private final Texture storyTexture;
    private final Label storyLabel;
    private final Image continueButton;

    private final String fullText = "This is the prologue of the story. Your adventure begins now...";
    private final StringBuilder sb = new StringBuilder();
    private float charTimer = 0f;
    private final float charDelay = 0.05f;
    private int charIndex = 0;
    private boolean finished = false;

    private final DetectiveGame game;

    public StoryPopup(Stage stage, DetectiveGame game) {
        super(stage);
        this.game = game;

        // Картинка прологу
        storyTexture = new Texture(Assets.PROLOGUE);
        storyImage = new Image(storyTexture);


        // Лейбл для тексту
        Skin skin = new Skin(Gdx.files.internal("ui/uiskin.json"));
        BitmapFont font = new BitmapFont(Gdx.files.internal("fonts/8bold.fnt"));
        font.getData().lineHeight *= 1.5f;
        font.setColor(Color.BLACK);

        storyLabel = new Label("", skin);
        storyLabel.setWrap(true);
        storyLabel.setAlignment(Align.topLeft);

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
        background.setSize(screenWidth, screenHeight);
        resizeCentered(storyImage, storyTexture, screenWidth, screenHeight);

        float btnWidth = storyImage.getWidth() * 0.55f;
        float btnHeight = storyImage.getHeight() * 0.17f;
        float paddingBottom = storyImage.getHeight() * 0.07f;

        storyLabel.setWidth(storyImage.getWidth() * 0.8f);
        storyLabel.setPosition(
                storyImage.getX() + storyImage.getWidth() * 0.1f,
                storyImage.getY() + storyImage.getHeight() * 0.6f
        );

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
