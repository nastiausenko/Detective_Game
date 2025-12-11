package com.gdx.game.infra.resources;

import com.badlogic.gdx.graphics.Texture;

public interface ResourceProvider {
    Texture getTexture(String path);
}
