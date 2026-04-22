package com.gdx.game.utils;

import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.utils.viewport.Viewport;

public class ScreenUtilsHelper {

    public static float[] calculateDrawSize(float textureWidth, float textureHeight, float worldWidth, float worldHeight) {
        float scaleX = worldWidth / textureWidth;
        float scaleY = worldHeight / textureHeight;
        float baseScale = Math.max(1f, Math.max(scaleX, scaleY));
        return new float[]{textureWidth * baseScale, textureHeight * baseScale};
    }

    public static void scaleButton(Image button, float targetHeight, Stage stage) {
        float aspect = button.getDrawable().getMinWidth() / button.getDrawable().getMinHeight();
        float screenAspect = stage.getViewport().getWorldWidth() / stage.getViewport().getWorldHeight();

        float widthScaleFactor = Math.min(1f, screenAspect / 1.1f);
        float adjustedHeight = targetHeight * widthScaleFactor;

        button.setSize(adjustedHeight * aspect, adjustedHeight);
    }

    public static void scaleNavButton(Image button, Image targetBackground) {
        float btnWidth = targetBackground.getWidth() * 0.5f;
        float btnHeight = targetBackground.getHeight() * 0.1f;
        float paddingBottom = targetBackground.getHeight() * 0.1f;

        button.setSize(btnWidth, btnHeight);
        button.setPosition(
            targetBackground.getX() + (targetBackground.getWidth() - btnWidth) / 2f,
            targetBackground.getY() + paddingBottom
        );
    }

    public static float centerX(Image button, Viewport viewport) {
        return (viewport.getWorldWidth() - button.getWidth()) / 2f;
    }
}
