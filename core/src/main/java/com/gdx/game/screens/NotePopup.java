package com.gdx.game.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.graphics.Texture;
import com.gdx.game.utils.NotePages;

public class NotePopup {
    private final Texture noteTexture;
    private final Image noteImage;
    private final Image background;
    private final Stage stage;
    private final Skin skin;
    private final NotePages pages;

    public NotePopup(Stage stage, Skin skin, String texturePath) {
        this.stage = stage;
        this.skin = skin;
        this.pages = new NotePages(stage, skin);

        background = new Image(new Texture("background.png"));
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
        noteImage.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                event.stop();
            }
        });

        resize(stage.getViewport().getWorldWidth(), stage.getViewport().getWorldHeight());
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

        float paddingTop = height * 0.13f;
        float paddingBottom = height * 0.13f;

        float columnHeight = height - paddingTop - paddingBottom;

        float outerPadding = width * 0.13f;
        float innerPadding = width * 0.05f;

        float halfWidth = width / 2f;
        float columnWidth = halfWidth - outerPadding - innerPadding;

        pages.setColumnWidth(columnWidth);
        pages.setPosition(noteImage.getX() + outerPadding, noteImage.getY() + paddingBottom, columnHeight);
    }

    public void show() {
        stage.addActor(background);
        stage.addActor(noteImage);
        pages.show();
    }

    public void remove() {
        noteImage.remove();
        background.remove();
        pages.remove();
    }

    public void dispose() {
        noteTexture.dispose();
        skin.dispose();
    }
}
