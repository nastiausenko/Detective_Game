package com.gdx.game.shared.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Cursor;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.gdx.game.shared.audio.AudioManager;

public class UIButtonFactory {
    private final AudioManager audioManager;

    public UIButtonFactory(AudioManager audioManager) {
        this.audioManager = audioManager;
    }

    public Image createButton(String path, float width, float height, Runnable onClick) {
        return createButton(path, width, height, onClick, true);
    }

    public Image createButton(String path, float width, float height, Runnable onClick, boolean playClickSound) {
        Image button = new Image(new Texture(Gdx.files.internal(path)));
        button.setSize(width, height);
        button.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (playClickSound && audioManager != null) {
                    audioManager.playButtonClick();
                }
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
