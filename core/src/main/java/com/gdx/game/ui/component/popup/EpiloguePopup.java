package com.gdx.game.ui.component.popup;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.gdx.game.DetectiveGame;
import com.gdx.game.infrastructure.Assets;

public class EpiloguePopup extends AbstractPopup {
    private final Image epilogueImage;
    private final Texture epilogueTexture;
    private final Label epilogueLabel;
    private final Image continueButton;

    private final DetectiveGame game;
    private final GlyphLayout layout;

    private String fullText = "";

    private final Array<String> pages = new Array<>();
    private int currentPageIndex = 0;

    private float visibleTextHeight = 0f;

    private float charTimer = 0f;
    private final float charDelay = 0.04f;
    private int charIndex = 0;
    private boolean pageFinished = false;

    public EpiloguePopup(Stage stage, DetectiveGame game) {
        super(stage);
        this.game = game;
        layout = new GlyphLayout();

        epilogueTexture = new Texture(Assets.EPILOGUE);
        epilogueImage = new Image(epilogueTexture);

        Label.LabelStyle labelStyle = new Label.LabelStyle();
        labelStyle.font = skin.getFont("default-font");
        labelStyle.fontColor = new Color(154 / 255f, 109 / 255f, 69 / 255f, 1f);

        epilogueLabel = new Label("", labelStyle);
        epilogueLabel.setWrap(true);
        epilogueLabel.setAlignment(Align.center);

        continueButton = game.getButtonFactory().createButton(
            Assets.CONTINUE_BUTTON, 60, 60,
            this::onContinueClicked
        );

        resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
    }

    public void setFullText(String text) {
        this.fullText = (text != null ? text.trim() : "");
        rebuildPages();
        showPage(0);
    }

    private void onContinueClicked() {
        if (!pageFinished && pages.size > 0) {
            String pageText = pages.get(currentPageIndex);
            epilogueLabel.setText(pageText);
            charIndex = pageText.length();
            pageFinished = true;
            return;
        }

        if (currentPageIndex + 1 < pages.size) {
            showPage(currentPageIndex + 1);
        } else {
            remove();
            game.overlay.showTheEndPublic();
        }
    }

    private void showPage(int index) {
        if (index < 0 || index >= pages.size) return;
        currentPageIndex = index;

        charTimer = 0f;
        charIndex = 0;
        pageFinished = false;
        epilogueLabel.setText("");
    }

    private void rebuildPages() {
        pages.clear();

        if (fullText == null || fullText.isEmpty()) {
            pages.add("");
            return;
        }

        BitmapFont font = epilogueLabel.getStyle().font;

        float labelWidth = epilogueLabel.getWidth();
        float maxHeight = visibleTextHeight > 0 ? visibleTextHeight : epilogueLabel.getHeight();

        String[] words = fullText.split("\\s+");
        StringBuilder pageBuilder = new StringBuilder();

        for (String w : words) {
            String candidate;
            if (pageBuilder.length() == 0) {
                candidate = w;
            } else {
                candidate = pageBuilder + " " + w;
            }

            layout.setText(font, candidate, epilogueLabel.getStyle().fontColor,
                labelWidth, Align.left, true);

            if (layout.height > maxHeight) {
                pages.add(pageBuilder.toString().trim());
                pageBuilder.setLength(0);
                pageBuilder.append(w);
            } else {
                pageBuilder.setLength(0);
                pageBuilder.append(candidate);
            }
        }

        if (pageBuilder.length() > 0) {
            pages.add(pageBuilder.toString().trim());
        }

        if (pages.size == 0) {
            pages.add("");
        }
    }

    public void update(float delta) {
        if (pages.size == 0) return;

        if (pageFinished) return;

        String pageText = pages.get(currentPageIndex);
        if (pageText == null) pageText = "";

        charTimer += delta;

        while (charTimer >= charDelay && charIndex < pageText.length()) {
            charTimer -= charDelay;
            charIndex++;
            epilogueLabel.setText(pageText.substring(0, charIndex));
        }

        if (charIndex >= pageText.length()) {
            pageFinished = true;
        }
    }

    public void resize(float screenWidth, float screenHeight) {
        epilogueImage.setWidth(screenWidth);
        background.setSize(screenWidth, screenHeight);
        resizeCentered(epilogueImage, epilogueTexture, screenWidth, screenHeight);

        float textAreaWidth  = epilogueImage.getWidth() * 0.7f;
        float textAreaX = epilogueImage.getX() + epilogueImage.getWidth() * 0.15f;
        float textAreaY = epilogueImage.getY() + epilogueImage.getHeight() * 0.3f;
        visibleTextHeight = epilogueImage.getHeight() * 0.35f;

        epilogueLabel.setWidth(textAreaWidth);
        epilogueLabel.setHeight(visibleTextHeight);
        epilogueLabel.setPosition(textAreaX, textAreaY);

        float btnWidth = epilogueImage.getWidth() * 0.5f;
        float btnHeight = epilogueImage.getHeight() * 0.1f;
        float paddingBottom = epilogueImage.getHeight() * 0.1f;

        continueButton.setSize(btnWidth, btnHeight);
        continueButton.setPosition(
            epilogueImage.getX() + (epilogueImage.getWidth() - btnWidth) / 2f,
            epilogueImage.getY() + paddingBottom
        );

        if (fullText != null && !fullText.isEmpty()) {
            rescaleFontToFit();

            rebuildPages();
            showPage(Math.min(currentPageIndex, pages.size - 1));
        }
    }

    @Override
    public void show() {
        super.show();
        stage.addActor(epilogueImage);
        stage.addActor(epilogueLabel);
        stage.addActor(continueButton);
    }

    @Override
    public void remove() {
        super.remove();
        epilogueImage.remove();
        epilogueLabel.remove();
        continueButton.remove();
    }

    private void rescaleFontToFit() {
        if (epilogueImage.getWidth() == 0 || epilogueImage.getHeight() == 0) return;

        Label.LabelStyle style = epilogueLabel.getStyle();
        BitmapFont font = style.font;

        float labelWidth = epilogueLabel.getWidth();
        float availableHeight = visibleTextHeight > 0 ? visibleTextHeight : epilogueLabel.getHeight();

        String textToMeasure = (fullText != null && !fullText.isEmpty())
            ? fullText
            : "Т";

        font.getData().setScale(1f);

        layout.setText(font, textToMeasure, style.fontColor, labelWidth, Align.left, true);
        float prefHeight = layout.height;
        if (prefHeight <= 0f) prefHeight = font.getCapHeight();

        float scale = availableHeight / prefHeight;
        scale = MathUtils.clamp(scale, 0.6f, 1.4f);

        font.getData().setScale(scale);
        epilogueLabel.invalidateHierarchy();
    }
}
