package com.gdx.game.widgets.popup;

import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.math.MathUtils;

public final class PopupTextScaler {
    private PopupTextScaler() {
    }

    public static void scaleToFit(
        Label label,
        GlyphLayout layout,
        String text,
        float labelWidth,
        float availableHeight,
        int alignment
    ) {
        Label.LabelStyle style = label.getStyle();
        BitmapFont font = style.font;

        font.getData().setScale(1f);
        layout.setText(font, text != null && !text.isEmpty() ? text : "Т",
            style.fontColor, labelWidth, alignment, true);

        float prefHeight = layout.height;
        if (prefHeight <= 0f) {
            prefHeight = font.getCapHeight();
        }

        float scale = MathUtils.clamp(availableHeight / prefHeight, 0.6f, 1.4f);
        font.getData().setScale(scale);
        label.invalidateHierarchy();
    }

    public static void scaleToFitCentered(Label label, GlyphLayout layout, String text,
                                          float labelWidth, float availableHeight) {
        scaleToFit(label, layout, text, labelWidth, availableHeight, Align.center);
    }

    public static void scaleToFitLeft(Label label, GlyphLayout layout, String text,
                                      float labelWidth, float availableHeight) {
        scaleToFit(label, layout, text, labelWidth, availableHeight, Align.left);
    }
}
