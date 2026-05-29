package com.gdx.game.screens.interior.components;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.NinePatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable;
import com.badlogic.gdx.utils.Align;
import com.gdx.game.shared.config.Assets;
import com.gdx.game.app.model.GameContext;
import com.gdx.game.shared.config.UiLayoutProfile;
import com.gdx.game.shared.ui.UiStyles;

public class DialogueInputPanel {
    public interface SubmitListener {
        void onSubmit(String question);
    }

    private static final String DEFAULT_PLACEHOLDER = "Запитайте персонажа...";
    private static final int PATCH_LEFT = 52;
    private static final int PATCH_RIGHT = 138;
    private static final int PATCH_TOP = 44;
    private static final int PATCH_BOTTOM = 55;
    private static final float INPUT_VERTICAL_PADDING_FONT_RATIO = 0.65f;
    private static final float LAST_QUESTION_FRAME_BOTTOM_FROM_TOP = 12f;

    private final Stage stage;
    private final Skin skin;
    private final GameContext game;
    private final Texture backgroundTexture;
    private final Image backgroundImage;
    private final Image sendButton;
    private final TextField inputField;
    private final Label lastQuestionLabel;
    private final Label charCounterLabel;
    private final SubmitListener submitListener;

    private String lastQuestion = "";
    private boolean waiting;
    private int lastLayoutWidth;
    private int lastLayoutHeight;
    private UiLayoutProfile lastLayoutProfile;

    public DialogueInputPanel(Stage stage, Skin skin, GameContext game, SubmitListener submitListener) {
        this.stage = stage;
        this.skin = skin;
        this.game = game;

        backgroundTexture = new Texture(Assets.QUESTION_AREA);
        backgroundImage = new Image(createBackgroundDrawable());
        sendButton = game.getButtonFactory().createButton(Assets.SEND_BUTTON, 40, 40, this::submit);

        lastQuestionLabel = new Label("", skin);
        lastQuestionLabel.setWrap(false);
        lastQuestionLabel.setEllipsis(true);
        lastQuestionLabel.setAlignment(Align.left);
        lastQuestionLabel.setColor(UiStyles.lastQuestionText());
        lastQuestionLabel.setVisible(false);

        charCounterLabel = new Label("", skin);
        charCounterLabel.setAlignment(Align.right);
        charCounterLabel.setColor(UiStyles.lastQuestionText());

        inputField = new TextField("", UiStyles.transparentTextField(skin));
        inputField.setMessageText(DEFAULT_PLACEHOLDER);
        inputField.setTextFieldFilter((textField, c) ->
            c == '\n' || c == '\r' || inputField.getText().length() < Assets.MAX_CHARS_INPUT
        );
        inputField.setTextFieldListener((textField, c) -> {
            if (c == '\n' || c == '\r') {
                submit();
                return;
            }
            inputField.setMessageText(DEFAULT_PLACEHOLDER);
            updateCharCounter();
        });

        this.submitListener = submitListener;

        stage.addActor(backgroundImage);
        stage.addActor(sendButton);
        stage.addActor(lastQuestionLabel);
        stage.addActor(inputField);
        stage.addActor(charCounterLabel);
        updateCharCounter();
    }

    private void submit() {
        if (waiting) return;

        String text = inputField.getText();
        if (text == null || text.trim().isEmpty()) {
            inputField.setMessageText("Введіть питання");
            inputField.setText("");
            updateCharCounter();
            return;
        }

        inputField.setMessageText(DEFAULT_PLACEHOLDER);
        submitListener.onSubmit(text.trim());
    }

    public boolean ownsTarget(Actor target) {
        return target != null && (
            target == inputField
                || target.isDescendantOf(inputField)
                || target == sendButton
                || target.isDescendantOf(sendButton)
        );
    }

    public void showQuestion(String question) {
        lastQuestion = question != null ? question : "";
        lastQuestionLabel.setText(lastQuestion);
        lastQuestionLabel.setVisible(!lastQuestion.isEmpty());
        inputField.setText("");
        updateCharCounter();
        relayoutIfReady();
    }

    public void setWaiting(boolean waiting) {
        this.waiting = waiting;
        inputField.setDisabled(waiting);
        sendButton.setTouchable(waiting || !sendButton.isVisible() ? Touchable.disabled : Touchable.enabled);
    }

    public void setVisible(boolean visible) {
        backgroundImage.setVisible(visible);
        sendButton.setVisible(visible);
        sendButton.setTouchable(visible && !waiting ? Touchable.enabled : Touchable.disabled);
        inputField.setVisible(visible);
        inputField.setDisabled(!visible || waiting);
        lastQuestionLabel.setVisible(visible && !lastQuestion.isEmpty());
        charCounterLabel.setVisible(visible);
    }

