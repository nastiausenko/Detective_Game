package com.gdx.game.render;

import com.badlogic.gdx.graphics.g2d.NinePatch;
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable;

public class NoMinNinePatchDrawable extends NinePatchDrawable {
    public NoMinNinePatchDrawable(NinePatch patch) {
        super(patch);
    }

    @Override
    public float getMinHeight() {
        return 0f;
    }

    @Override
    public float getMinWidth() {
        return 0f;
    }
}
