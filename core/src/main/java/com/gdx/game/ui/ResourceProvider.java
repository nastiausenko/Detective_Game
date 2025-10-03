package com.gdx.game.ui;

import com.badlogic.gdx.graphics.Texture;

public interface ResourceProvider {
    Texture getTexture(String path);
}
