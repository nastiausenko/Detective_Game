package com.gdx.game.ui;

import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.ui.Image;

public class BackgroundFactory {
    private static Texture whiteTexture;

    public static Image createDimBackground(float width, float height, float alpha) {
        if (whiteTexture == null) {
            Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
            pixmap.setColor(1, 1, 1, 1);
            pixmap.fill();
            whiteTexture = new Texture(pixmap);
            pixmap.dispose();
        }
        Image bg = new Image(whiteTexture);
        bg.setSize(width, height);
        bg.setColor(0, 0, 0, alpha);
        return bg;
    }

    public static void dispose() {
        if (whiteTexture != null) {
            whiteTexture.dispose();
            whiteTexture = null;
        }
    }
}
