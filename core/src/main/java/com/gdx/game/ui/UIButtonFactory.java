package com.gdx.game.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Cursor;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

public class UIButtonFactory {
    private final ResourceProvider resourceProvider;

    public UIButtonFactory(ResourceProvider resourceProvider) {
        this.resourceProvider = resourceProvider;
    }

    public Image createButton(String path, float width, float height, Runnable onClick) {
        Image button = new Image(resourceProvider.getTexture(path));
        button.setSize(width, height);
        button.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                onClick.run();
            }

            @Override
            public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                Gdx.graphics.setSystemCursor(Cursor.SystemCursor.Hand);
            }

            @Override
            public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
                Gdx.graphics.setSystemCursor(Cursor.SystemCursor.Arrow);
            }
        });
        return button;
    }
}
