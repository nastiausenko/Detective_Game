package com.gdx.game.ui.popup;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.gdx.game.DetectiveGame;
import com.gdx.game.utils.Assets;
import com.gdx.game.utils.ScreenUtilsHelper;

public class DossierPopup extends AbstractPopup {
    private final Texture[] pages;
    private final Image pageImage;

    private final Image btnNext;
    private final Image btnPrev;
    private final Image closeBtn;

    private int currentPage = 0;

    public DossierPopup(Stage stage, DetectiveGame game) {
        super(stage);

        pages = new Texture[]{
            new Texture(Assets.DOSSIER_1),
            new Texture(Assets.DOSSIER_2),
            new Texture(Assets.DOSSIER_3)
        };

        pageImage = new Image(pages[currentPage]);
        pageImage.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                event.stop();
            }
        });

        btnPrev = game.getButtonFactory().createButton(Assets.ARROW_LEFT, 64, 64, this::prevPage);
        btnNext = game.getButtonFactory().createButton(Assets.ARROW_RIGHT, 64, 64, this::nextPage);
        closeBtn = game.getButtonFactory().createButton(Assets.CLOSE_BUTTON, 64, 64, this::remove);

        resize(stage.getViewport().getWorldWidth(), stage.getViewport().getWorldHeight());
    }

    public void nextPage() {
        if (currentPage < pages.length - 1) {
            currentPage++;
            updatePageImage();
        }
    }

    public void prevPage() {
        if (currentPage > 0) {
            currentPage--;
            updatePageImage();
        }
    }

    private void updatePageImage() {
        pageImage.setDrawable(new TextureRegionDrawable(new TextureRegion(pages[currentPage])));
    }

    public void resize(float screenWidth, float screenHeight) {
        background.setSize(screenWidth, screenHeight);
        resizeCentered(pageImage, pages[0], screenWidth, screenHeight);

        float pageWidth = pageImage.getWidth();
        float pageHeight = pageImage.getHeight();
        float targetHeight = screenHeight * 0.12f;

        ScreenUtilsHelper.scaleAndPositionButton(btnPrev, targetHeight,
            pageImage.getX() - targetHeight * 0.5f, pageImage.getY() + pageHeight / 2 - targetHeight / 2);
        ScreenUtilsHelper.scaleAndPositionButton(btnNext, targetHeight,
            pageImage.getX() + pageWidth - targetHeight * 0.5f, pageImage.getY() + pageHeight / 2 - targetHeight / 2);
        ScreenUtilsHelper.scaleAndPositionButton(closeBtn, targetHeight,
            10, screenHeight - targetHeight - 10);
    }

    @Override
    public void show() {
        super.show();
        stage.addActor(pageImage);
        stage.addActor(btnPrev);
        stage.addActor(btnNext);
        stage.addActor(closeBtn);
    }

    @Override
    public void remove() {
        super.remove();
        pageImage.remove();
        btnPrev.remove();
        btnNext.remove();
        closeBtn.remove();
    }

    public void dispose() {
        for (Texture t : pages) t.dispose();
    }
}
