package com.gdx.game.utils;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextArea;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;

import java.util.ArrayList;
import java.util.List;

public class NotePages {
    private final Stage stage;
    private final Skin skin;
    private float columnWidth;
    private float columnHeight;

    private final List<TextArea[]> pages = new ArrayList<>();
    private int currentPageIndex = 0;

    public NotePages(Stage stage, Skin skin) {
        this.stage = stage;
        this.skin = skin;
        createNewPage();
    }

    private void createNewPage() {
        BitmapFont font = new BitmapFont(Gdx.files.internal("fonts/8bold.fnt"));
        font.getData().lineHeight *= 1.5f;

        TextField.TextFieldStyle style = new TextField.TextFieldStyle();
        style.font = font;
        style.fontColor = Color.BLACK;
        style.cursor = skin.newDrawable("cursor", Color.BLACK);
        style.background = null;
        style.selection = skin.newDrawable("white", new Color(0.3f, 0.5f, 1f, 0.5f));

        TextArea left = new TextArea("", style);
        left.setFocusTraversal(false);

        TextArea right = new TextArea("", style);
        right.setFocusTraversal(false);

        TextArea[] page = new TextArea[]{left, right};
        pages.add(page);

        if (pages.size() == 1) {
            stage.addActor(left);
            stage.addActor(right);
            stage.setKeyboardFocus(left);
        }

        left.setTextFieldListener((textField, c) -> {
            wrapText(left);
            handleOverflow(left, right);
        });

        right.setTextFieldListener((textField, c) -> {
            wrapText(right);
            handleOverflow(right, null);
        });
    }

    public void setColumnWidth(float width) {
        this.columnWidth = width;
    }

    public void setPosition(float x, float y, float height) {
        this.columnHeight = height;
        for (TextArea[] page : pages) {
            page[0].setSize(columnWidth, height);
            page[0].setPosition(x, y);

            float innerPadding = columnWidth * 0.5f;

            page[1].setSize(columnWidth, height);
            page[1].setPosition(x + columnWidth + innerPadding, y);
        }
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

        if (!currentLine.isEmpty()) wrapped.append(currentLine);
        area.setText(wrapped.toString());

        if (cursorAtEnd) area.setCursorPosition(area.getText().length());
    }

    private void handleOverflow(TextArea current, TextArea next) {
        BitmapFont font = current.getStyle().font;
        int maxLines = (int) (columnHeight / font.getLineHeight());

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
            next.setCursorPosition(overflow.length());
            stage.setKeyboardFocus(next);
        } else {
            createNewPage();
            TextArea[] newPage = pages.get(pages.size() - 1);

            setPositionForNewPage(newPage);
            newPage[0].setText(overflow.toString().trim());
            newPage[0].setCursorPosition(newPage[0].getText().length());
            showPage(pages.size() - 1);
        }
    }

    private void setPositionForNewPage(TextArea[] page) {
        page[0].setSize(columnWidth, columnHeight);
        page[0].setPosition(pages.get(0)[0].getX(), pages.get(0)[0].getY());

        page[1].setSize(columnWidth, columnHeight);
        page[1].setPosition(pages.get(0)[1].getX(), pages.get(0)[1].getY());
    }

    public void showPage(int index) {
        if (index < 0 || index >= pages.size()) return;

        for (TextArea[] page : pages) {
            page[0].remove();
            page[1].remove();
        }

        TextArea[] page = pages.get(index);
        stage.addActor(page[0]);
        stage.addActor(page[1]);

        stage.setKeyboardFocus(page[0]);
        currentPageIndex = index;
    }

    public void nextPage() {
        if (currentPageIndex < pages.size() - 1) {
            showPage(currentPageIndex + 1);
        }
    }

    public void prevPage() {
        if (currentPageIndex > 0) {
            showPage(currentPageIndex - 1);
            removeEmptyPagesAfterCurrent();
        }
    }

    private void removeEmptyPagesAfterCurrent() {
        for (int i = pages.size() - 1; i > currentPageIndex; i--) {
            TextArea[] page = pages.get(i);
            if (page[0].getText().trim().isEmpty() && page[1].getText().trim().isEmpty()) {
                page[0].remove();
                page[1].remove();
                pages.remove(i);
            }
        }
    }

    public void show() {
        showPage(0);
    }

    public void remove() {
        for (TextArea[] page : pages) {
            page[0].remove();
            page[1].remove();
        }
    }
}
