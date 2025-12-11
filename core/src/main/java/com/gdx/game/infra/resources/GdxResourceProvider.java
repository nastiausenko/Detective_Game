package com.gdx.game.infra.resources;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;

public class GdxResourceProvider implements ResourceProvider {
    @Override
    public Texture getTexture(String path) {
        return new Texture(Gdx.files.internal(path));
    }
}
