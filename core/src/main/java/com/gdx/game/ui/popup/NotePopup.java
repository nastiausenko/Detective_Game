package com.gdx.game.ui.popup;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.gdx.game.DetectiveGame;
import com.gdx.game.utils.Assets;
import com.gdx.game.utils.NotePages;
import com.gdx.game.utils.ScreenUtilsHelper;

public class NotePopup extends AbstractPopup {
    private final Texture noteTexture;
    private final Image noteImage;
    private final Skin skin;
    private final NotePages pages;

    private final Image btnNext;
    private final Image btnPrev;
    private final Image closeBtn;

    public NotePopup(Stage stage, Skin skin, DetectiveGame game) {
        super(stage);
        this.skin = skin;
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
        //TODO add close button image
        closeBtn = game.getButtonFactory().createButton(Assets.CLOSE_BUTTON, 64, 64, () -> {
            pages.onExit();
            remove();
        });

        resize(stage.getViewport().getWorldWidth(), stage.getViewport().getWorldHeight());
    }

    public void resize(float screenWidth, float screenHeight) {
        background.setSize(screenWidth, screenHeight);
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

        float referenceWidth = 1280f;
        float scaleFactor = Math.max(0.8f, Math.min(1.5f, screenWidth / referenceWidth));

        pages.setFontScale(scaleFactor);
        pages.setColumnWidth(columnWidth);
        pages.setPosition(
                noteImage.getX() + outerPadding,
                noteImage.getY() + paddingBottom,
                columnHeight,
                innerPadding
        );

        float targetHeight = screenHeight * 0.12f;
        float btnSize = 64;

        ScreenUtilsHelper.scaleAndPositionButton(btnPrev, targetHeight, noteImage.getX() - 25,
            noteImage.getY() + height / 2 - btnSize / 2);

        ScreenUtilsHelper.scaleAndPositionButton(btnNext, targetHeight, noteImage.getX() + width - 40,
            noteImage.getY() + height / 2 - btnSize / 2);

        ScreenUtilsHelper.scaleAndPositionButton(closeBtn, targetHeight, 10,
            screenHeight - targetHeight - 10);
    }

    @Override
    public void show() {
        super.show();
        stage.addActor(noteImage);
        pages.show();
        stage.addActor(btnPrev);
        stage.addActor(btnNext);
        stage.addActor(closeBtn);
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