    public void layout(int width, int height, UiLayoutProfile profile) {
        lastLayoutWidth = width;
        lastLayoutHeight = height;
        lastLayoutProfile = profile;

        float panelWidth = width * profile.getQuestionAreaWidthRatio();
        float maxPanelHeight = height * profile.getQuestionAreaHeightRatio();

        float paddingX = Math.max(profile.scale(18f), panelWidth * 0.05f);
        float gapToSend = profile.scale(8f);

        TextField.TextFieldStyle inputStyle = UiStyles.transparentTextField(skin);
        inputField.setStyle(inputStyle);
        inputField.setAlignment(Align.left);

        float fontHeight = Math.max(
            inputStyle.font.getLineHeight(),
            inputStyle.font.getCapHeight()
        );
        float desiredVerticalPadding = Math.max(
            profile.scale(12f),
            fontHeight * INPUT_VERTICAL_PADDING_FONT_RATIO
        );
        float minimumPatchHeight = PATCH_TOP + PATCH_BOTTOM;
        float preferredPanelHeight = Math.max(
            minimumPatchHeight,
            fontHeight + desiredVerticalPadding * 2f
        );
        float panelHeight = Math.min(maxPanelHeight, preferredPanelHeight);
        float verticalPadding = Math.max(0f, (panelHeight - fontHeight) / 2f);

        float panelX = (width - panelWidth) / 2f;
        float panelY = height * profile.getQuestionAreaBottomMarginRatio();

        backgroundImage.setBounds(panelX, panelY, panelWidth, panelHeight);

        charCounterLabel.setFontScale(profile.getBubbleFontScale() * 0.78f);
        charCounterLabel.invalidateHierarchy();

        float counterHeight = charCounterLabel.getPrefHeight();
        float counterWidth = charCounterLabel.getPrefWidth();

        lastQuestionLabel.setFontScale(profile.getBubbleFontScale() * 0.78f);
        lastQuestionLabel.setWrap(false);
        lastQuestionLabel.setEllipsis(true);

        float frameInset = Math.min(
            calculateSendButtonFrameInset(),
            Math.max(0f, (panelHeight - 1f) / 2f)
        );
        float sendSize = calculateSendButtonSize(panelHeight, frameInset);
        float sendX = panelX + panelWidth - frameInset - sendSize - 4f;
        float sendY = panelY + frameInset + 1f;

        sendButton.setBounds(sendX, sendY, sendSize, sendSize);

        float contentX = panelX + paddingX;
        float contentWidth = sendX - gapToSend - contentX;

        lastQuestionLabel.setWidth(contentWidth);
        lastQuestionLabel.invalidateHierarchy();

        float questionHeight = lastQuestionLabel.isVisible()
            ? lastQuestionLabel.getPrefHeight()
            : 0f;

        float counterX = contentX + contentWidth - counterWidth;
        float counterFrameHeight = Math.max(frameInset, counterHeight);
        float counterY = panelY + (counterFrameHeight - counterHeight) / 2f - 2f;

        charCounterLabel.setBounds(counterX, counterY, counterWidth, counterHeight);

        if (lastQuestionLabel.isVisible()) {
            float questionY = panelY + panelHeight - LAST_QUESTION_FRAME_BOTTOM_FROM_TOP;

            lastQuestionLabel.setBounds(
                contentX,
                questionY,
                contentWidth,
                questionHeight
            );
        } else {
            lastQuestionLabel.setBounds(
                contentX,
                panelY + panelHeight - LAST_QUESTION_FRAME_BOTTOM_FROM_TOP,
                contentWidth,
                0f
            );
        }

        float inputHeight = Math.max(1f, panelHeight - verticalPadding * 2f);

        inputField.setBounds(
            contentX,
            panelY + verticalPadding + 2f,
            contentWidth,
            inputHeight
        );
    }

    public NinePatchDrawable createBackgroundDrawable() {
        TextureRegion artRegion = new TextureRegion(
            backgroundTexture,
            1,
            1,
            backgroundTexture.getWidth() - 2,
            backgroundTexture.getHeight() - 2
        );
        return new NinePatchDrawable(new NinePatch(
            artRegion,
            PATCH_LEFT,
            PATCH_RIGHT,
            PATCH_TOP,
            PATCH_BOTTOM
        ));
    }

    private float calculateSendButtonFrameInset() {
        float buttonArtHeight = sendButton.getDrawable() != null
            ? sendButton.getDrawable().getMinHeight()
            : 0f;

        if (buttonArtHeight <= 0f) {
            return 20f;
        }

        return Math.max(0f, (backgroundTexture.getHeight() - buttonArtHeight) / 2f);
    }

    private float calculateSendButtonSize(float panelHeight, float frameInset) {
        float innerHeight = panelHeight - frameInset * 2f;
        if (innerHeight > 0f) {
            return innerHeight;
        }

        return Math.max(1f, panelHeight);
    }

    private void updateCharCounter() {
        int length = inputField.getText() != null ? inputField.getText().length() : 0;
        charCounterLabel.setText(length + "/" + Assets.MAX_CHARS_INPUT);
        relayoutIfReady();
    }

    private void relayoutIfReady() {
        if (lastLayoutProfile == null || lastLayoutWidth <= 0 || lastLayoutHeight <= 0) {
            return;
        }

        layout(lastLayoutWidth, lastLayoutHeight, lastLayoutProfile);
    }

    public void dispose() {
        backgroundTexture.dispose();
    }
}
