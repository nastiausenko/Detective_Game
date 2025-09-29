package com.gdx.game.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.InputEvent;

public class NotePopup {
    private final Texture noteTexture;
    private final Image noteImage;
    private final Image background;
    private final Stage stage;

    public NotePopup(Stage stage, String texturePath) {
        this.stage = stage;

        background = new Image(new Texture(Gdx.files.internal("background.png")));
        background.setColor(0, 0, 0, 0.5f);
        background.setSize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        background.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                remove();
            }
        });

        noteTexture = new Texture(texturePath);
        noteImage = new Image(noteTexture);
        resize(stage.getViewport().getWorldWidth(), stage.getViewport().getWorldHeight());

        noteImage.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                event.stop();
            }
        });
    }

    public void resize(float screenWidth, float screenHeight) {
        background.setSize(screenWidth, screenHeight);

        float maxWidth = screenWidth * 0.9f;
        float maxHeight = screenHeight * 0.9f;
        float aspect = noteTexture.getWidth() / (float) noteTexture.getHeight();

        float width = noteTexture.getWidth();
        float height = noteTexture.getHeight();

        if (width > maxWidth) {
            width = maxWidth;
            height = width / aspect;
        }
        if (height > maxHeight) {
            height = maxHeight;
            width = height * aspect;
        }

        noteImage.setSize(width, height);

        noteImage.setPosition((screenWidth - width) / 2f, (screenHeight - height) / 2f);
    }

    public void show() {
        stage.addActor(background);
        stage.addActor(noteImage);
    }

    public void remove() {
        noteImage.remove();
        background.remove();
        noteTexture.dispose();
    }
}
