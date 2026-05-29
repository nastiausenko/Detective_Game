package com.gdx.game.widgets.popup;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.gdx.game.app.DetectiveGame;
import com.gdx.game.shared.config.Assets;
import com.gdx.game.shared.ui.NotePages;
import com.gdx.game.shared.config.UiLayout;
import com.gdx.game.shared.config.UiLayoutProfile;
import com.gdx.game.shared.lib.ScreenUtilsHelper;

public class NotePopup extends AbstractPopup {
    private final Texture noteTexture;
    private final Image noteImage;
    private final NotePages pages;

    private final Image btnNext;
    private final Image btnPrev;
    private final Image closeBtn;

    public NotePopup(Stage stage, DetectiveGame game) {
        super(stage);
        this.pages = new NotePages(stage, skin);

        background.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                clearTextFocus();
                event.stop();
            }
        });

        noteTexture = new Texture(Assets.NOTES);
        noteImage = new Image(noteTexture);
        noteImage.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                clearTextFocus();
                event.stop();
            }
        });

        btnPrev = game.getButtonFactory().createButton(Assets.ARROW_LEFT, 64, 64, pages::prevPage);
        btnNext = game.getButtonFactory().createButton(Assets.ARROW_RIGHT, 64, 64, pages::nextPage);
        closeBtn = game.getButtonFactory().createButton(Assets.CLOSE_BUTTON, 64, 64, () -> {
            pages.onExit();
            remove();
        });

        resize(stage.getViewport().getWorldWidth(), stage.getViewport().getWorldHeight());
    }

    public void resize(float screenWidth, float screenHeight) {
        UiLayoutProfile profile = UiLayout.current(screenWidth, screenHeight);
        resizeCentered(noteImage, noteTexture, screenWidth, screenHeight);

        float width = noteImage.getWidth();
        float height = noteImage.getHeight();

        float paddingTop = height * 0.13f;
        float paddingBottom = height * 0.13f;
        float columnHeight = height - paddingTop - paddingBottom;

        float outerPadding = width * 0.13f;
        float innerPadding = width * 0.1f;
        float halfWidth = width / 2f;
        float columnWidth = halfWidth - outerPadding - innerPadding;

        pages.setColumnWidth(columnWidth);
        pages.setPosition(
                noteImage.getX() + outerPadding,
                noteImage.getY() + paddingBottom,
                columnHeight,
                innerPadding
        );

        float targetHeight = screenHeight * profile.getPopupButtonHeightRatio();
        float margin = profile.scale(10f);

        ScreenUtilsHelper.scaleButton(btnPrev, targetHeight, stage);
        ScreenUtilsHelper.scaleButton(btnNext, targetHeight, stage);
        ScreenUtilsHelper.scaleButton(closeBtn, targetHeight, stage);

        btnPrev.setPosition(noteImage.getX() - btnPrev.getWidth() * 0.5f,
            noteImage.getY() + height / 2 - btnPrev.getHeight() / 2);
        btnNext.setPosition(noteImage.getX() + width - btnNext.getWidth() * 0.5f,
            noteImage.getY() + height / 2 - btnNext.getHeight() / 2);
        closeBtn.setPosition(margin,
            screenHeight - closeBtn.getHeight() - margin);
    }

    @Override
    public void show() {
        super.show();
        addPopupActors(noteImage);
        pages.show();
        addPopupActors(btnPrev, btnNext, closeBtn);

        resize(stage.getViewport().getWorldWidth(), stage.getViewport().getWorldHeight());
    }

    @Override
    public void remove() {
        clearTextFocus();
        super.remove();
        pages.remove();
        removePopupActors(noteImage, btnPrev, btnNext, closeBtn);
    }

    public void dispose() {
        noteTexture.dispose();
        skin.dispose();
    }

    private void clearTextFocus() {
        stage.setKeyboardFocus(null);
        stage.setScrollFocus(null);
        Gdx.input.setOnscreenKeyboardVisible(false);
    }
}
