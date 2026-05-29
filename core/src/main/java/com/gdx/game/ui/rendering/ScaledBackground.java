package com.gdx.game.ui.rendering;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.gdx.game.utils.TiledTextureHelper;

public class ScaledBackground {
    private static final int IOS_TILE_SIZE = 256;

    private final Texture texture;
    private final TiledTextureHelper tiledTextureHelper;
    private final boolean tileOnIos;
    private final boolean keepOriginalSizeAsMinimum;

    private float drawWidth;
    private float drawHeight;

    public ScaledBackground(String texturePath) {
        this(texturePath, false, false);
    }

    public ScaledBackground(String texturePath, boolean tileOnIos, boolean keepOriginalSizeAsMinimum) {
        texture = new Texture(texturePath);
        this.tileOnIos = tileOnIos;
        this.keepOriginalSizeAsMinimum = keepOriginalSizeAsMinimum;
        tiledTextureHelper = tileOnIos ? new TiledTextureHelper(texture, IOS_TILE_SIZE) : null;
    }

    public void resizeToCover(float width, float height) {
        float scaleX = width / texture.getWidth();
        float scaleY = height / texture.getHeight();
        float scale = Math.max(scaleX, scaleY);
        if (keepOriginalSizeAsMinimum) {
            scale = Math.max(1f, scale);
        }

        drawWidth = texture.getWidth() * scale;
        drawHeight = texture.getHeight() * scale;
    }

    public void render(SpriteBatch batch) {
        if (tileOnIos && Gdx.app.getType() == Application.ApplicationType.iOS) {
            tiledTextureHelper.renderTiled(batch, drawWidth, drawHeight);
            return;
        }

        batch.begin();
        batch.draw(texture, 0, 0, drawWidth, drawHeight);
        batch.end();
    }

    public Texture getTexture() {
        return texture;
    }

    public float getTextureWidth() {
        return texture.getWidth();
    }

    public float getTextureHeight() {
        return texture.getHeight();
    }

    public float getDrawWidth() {
        return drawWidth;
    }

    public float getDrawHeight() {
        return drawHeight;
    }

    public void dispose() {
        texture.dispose();
    }
}
