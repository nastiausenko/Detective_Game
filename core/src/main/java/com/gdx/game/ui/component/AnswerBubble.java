package com.gdx.game.ui.component;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.Align;
import com.gdx.game.infrastructure.Assets;
import com.gdx.game.ui.style.UiLayout;
import com.gdx.game.ui.style.UiLayoutProfile;
import com.gdx.game.ui.style.UiStyles;

public class AnswerBubble {
    private static final float THINKING_FRAME_SECONDS = 0.32f;
    private static final float TYPEWRITER_CHAR_SECONDS = 0.025f;

    private final Texture texture;
    private final Image background;
    private final Label label;
    private final GlyphLayout glyphLayout = new GlyphLayout();

    private String text = "";
    private boolean thinking = false;
    private float thinkingElapsed = 0f;
    private int thinkingFrame = 0;
    private boolean typing = false;
    private float typingElapsed = 0f;
    private int typedChars = 0;

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
        thinking = false;
        typing = false;
        this.text = text != null ? text : "";
        label.setText(this.text);
        label.setVisible(true);
        background.setVisible(true);
    }

    public void showTypewriterText(String text) {
        thinking = false;
        typing = true;
        typingElapsed = 0f;
        typedChars = 0;
        this.text = text != null ? text : "";
        label.setText("");
        label.setVisible(true);
        background.setVisible(true);
    }

    public void showThinking() {
        thinking = true;
        typing = false;
        thinkingElapsed = 0f;
        thinkingFrame = 0;
        this.text = "...";
        label.setText(".");
        label.setVisible(true);
        background.setVisible(true);
    }

    public void update(float delta) {
        if (typing) {
            updateTypewriter(delta);
            return;
        }

        if (!thinking) return;

        thinkingElapsed += delta;
        if (thinkingElapsed < THINKING_FRAME_SECONDS) return;

        thinkingElapsed = 0f;
        if (thinkingFrame == 0) {
            label.setText(".");
        } else if (thinkingFrame == 1) {
            label.setText("..");
        } else {
            label.setText("...");
        }
        thinkingFrame = (thinkingFrame + 1) % 3;
    }

    private void updateTypewriter(float delta) {
        if (text.isEmpty()) {
            typing = false;
            label.setText("");
            return;
        }

        typingElapsed += delta;
        while (typingElapsed >= TYPEWRITER_CHAR_SECONDS && typedChars < text.length()) {
            typingElapsed -= TYPEWRITER_CHAR_SECONDS;
            typedChars++;
        }

        label.setText(text.substring(0, typedChars));
        if (typedChars >= text.length()) {
            typing = false;
        }
    }

    public boolean isTyping() {
        return typing;
    }

    public void finishTyping() {
        if (!typing) return;
        typedChars = text.length();
        typing = false;
        label.setText(text);
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
        label.setAlignment(thinking ? Align.left : Align.center);
        label.setFontScale(profile.getBubbleFontScale());
        label.setWidth(innerWidth);
        String visibleText = label.getText().toString();
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
        label.setText(visibleText);

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
