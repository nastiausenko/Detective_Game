package com.gdx.game.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextArea;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.graphics.Texture;

public class NotePopup {
    private final Texture noteTexture;
    private final Image noteImage;
    private final Image background;
    private final Stage stage;
    private final Skin skin;
    private TextArea textArea;
    private String savedText = "";

    public NotePopup(Stage stage, Skin skin, String texturePath) {
        this.stage = stage;
        this.skin = skin;

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

        createTextArea();
        resize(stage.getViewport().getWorldWidth(), stage.getViewport().getWorldHeight());
    }

    private void createTextArea() {
        BitmapFont font = new BitmapFont(Gdx.files.internal("fonts/8bold.fnt"));

        TextField.TextFieldStyle style = new TextField.TextFieldStyle();
        style.font = font;
        style.fontColor = Color.BLACK;
        style.background = null;
        style.cursor = skin.newDrawable("cursor", Color.BLACK);
        style.selection = skin.newDrawable("selection", new Color(0, 0, 1, 0.3f));

        textArea = new TextArea(savedText, style);
        textArea.setFocusTraversal(false);

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

        float paddingLeft = width * 0.29f;
        float paddingRight = width * 0.17f;
        float paddingTop = height * 0.23f;
        float paddingBottom = height * 0.17f;

        textArea.setSize(width - paddingLeft - paddingRight, height - paddingTop - paddingBottom);
        textArea.setPosition(noteImage.getX() + paddingLeft, noteImage.getY() + paddingBottom);

        float fontScale = (textArea.getHeight() / 300f);
        textArea.getStyle().font.getData().setScale(fontScale);
    }

    public void show() {
        stage.addActor(background);
        stage.addActor(noteImage);
        stage.addActor(textArea);
        stage.setKeyboardFocus(textArea);
    }

    public void remove() {
        savedText = textArea.getText();

        textArea.remove();
        noteImage.remove();
        background.remove();
    }

    public void dispose() {
        noteTexture.dispose();
        skin.dispose();
    }
}
