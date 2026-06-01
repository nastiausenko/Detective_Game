package com.gdx.game.render;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

public class TiledTextureHelper {

    private final Texture texture;
    private final int tileSize;
    private final TextureRegion[][] tiles;

    public TiledTextureHelper(Texture texture, int tileSize) {
        this.texture = texture;
        this.tileSize = tileSize;
        this.tiles = splitTexture(texture, tileSize);
    }

    private TextureRegion[][] splitTexture(Texture texture, int tileSize) {
        int texWidth = texture.getWidth();
        int texHeight = texture.getHeight();

        int cols = (int) Math.ceil((float) texWidth / tileSize);
        int rows = (int) Math.ceil((float) texHeight / tileSize);

        TextureRegion[][] regions = new TextureRegion[rows][cols];

        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                int x = col * tileSize;
                int y = row * tileSize;
                int w = Math.min(tileSize, texWidth - x);
                int h = Math.min(tileSize, texHeight - y);
                regions[row][col] = new TextureRegion(texture, x, y, w, h);
            }
        }
        return regions;
    }

    public void renderTiled(SpriteBatch batch, float drawWidth, float drawHeight) {
        int rows = tiles.length;
        int cols = tiles[0].length;

        float scaleX = drawWidth / texture.getWidth();
        float scaleY = drawHeight / texture.getHeight();
        float scale = Math.min(scaleX, scaleY);

        float offsetX = (drawWidth - texture.getWidth() * scale) / 2f;
        float offsetY = (drawHeight - texture.getHeight() * scale) / 2f;

        batch.begin();
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                TextureRegion region = tiles[row][col];
                float x = offsetX + col * tileSize * scale;
                float y = offsetY + (rows - 1 - row) * tileSize * scale;
                float w = region.getRegionWidth() * scale;
                float h = region.getRegionHeight() * scale;
                batch.draw(region, x, y, w, h);
            }
        }
        batch.end();
    }

    public Texture getTexture() {
        return texture;
    }

    public void dispose() {
        if (texture != null) texture.dispose();
    }
}
