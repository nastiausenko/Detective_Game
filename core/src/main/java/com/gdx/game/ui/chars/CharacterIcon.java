package com.gdx.game.ui.chars;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.ui.Image;

public class CharacterIcon extends Image {
    private final float mapX;
    private final float mapY;
    private final float baseSize;

    public CharacterIcon(String iconPath, float mapX, float mapY, float size) {
        super(new Texture(iconPath));
        this.mapX = mapX;
        this.mapY = mapY;
        this.baseSize = size;

        setSize(size, size * 1.4f);
    }

    public void updatePosition(float drawWidth, float drawHeight, float originalWidth, float originalHeight) {
        float scaleX = drawWidth / originalWidth;
        float scaleY = drawHeight / originalHeight;

        float newX = mapX * scaleX;
        float newY = mapY * scaleY;

        float scaledWidth = baseSize * scaleX;
        float scaledHeight = baseSize * 1.4f * scaleY;

        setSize(scaledWidth, scaledHeight);
        setPosition(newX - scaledWidth / 2f, newY - scaledHeight / 2f);
    }
}
