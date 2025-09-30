package com.gdx.game.utils;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextArea;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;

public class NotePages {
    private final Stage stage;
    private final Skin skin;
    private TextArea left;
    private TextArea right;
    private float columnWidth;
    private String savedText = "";

    public NotePages(Stage stage, Skin skin) {
        this.stage = stage;
        this.skin = skin;
        createTextAreas();
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

        left = new TextArea(savedText, style);
        left.setFocusTraversal(false);

        right = new TextArea("", style);
        right.setFocusTraversal(false);

        left.addListener(event -> {
            wrapText(left);
            handleOverflow(left, right);
            return false;
        });

        right.addListener(new InputListener() {
            @Override
            public boolean keyDown(InputEvent event, int keycode) {
                if (keycode == Input.Keys.BACKSPACE && right.getText().isEmpty()) {
                    stage.setKeyboardFocus(left);
                    left.setCursorPosition(left.getText().length());
                    return true;
                }
                return false;
            }
        });
    }

    public void setColumnWidth(float width) {
        this.columnWidth = width;
    }

    public void setPosition(float x, float y, float height) {
        left.setSize(columnWidth, height);
        left.setPosition(x, y);

        float innerPadding = columnWidth * 0.5f;

        right.setSize(columnWidth, height);
        right.setPosition(x + columnWidth + innerPadding, y);
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
                if (c != ' ') currentLine.append(c);
            }
        }

        if (currentLine.length() > 0) wrapped.append(currentLine);
        area.setText(wrapped.toString());

        if (cursorAtEnd) area.setCursorPosition(area.getText().length());
    }

    private void handleOverflow(TextArea current, TextArea next) {
        BitmapFont font = current.getStyle().font;
        int maxLines = (int) (current.getHeight() / font.getLineHeight());

        String[] lines = current.getText().split("\n", -1);
        if (lines.length <= maxLines) return;

        StringBuilder stay = new StringBuilder();
        StringBuilder overflow = new StringBuilder();

        for (int i = 0; i < lines.length; i++) {
            if (i < maxLines) stay.append(lines[i]).append("\n");
            else overflow.append(lines[i]).append("\n");
        }

        current.setText(stay.toString().trim());

        if (next != null) {
            next.setText(overflow.toString().trim());
            stage.setKeyboardFocus(next);
        }
    }

    public void show() {
        stage.addActor(left);
        stage.addActor(right);
        stage.setKeyboardFocus(left);
    }

    public void remove() {
        savedText = left.getText();
        left.remove();
        right.remove();
    }

    public String getSavedText() {
        return savedText;
    }

    public TextArea getLeft() { return left; }
    public TextArea getRight() { return right; }
}
