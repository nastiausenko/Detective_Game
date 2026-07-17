package com.gdx.game.ui.component.popup;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.gdx.game.infrastructure.GameContext;
import com.gdx.game.game.GameFlow;
import com.gdx.game.infrastructure.Assets;
import com.gdx.game.ui.style.UiStyles;
import com.gdx.game.ui.component.TypewriterText;
import com.gdx.game.render.ScreenUtilsHelper;

public class EpiloguePopup extends AbstractPopup {
    private final Image epilogueImage;
    private final Texture epilogueTexture;
    private final Label epilogueLabel;
    private final Image continueButton;

    private final GameContext game;
    private final GameFlow flow;
    private final GlyphLayout layout;

    private String fullText = "";

    private final Array<String> pages = new Array<>();
    private int currentPageIndex = 0;

    private float visibleTextHeight = 0f;
    private final TypewriterText typewriterText;

    public EpiloguePopup(Stage stage, GameContext game, GameFlow flow) {
        super(stage);
        this.game = game;
        this.flow = flow;
        layout = new GlyphLayout();

        epilogueTexture = new Texture(Assets.EPILOGUE);
        epilogueImage = new Image(epilogueTexture);

        epilogueLabel = new Label("", UiStyles.label(skin, UiStyles.parchmentText()));
        epilogueLabel.setWrap(true);
        epilogueLabel.setAlignment(Align.center);
        typewriterText = new TypewriterText(epilogueLabel);

        continueButton = game.buttonFactory.createButton(
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
        if (!typewriterText.isFinished() && pages.size > 0) {
            typewriterText.finish();
            return;
        }

        if (currentPageIndex + 1 < pages.size) {
            showPage(currentPageIndex + 1);
        } else {
            remove();
            flow.showTheEnd();
        }
    }

    private void showPage(int index) {
        if (index < 0 || index >= pages.size) return;
        currentPageIndex = index;
        typewriterText.start(pages.get(currentPageIndex));
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
        typewriterText.update(delta);
    }

    public void resize(float screenWidth, float screenHeight) {
        epilogueImage.setWidth(screenWidth);
        resizeCentered(epilogueImage, epilogueTexture, screenWidth, screenHeight);

        float textAreaWidth  = epilogueImage.getWidth() * 0.7f;
        float textAreaX = epilogueImage.getX() + epilogueImage.getWidth() * 0.15f;
        float textAreaY = epilogueImage.getY() + epilogueImage.getHeight() * 0.3f;
        visibleTextHeight = epilogueImage.getHeight() * 0.35f;

        epilogueLabel.setWidth(textAreaWidth);
        epilogueLabel.setHeight(visibleTextHeight);
        epilogueLabel.setPosition(textAreaX, textAreaY);

        ScreenUtilsHelper.scaleNavButton(continueButton, epilogueImage);

        if (fullText != null && !fullText.isEmpty()) {
            rescaleFontToFit();

            rebuildPages();
            showPage(Math.min(currentPageIndex, pages.size - 1));
        }
    }

    @Override
    public void show() {
        super.show();
        addPopupActors(epilogueImage, epilogueLabel, continueButton);
    }

    @Override
    public void remove() {
        super.remove();
        removePopupActors(epilogueImage, epilogueLabel, continueButton);
    }

    private void rescaleFontToFit() {
        if (epilogueImage.getWidth() == 0 || epilogueImage.getHeight() == 0) return;

        float labelWidth = epilogueLabel.getWidth();
        float availableHeight = visibleTextHeight > 0 ? visibleTextHeight : epilogueLabel.getHeight();
        PopupTextScaler.scaleToFitLeft(epilogueLabel, layout, fullText, labelWidth, availableHeight);
    }
}
