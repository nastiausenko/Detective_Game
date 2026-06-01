package com.gdx.game.ui.style;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.math.MathUtils;
import com.gdx.game.ui.style.UiLayout;
import com.gdx.game.ui.style.UiLayoutProfile;

public class FontScaler {

    private static float lastComputedScale = 1f;
    private static float smoothScale = 1f;

    public static float computeGlobalScale() {
        float worldWidth = Gdx.graphics.getWidth();
        float worldHeight = Gdx.graphics.getHeight();
        UiLayoutProfile profile = UiLayout.current(worldWidth, worldHeight);
        boolean isMobile = profile.isTouchDevice();

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

        targetScale *= profile.getFontScaleMultiplier();

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
