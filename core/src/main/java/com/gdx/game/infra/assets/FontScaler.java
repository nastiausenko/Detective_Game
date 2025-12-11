package com.gdx.game.infra.assets;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.math.MathUtils;

public class FontScaler {

    private static float lastComputedScale = 1f;
    private static float smoothScale = 1f;

    public static float computeGlobalScale() {
        float worldWidth = Gdx.graphics.getWidth();
        float worldHeight = Gdx.graphics.getHeight();
        boolean isMobile = Gdx.app.getType() == Application.ApplicationType.iOS
            || Gdx.app.getType() == Application.ApplicationType.Android;

        float targetScale;

        if (worldHeight > worldWidth) {
            float base = Math.min(worldHeight / 800f, worldWidth / 800f);
            if (isMobile) {
                targetScale = base * Gdx.graphics.getDensity() * 0.5f;
            } else {
                targetScale = base;
            }
        } else {
            if (isMobile) {
                targetScale = (worldHeight / 1100f) * Gdx.graphics.getDensity() * 0.5f;
            } else {
                targetScale = worldHeight / 800f;
            }
        }

        smoothScale = MathUtils.lerp(smoothScale, targetScale, 0.1f);

        lastComputedScale = smoothScale;
        return smoothScale;
    }

    public static void applyScale(BitmapFont font) {
        font.getData().setScale(computeGlobalScale());
    }

    public static void applyLastScale(BitmapFont font) {
        font.getData().setScale(lastComputedScale);
    }

    public static float getLastScale() {
        return lastComputedScale;
    }
}
