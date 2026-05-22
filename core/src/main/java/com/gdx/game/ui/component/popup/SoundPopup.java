package com.gdx.game.ui.component.popup;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.gdx.game.DetectiveGame;
import com.gdx.game.infrastructure.Assets;
import com.gdx.game.infrastructure.UiLayout;
import com.gdx.game.infrastructure.UiLayoutProfile;
import com.gdx.game.utils.ScreenUtilsHelper;

public class SoundPopup extends AbstractPopup {
    private final Texture popupTexture;
    private final Texture uncheckedTexture;

    private final Image popupImage;
    private final Image musicToggle;
    private final Image soundEffectsToggle;
    private final Image closeBtn;

    public SoundPopup(Stage stage, DetectiveGame game) {
        super(stage);

        popupTexture = new Texture(Assets.SOUND_SETTINGS_POPUP);
        uncheckedTexture = new Texture(Assets.SOUND_TOGGLE_UNCHECKED);

        popupImage = new Image(popupTexture);
        popupImage.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                event.stop();
            }
        });

        musicToggle = new Image(uncheckedTexture);
        soundEffectsToggle = new Image(uncheckedTexture);
        closeBtn = game.getButtonFactory().createButton(Assets.CLOSE_BUTTON, 64, 64, this::remove);

        resize(stage.getViewport().getWorldWidth(), stage.getViewport().getWorldHeight());
    }

    public void resize(float screenWidth, float screenHeight) {
        UiLayoutProfile profile = UiLayout.current(screenWidth, screenHeight);
        resizeCentered(popupImage, popupTexture, screenWidth, screenHeight);

        float toggleWidth = popupImage.getWidth() * (39f / 689f);
        float toggleHeight = popupImage.getHeight() * (41f / 658f);
        float toggleX = popupImage.getX() + popupImage.getWidth() * (483f / 689f);

        musicToggle.setSize(toggleWidth, toggleHeight);
        musicToggle.setPosition(
            toggleX,
            popupImage.getY() + popupImage.getHeight() * (403f / 658f)
        );

        soundEffectsToggle.setSize(toggleWidth, toggleHeight);
        soundEffectsToggle.setPosition(
            toggleX,
            popupImage.getY() + popupImage.getHeight() * (281f / 658f)
        );

        float targetHeight = screenHeight * profile.getPopupButtonHeightRatio();
        float margin = profile.scale(10f);

        ScreenUtilsHelper.scaleButton(closeBtn, targetHeight, stage);
        closeBtn.setPosition(
            popupImage.getX() + popupImage.getWidth() - closeBtn.getWidth() * 0.55f,
            popupImage.getY() + popupImage.getHeight() - closeBtn.getHeight() * 0.55f - margin
        );
    }

    @Override
    public void show() {
        super.show();
        stage.addActor(popupImage);
        stage.addActor(musicToggle);
        stage.addActor(soundEffectsToggle);
        stage.addActor(closeBtn);

        resize(stage.getViewport().getWorldWidth(), stage.getViewport().getWorldHeight());
    }

    @Override
    public void remove() {
        super.remove();
        popupImage.remove();
        musicToggle.remove();
        soundEffectsToggle.remove();
        closeBtn.remove();
    }

    public void dispose() {
        popupTexture.dispose();
        uncheckedTexture.dispose();
    }
}
