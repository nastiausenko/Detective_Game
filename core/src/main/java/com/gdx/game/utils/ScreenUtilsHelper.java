package com.gdx.game.utils;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.utils.viewport.Viewport;

public class ScreenUtilsHelper {

    public static float[] calculateDrawSize(float textureWidth, float textureHeight, float worldWidth, float worldHeight) {
        float scaleX = worldWidth / textureWidth;
        float scaleY = worldHeight / textureHeight;
        float baseScale = Math.max(1f, Math.max(scaleX, scaleY));
        return new float[]{textureWidth * baseScale, textureHeight * baseScale};
    }

    public static void scaleAndPositionButton(Image button, float targetHeight, float posX, float posY) {
        float aspect = button.getDrawable().getMinWidth() / button.getDrawable().getMinHeight();
        float screenAspect = (float) Gdx.graphics.getWidth() / Gdx.graphics.getHeight();

        float widthScaleFactor = Math.min(1f, screenAspect / 1.1f);
        float adjustedHeight = targetHeight * widthScaleFactor;

        button.setSize(adjustedHeight * aspect, adjustedHeight);
        button.setPosition(posX, posY);
    }

    public static float centerX(Image button, Viewport viewport) {
        return (viewport.getWorldWidth() - button.getWidth()) / 2f;
    }
}
