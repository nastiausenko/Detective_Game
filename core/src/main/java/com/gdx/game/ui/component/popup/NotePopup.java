package com.gdx.game.ui.component.popup;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.gdx.game.DetectiveGame;
import com.gdx.game.infrastructure.Assets;
import com.gdx.game.infrastructure.NotePages;
import com.gdx.game.utils.ScreenUtilsHelper;

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

        noteTexture = new Texture(Assets.NOTES);
        noteImage = new Image(noteTexture);
        noteImage.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
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

        float targetHeight = screenHeight * 0.12f;

        ScreenUtilsHelper.scaleButton(btnPrev, targetHeight, stage);
        ScreenUtilsHelper.scaleButton(btnNext, targetHeight, stage);
        ScreenUtilsHelper.scaleButton(closeBtn, targetHeight, stage);

        btnPrev.setPosition(noteImage.getX() - btnPrev.getWidth() * 0.5f,
            noteImage.getY() + height / 2 - btnPrev.getHeight() / 2);
        btnNext.setPosition(noteImage.getX() + width - btnNext.getWidth() * 0.5f,
            noteImage.getY() + height / 2 - btnNext.getHeight() / 2);
        closeBtn.setPosition(10,
            screenHeight - closeBtn.getHeight() - 10);
    }

    @Override
    public void show() {
        super.show();
        stage.addActor(noteImage);
        pages.show();
        stage.addActor(btnPrev);
        stage.addActor(btnNext);
        stage.addActor(closeBtn);

        resize(stage.getViewport().getWorldWidth(), stage.getViewport().getWorldHeight());
    }

    @Override
    public void remove() {
        super.remove();
        noteImage.remove();
        pages.remove();
        btnPrev.remove();
        btnNext.remove();
        closeBtn.remove();
    }

    public void dispose() {
        noteTexture.dispose();
        skin.dispose();
    }
}
