package com.gdx.game.ui.popup;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.gdx.game.DetectiveGame;
import com.gdx.game.utils.NotePages;

public class NotePopup {
    private final Texture noteTexture;
    private final Image noteImage;
    private final Image background;
    private final Stage stage;
    private final Skin skin;
    private final NotePages pages;

    private final Image btnNext;
    private final Image btnPrev;

    public NotePopup(Stage stage, Skin skin, DetectiveGame game) {
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

        noteTexture = new Texture("menu/note/notes.png");
        noteImage = new Image(noteTexture);
        noteImage.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                event.stop();
            }
        });

        btnPrev = game.getButtonFactory().createButton(
                "menu/note/arrow_left.png", 60, 60,
                pages::prevPage
        );

        btnNext = game.getButtonFactory().createButton(
                "menu/note/arrow_right.png", 60, 60,
                pages::nextPage
        );

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

        float btnSize = 60;
        btnPrev.setPosition(noteImage.getX()-25, noteImage.getY() + height / 2 - btnSize / 2);

        btnNext.setPosition(noteImage.getX() + width - 40, noteImage.getY() + height / 2 - btnSize / 2);
    }

    public void show() {
        stage.addActor(background);
        stage.addActor(noteImage);
        pages.show();
        stage.addActor(btnPrev);
        stage.addActor(btnNext);
    }

    public void remove() {
        noteImage.remove();
        background.remove();
        pages.remove();
        btnPrev.remove();
        btnNext.remove();
    }

    public void dispose() {
        noteTexture.dispose();
        skin.dispose();
    }
}
