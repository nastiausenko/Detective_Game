package com.gdx.game.ui.component.popup;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.gdx.game.infrastructure.BackgroundFactory;

public abstract class AbstractPopup {
    protected final Stage stage;
    protected final Image background;
    protected final Skin skin;

    protected AbstractPopup(Stage stage) {
        this.stage = stage;
        skin = new Skin(Gdx.files.internal("ui/uiskin.json"));

        this.background = BackgroundFactory.createDimBackground(
            stage.getViewport().getWorldWidth(),
            stage.getViewport().getWorldHeight(),
            0.5f
        );
    }

    protected void resizeCentered(Image image, Texture texture, float screenWidth, float screenHeight) {
        float maxWidth = screenWidth * 0.9f;
        float maxHeight = screenHeight * 0.9f;
        float aspect = texture.getWidth() / (float) texture.getHeight();

        float width = texture.getWidth();
        float height = texture.getHeight();

        if (width > maxWidth) {
            width = maxWidth;
            height = width / aspect;
        }
        if (height > maxHeight) {
            height = maxHeight;
            width = height * aspect;
        }

        image.setSize(width, height);
        image.setPosition((screenWidth - width) / 2f, (screenHeight - height) / 2f);
    }

    public void show() {
        stage.addActor(background);
    }

    public void remove() {
        background.remove();
    }

    public boolean isVisible() {
        return background.hasParent();
    }
}
