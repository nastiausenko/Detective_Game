package com.gdx.game.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputListener;
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
    private TextArea textArea1;
    private TextArea textArea2;
    private String savedText = "";
    private float columnWidth;

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

        createTextAreas();
        resize(stage.getViewport().getWorldWidth(), stage.getViewport().getWorldHeight());
    }

    private void createTextAreas() {
        BitmapFont font = new BitmapFont(Gdx.files.internal("fonts/8bold.fnt"));
        font.getData().lineHeight *= 1.5f;

        TextField.TextFieldStyle style = new TextField.TextFieldStyle();
        style.font = font;
        style.fontColor = Color.BLACK;
        style.background = null;
        style.cursor = skin.newDrawable("cursor", Color.BLACK);
        style.selection = skin.newDrawable("selection", new Color(0, 0, 1, 0.3f));

        textArea1 = new TextArea(savedText, style);
        textArea1.setFocusTraversal(false);

        textArea2 = new TextArea("", style);
        textArea2.setFocusTraversal(false);

        textArea1.addListener(event -> {
            wrapText(textArea1);
            handleOverflow(textArea1, textArea2);
            return false;
        });

        textArea2.addListener(new InputListener() {
            @Override
            public boolean keyDown(InputEvent event, int keycode) {
                if (keycode == Input.Keys.BACKSPACE && textArea2.getText().isEmpty()) {
                    stage.setKeyboardFocus(textArea1);
                    textArea1.setCursorPosition(textArea1.getText().length());
                    return true;
                }
                return false;
            }
        });
    }

    private void wrapText(TextArea area) {
        BitmapFont font = area.getStyle().font;
        GlyphLayout layout = new GlyphLayout();

        String text = area.getText().replace("\r", "");
        int originalCursor = area.getCursorPosition();
        boolean cursorAtEnd = originalCursor == text.length();

        StringBuilder wrapped = new StringBuilder();
        StringBuilder currentLine = new StringBuilder();

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);

            if (c == '\n') {
                wrapped.append(currentLine).append("\n");
                currentLine = new StringBuilder();
                continue;
            }

            currentLine.append(c);
            layout.setText(font, currentLine);

            if (layout.width > columnWidth) {
                currentLine.deleteCharAt(currentLine.length() - 1);
                wrapped.append(currentLine).append("\n");
                currentLine = new StringBuilder();
                if (c != ' ') {
                    currentLine.append(c);
                }
            }
        }

        if (currentLine.length() > 0) wrapped.append(currentLine);

        area.setText(wrapped.toString());

        if (cursorAtEnd) {
            area.setCursorPosition(area.getText().length());
        }
    }

    private void handleOverflow(TextArea current, TextArea next) {
        BitmapFont font = current.getStyle().font;
        int maxLines = (int) (current.getHeight() / font.getLineHeight());

        String[] lines = current.getText().split("\n", -1);
        if (lines.length <= maxLines) return;

        StringBuilder stay = new StringBuilder();
        StringBuilder overflow = new StringBuilder();

        for (int i = 0; i < lines.length; i++) {
            if (i < maxLines) {
                stay.append(lines[i]).append("\n");
            } else {
                overflow.append(lines[i]).append("\n");
            }
        }

        current.setText(stay.toString().trim());

        if (next != null) {
            next.setText(overflow.toString().trim());
            stage.setKeyboardFocus(next);
        }
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
        columnWidth = halfWidth - outerPadding - innerPadding;

        textArea1.setSize(columnWidth, columnHeight);
        textArea1.setPosition(noteImage.getX() + outerPadding, noteImage.getY() + paddingBottom);

        textArea2.setSize(columnWidth, columnHeight);
        textArea2.setPosition(noteImage.getX() + halfWidth + innerPadding * 2, noteImage.getY() + paddingBottom);
    }

    public void show() {
        stage.addActor(background);
        stage.addActor(noteImage);
        stage.addActor(textArea1);
        stage.addActor(textArea2);
        stage.setKeyboardFocus(textArea1);
    }

    public void remove() {
        savedText = textArea1.getText();

        textArea1.remove();
        textArea2.remove();
        noteImage.remove();
        background.remove();
    }

    public void dispose() {
        noteTexture.dispose();
        skin.dispose();
    }
}
