package com.gdx.game.shared.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;

public final class UiStyles {
    private UiStyles() {
    }

    public static Color ink() {
        return Color.BLACK;
    }

    public static Color parchmentText() {
        return new Color(154 / 255f, 109 / 255f, 69 / 255f, 1f);
    }

    public static Color lastQuestionText() {
        return new Color(0.82f, 0.69f, 0.47f, 1f);
    }

    public static Color counterText() {
        return new Color(0.32f, 0.18f, 0.08f, 0.85f);
    }

    public static Color inputFrameCounterText() {
        return new Color(0.86f, 0.61f, 0.24f, 0.95f);
    }

    public static Color textSelection() {
        return new Color(0.3f, 0.5f, 1f, 0.5f);
    }

    public static Label.LabelStyle label(Skin skin, Color color) {
        return new Label.LabelStyle(skin.getFont("default-font"), color);
    }

    public static TextField.TextFieldStyle transparentTextField(Skin skin) {
        TextField.TextFieldStyle style = new TextField.TextFieldStyle();
        style.font = skin.getFont("default-font");
        style.fontColor = ink();
        style.messageFontColor = ink();
        style.cursor = skin.newDrawable("cursor", ink());
        style.background = null;
        style.selection = skin.newDrawable("white", textSelection());
        return style;
    }
}
