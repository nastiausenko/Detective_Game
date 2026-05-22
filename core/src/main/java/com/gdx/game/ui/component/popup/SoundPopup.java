package com.gdx.game.ui.component.popup;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Cursor;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.gdx.game.DetectiveGame;
import com.gdx.game.infrastructure.AudioManager;
import com.gdx.game.infrastructure.Assets;
import com.gdx.game.infrastructure.UiLayout;
import com.gdx.game.infrastructure.UiLayoutProfile;
import com.gdx.game.utils.ScreenUtilsHelper;

public class SoundPopup extends AbstractPopup {
    private final Texture popupTexture;
    private final Texture checkedTexture;
    private final Texture uncheckedTexture;
    private final TextureRegionDrawable checkedDrawable;
    private final TextureRegionDrawable uncheckedDrawable;

    private final DetectiveGame game;
    private final Image popupImage;
    private final Image musicToggle;
    private final Image soundEffectsToggle;
    private final Image closeBtn;

    public SoundPopup(Stage stage, DetectiveGame game) {
        super(stage);
        this.game = game;

        popupTexture = new Texture(Assets.SOUND_SETTINGS_POPUP);
        checkedTexture = new Texture(Assets.SOUND_TOGGLE_CHECKED);
        uncheckedTexture = new Texture(Assets.SOUND_TOGGLE_UNCHECKED);
        checkedDrawable = new TextureRegionDrawable(new TextureRegion(checkedTexture));
        uncheckedDrawable = new TextureRegionDrawable(new TextureRegion(uncheckedTexture));

        popupImage = new Image(popupTexture);
        popupImage.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                event.stop();
            }
        });

        musicToggle = new Image(uncheckedTexture);
        soundEffectsToggle = new Image(uncheckedTexture);
        addToggleListener(musicToggle, this::toggleMusic);
        addToggleListener(soundEffectsToggle, this::toggleSoundEffects);
        closeBtn = game.getButtonFactory().createButton(Assets.CLOSE_BUTTON, 64, 64, this::remove);

        resize(stage.getViewport().getWorldWidth(), stage.getViewport().getWorldHeight());
    }

    private void addToggleListener(Image toggle, Runnable action) {
        toggle.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                action.run();
                event.stop();
            }

            @Override
            public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                Gdx.graphics.setSystemCursor(Cursor.SystemCursor.Hand);
            }

            @Override
            public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
                Gdx.graphics.setSystemCursor(Cursor.SystemCursor.Arrow);
            }
        });
    }

    private void toggleMusic() {
        AudioManager audioManager = game.getAudioManager();
        if (audioManager == null) return;

        audioManager.playButtonClick();
        audioManager.setMusicEnabled(!audioManager.isMusicEnabled());
        updateToggleState();
    }

    private void toggleSoundEffects() {
        AudioManager audioManager = game.getAudioManager();
        if (audioManager == null) return;

        boolean enableSoundEffects = !audioManager.isSoundEffectsEnabled();
        if (!enableSoundEffects) {
            audioManager.playButtonClick();
        }

        audioManager.setSoundEffectsEnabled(enableSoundEffects);

        if (enableSoundEffects) {
            audioManager.playButtonClick();
        }

        updateToggleState();
    }

    private void updateToggleState() {
        AudioManager audioManager = game.getAudioManager();
        if (audioManager == null) {
            musicToggle.setDrawable(uncheckedDrawable);
            soundEffectsToggle.setDrawable(uncheckedDrawable);
            return;
        }

        musicToggle.setDrawable(audioManager.isMusicEnabled() ? uncheckedDrawable : checkedDrawable);
        soundEffectsToggle.setDrawable(audioManager.isSoundEffectsEnabled() ? uncheckedDrawable : checkedDrawable);
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

        updateToggleState();
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
        checkedTexture.dispose();
        uncheckedTexture.dispose();
    }
}
