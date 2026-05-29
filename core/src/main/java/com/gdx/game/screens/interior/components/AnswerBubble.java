package com.gdx.game.screens.interior.components;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.Align;
import com.gdx.game.shared.config.Assets;
import com.gdx.game.shared.config.UiLayout;
import com.gdx.game.shared.config.UiLayoutProfile;
import com.gdx.game.shared.ui.UiStyles;

public class AnswerBubble {
    private final Texture texture;
    private final Image background;
    private final Label label;
    private final GlyphLayout glyphLayout = new GlyphLayout();

    private String text = "";

    public AnswerBubble(Stage stage, Skin skin) {
        texture = new Texture(Assets.ANSWER_AREA);
        background = new Image(texture);
        background.setVisible(false);

        label = new Label("", skin);
        label.setWrap(true);
        label.setColor(UiStyles.ink());
        label.setVisible(false);

        stage.addActor(background);
        stage.addActor(label);
    }

    public void showText(String text) {
        this.text = text != null ? text : "";
        label.setText(this.text);
        label.setVisible(true);
        background.setVisible(true);
    }

    public void setVisible(boolean visible) {
        label.setVisible(visible);
        background.setVisible(visible);
    }

    public void layout(Image characterImage, float screenWidth, float screenHeight) {
        if (!label.isVisible()) return;

        UiLayoutProfile profile = UiLayout.current(screenWidth, screenHeight);
        float paddingX = profile.scale(40f);
        float paddingY = profile.scale(16f);
        float tailHeight = profile.scale(21f);

        float anchorRight = characterImage.getX() + characterImage.getWidth() * 0.5f - paddingX;
        float maxBubbleWidth = texture.getWidth() * 0.4f;
        float screenLimit = screenWidth * (screenHeight > screenWidth
            ? profile.getBubbleWidthPortraitRatio()
            : profile.getBubbleWidthLandscapeRatio());
        maxBubbleWidth = Math.min(Math.min(maxBubbleWidth, screenLimit), anchorRight - paddingX);

        Label.LabelStyle style = label.getStyle();
        glyphLayout.setText(style.font, text);

        float maxInnerWidth = Math.max(0f, maxBubbleWidth - paddingX * 2f);
        float innerWidth = Math.min(glyphLayout.width, maxInnerWidth);
        float maxBubbleHeight = screenHeight * 0.45f;
        float maxInnerHeight = maxBubbleHeight - paddingY * 2f - tailHeight;

        label.setWrap(true);
        label.setAlignment(Align.center);
        label.setFontScale(profile.getBubbleFontScale());
        label.setWidth(innerWidth);
        label.setText(text);
        label.layout();
        float textHeight = label.getPrefHeight();

        if (textHeight > maxInnerHeight && textHeight > 0f) {
            float scale = MathUtils.clamp(maxInnerHeight / textHeight, 0.5f, 1f);
            label.setFontScale(scale);
            label.invalidateHierarchy();
            label.layout();
            textHeight = label.getPrefHeight();
        }

        float bubbleWidth = innerWidth + paddingX * 2f;
        float bubbleHeight = textHeight + paddingY * 2f + tailHeight;
        float bubbleX = anchorRight - bubbleWidth;
        float bubbleY = MathUtils.clamp(
            characterImage.getY() + characterImage.getHeight() * 0.6f,
            0,
            screenHeight - bubbleHeight
        );

        background.setBounds(bubbleX, bubbleY, bubbleWidth, bubbleHeight);

        float innerHeightActual = bubbleHeight - paddingY * 2f - tailHeight;
        label.setBounds(
            bubbleX + paddingX,
            bubbleY + tailHeight + paddingY + (innerHeightActual - textHeight) / 2f,
            innerWidth,
            textHeight
        );
    }

    public void dispose() {
        texture.dispose();
    }
}
