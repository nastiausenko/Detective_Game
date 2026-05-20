package com.gdx.game.infrastructure;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
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

    private static final String PREFS_NAME = "notes_data";
    private final Preferences prefs;

    private boolean programmaticChange = false;
    private boolean isVisible = false;

    private static class NoteTextArea extends TextArea {
        NoteTextArea(String text, TextField.TextFieldStyle style) {
            super(text, style);
        }

        void refreshTextMetrics() {
            sizeChanged();
            invalidate();
        }
    }

    public NotePages(Stage stage, Skin skin) {
        this.stage = stage;
        this.skin = skin;
        this.prefs = Gdx.app.getPreferences(PREFS_NAME);
        createNewPage();
        loadNotes();
    }

    public void saveNotes() {
        for (int i = 0; i < pages.size(); i++) {
            TextArea[] page = pages.get(i);
            prefs.putString("page" + i + "_left", page[0].getText());
            prefs.putString("page" + i + "_right", page[1].getText());
        }
        prefs.putInteger("pages_count", pages.size());
        prefs.flush();
    }

    private void loadNotes() {
        int count = prefs.getInteger("pages_count", 0);
        if (count == 0) return;

        pages.clear();
        for (int i = 0; i < count; i++) {
            createNewPage();
            TextArea[] page = pages.get(i);
            page[0].setText(prefs.getString("page" + i + "_left", ""));
            page[1].setText(prefs.getString("page" + i + "_right", ""));
        }
        currentPageIndex = 0;
    }

    public void onExit() {
        saveNotes();
    }

    private TextField.TextFieldStyle createTextStyle() {
        TextField.TextFieldStyle style = new TextField.TextFieldStyle();
        style.font = skin.getFont("default-font");
        style.fontColor = Color.BLACK;
        style.cursor = skin.newDrawable("cursor", Color.BLACK);
        style.background = null;
        style.selection = skin.newDrawable("white", new Color(0.3f, 0.5f, 1f, 0.5f));
        return style;
    }

    private void createNewPage() {
        TextField.TextFieldStyle style = createTextStyle();

        TextArea left = new NoteTextArea("", style);
        TextArea right = new NoteTextArea("", style);
        left.setFocusTraversal(false);
        right.setFocusTraversal(false);

        TextArea[] page = new TextArea[]{left, right};
        int pageIndex = pages.size();
        pages.add(page);

        left.setTextFieldListener((tf, c) -> {
            if (!programmaticChange) rebalanceFromPage(pageIndex);
        });
        right.setTextFieldListener((tf, c) -> {
            if (!programmaticChange) rebalanceFromPage(pageIndex);
        });
    }

    public void setColumnWidth(float width) {
        this.columnWidth = width;
        for (TextArea[] page : pages) {
            page[0].setWidth(width);
            page[1].setWidth(width);
        }
    }

    public void setPosition(float x, float y, float height, float innerPadding) {
        this.columnHeight = height;
        setFontScale();

        for (TextArea[] page : pages) {
            page[0].setSize(columnWidth, height);
            page[0].setPosition(x, y);
            refreshTextMetrics(page[0]);

            float rightX = x + columnWidth + innerPadding * 2;
            page[1].setSize(columnWidth, height);
            page[1].setPosition(rightX, y);
            refreshTextMetrics(page[1]);
        }
    }

    private void setFontScale() {
        boolean wasProgrammaticChange = programmaticChange;
        programmaticChange = true;

        try {
            String[][] texts = new String[pages.size()][2];
            int[][] cursorPositions = new int[pages.size()][2];

            for (int i = 0; i < pages.size(); i++) {
                TextArea[] page = pages.get(i);
                texts[i][0] = page[0].getText();
                texts[i][1] = page[1].getText();
                cursorPositions[i][0] = page[0].getCursorPosition();
                cursorPositions[i][1] = page[1].getCursorPosition();
            }

            FontScaler.applyScale(skin.getFont("default-font"));

            for (int i = 0; i < pages.size(); i++) {
                TextArea[] page = pages.get(i);
                TextField.TextFieldStyle style = createTextStyle();

                page[0].setStyle(style);
                page[1].setStyle(style);

                page[0].setText(texts[i][0]);
                page[1].setText(texts[i][1]);
                page[0].setCursorPosition(Math.min(cursorPositions[i][0], texts[i][0].length()));
                page[1].setCursorPosition(Math.min(cursorPositions[i][1], texts[i][1].length()));
            }
        } finally {
            programmaticChange = wasProgrammaticChange;
        }
    }

    private void setPositionForNewPage(TextArea[] page) {
        if (pages.isEmpty()) return;
        TextArea[] first = pages.get(0);

        page[0].setSize(columnWidth, columnHeight);
        page[0].setPosition(first[0].getX(), first[0].getY());
        refreshTextMetrics(page[0]);

        page[1].setSize(columnWidth, columnHeight);
        page[1].setPosition(first[1].getX(), first[1].getY());
        refreshTextMetrics(page[1]);
    }

    private void refreshTextMetrics(TextArea area) {
        if (area instanceof NoteTextArea) {
            ((NoteTextArea) area).refreshTextMetrics();
        }
    }


    private int findOverflowStartIndex(String text, BitmapFont font, float width, int maxLines) {
        if (text == null || text.isEmpty()) return -1;

        GlyphLayout layout = new GlyphLayout();
        int lineCount = 0, lineStart = 0, lastSpace = -1;

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\r') continue;
            if (c == '\n') {
                if (++lineCount > maxLines) return lineStart;
                lineStart = i + 1;
                lastSpace = -1;
                continue;
            }

            if (Character.isWhitespace(c)) lastSpace = i;

            layout.setText(font, text.substring(lineStart, i + 1));
            if (layout.width > width) {
                int breakIndex = (lastSpace >= lineStart) ? lastSpace + 1 : i;
                if (++lineCount > maxLines) return breakIndex;
                lineStart = breakIndex;
                lastSpace = -1;
            }
        }

        return lineCount + 1 > maxLines ? lineStart : -1;
    }

    private void rebalanceFromPage(int startIndex) {
        if (startIndex < 0 || startIndex >= pages.size()) return;

        programmaticChange = true;
        try {
            boolean changed;
            do {
                changed = false;
                for (int i = startIndex; i < pages.size(); i++) {
                    TextArea[] page = pages.get(i);
                    changed |= handleOverflow(page, 0, i);
                    changed |= handleOverflow(page, 1, i);
                }
            } while (changed);
        } finally {
            programmaticChange = false;
        }
    }

    private boolean handleOverflow(TextArea[] page, int colIndex, int pageIndex) {
        TextArea area = page[colIndex];
        BitmapFont font = area.getStyle().font;
        int maxLines = Math.max(1, area.getLinesShowing());

        String text = area.getText();
        int overflowStart = findOverflowStartIndex(text, font, columnWidth, maxLines);
        if (overflowStart == -1) return false;

        String stay = text.substring(0, overflowStart).trim();
        String overflow = text.substring(overflowStart).trim();

        TextArea target;
        int nextPageIndex = pageIndex + (colIndex == 0 ? 0 : 1);

        if (colIndex == 0) {
            target = page[1];
        } else {
            if (nextPageIndex >= pages.size()) {
                createNewPage();
                setPositionForNewPage(pages.get(pages.size() - 1));
            }
            target = pages.get(nextPageIndex)[0];
            if (isVisible) {
                showPage(nextPageIndex);
            } else {
                currentPageIndex = nextPageIndex;
            }
        }

        int cursorPos = area.getCursorPosition();

        area.setText(stay);
        String newText = target.getText().isEmpty() ? overflow : target.getText() + "\n" + overflow;
        target.setText(newText);

        if (cursorPos > stay.length() && isVisible) {
            int newCursor = Math.max(0, Math.min(cursorPos - overflowStart, newText.length()));
            stage.setKeyboardFocus(target);
            target.setCursorPosition(newCursor);
        }

        return true;
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
        page[0].setCursorPosition(page[0].getText().length());
        currentPageIndex = index;
    }

    public void nextPage() {
        if (currentPageIndex + 1 < pages.size()) showPage(currentPageIndex + 1);
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
        isVisible = true;
        showPage(0);
    }

    public void remove() {
        isVisible = false;
        for (TextArea[] page : pages) {
            page[0].remove();
            page[1].remove();
        }
    }
}
