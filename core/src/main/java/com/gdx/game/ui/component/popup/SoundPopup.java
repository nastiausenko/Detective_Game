package com.gdx.game.ui.component.popup;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Cursor;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.math.MathUtils;
import com.gdx.game.infrastructure.GameContext;
import com.gdx.game.infrastructure.AudioManager;
import com.gdx.game.infrastructure.Assets;
import com.gdx.game.ui.style.UiLayout;
import com.gdx.game.ui.style.UiLayoutProfile;
import com.gdx.game.render.ScreenUtilsHelper;

public class SoundPopup extends AbstractPopup {
    private final Texture popupTexture;
    private final Texture checkedTexture;
    private final Texture uncheckedTexture;
    private final Texture sliderCoverTexture;
    private final Texture sliderTrackTexture;
    private final Texture sliderFillTexture;
    private final Texture sliderKnobTexture;
    private final TextureRegionDrawable checkedDrawable;
    private final TextureRegionDrawable uncheckedDrawable;

    private final GameContext game;
    private final Image popupImage;
    private final Image musicToggle;
    private final Image soundEffectsToggle;
    private final Image volumeSliderCover;
    private final Image volumeSliderTrack;
    private final Image volumeSliderFill;
    private final Image volumeSliderKnob;
    private final Image closeBtn;
    private float sliderTrackX;
    private float sliderTrackWidth;
    private float sliderKnobWidth;

    public SoundPopup(Stage stage, GameContext game) {
        super(stage);
        this.game = game;

        popupTexture = new Texture(Assets.SOUND_SETTINGS_POPUP);
        checkedTexture = new Texture(Assets.SOUND_TOGGLE_CHECKED);
        uncheckedTexture = new Texture(Assets.SOUND_TOGGLE_UNCHECKED);
        sliderCoverTexture = createSolidTexture(0.08f, 0.03f, 0.01f, 1f);
        sliderTrackTexture = createSolidTexture(0.24f, 0.11f, 0.03f, 1f);
        sliderFillTexture = createSolidTexture(0.66f, 0.40f, 0.12f, 1f);
        sliderKnobTexture = createKnobTexture();
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

        volumeSliderCover = new Image(sliderCoverTexture);
        volumeSliderTrack = new Image(sliderTrackTexture);
        volumeSliderFill = new Image(sliderFillTexture);
        volumeSliderKnob = new Image(sliderKnobTexture);
        addVolumeSliderListener(volumeSliderCover);
        addVolumeSliderListener(volumeSliderTrack);
        addVolumeSliderListener(volumeSliderFill);
        addVolumeSliderListener(volumeSliderKnob);

        closeBtn = game.buttonFactory.createButton(Assets.CLOSE_BUTTON, 64, 64, this::remove);

        resize(stage.getViewport().getWorldWidth(), stage.getViewport().getWorldHeight());
    }

    private Texture createSolidTexture(float r, float g, float b, float a) {
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(r, g, b, a);
        pixmap.fill();

        Texture texture = new Texture(pixmap);
        texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        pixmap.dispose();
        return texture;
    }

    private Texture createKnobTexture() {
        Pixmap pixmap = new Pixmap(28, 42, Pixmap.Format.RGBA8888);
        pixmap.setColor(0.10f, 0.04f, 0.01f, 1f);
        pixmap.fillRectangle(0, 0, 28, 42);
        pixmap.setColor(0.36f, 0.16f, 0.04f, 1f);
        pixmap.fillRectangle(3, 3, 22, 36);
        pixmap.setColor(0.78f, 0.48f, 0.14f, 1f);
        pixmap.fillRectangle(6, 6, 16, 30);
        pixmap.setColor(0.18f, 0.07f, 0.02f, 1f);
        pixmap.drawRectangle(3, 3, 22, 36);

        Texture texture = new Texture(pixmap);
        texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        pixmap.dispose();
        return texture;
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

    private void addVolumeSliderListener(Image actor) {
        actor.addListener(new InputListener() {
            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                updateVolumeFromStageX(event.getStageX());
                event.stop();
                return true;
            }

            @Override
            public void touchDragged(InputEvent event, float x, float y, int pointer) {
                updateVolumeFromStageX(event.getStageX());
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

    private void updateVolumeFromStageX(float stageX) {
        if (sliderTrackWidth <= 0f) return;

        float volume = (stageX - sliderTrackX) / sliderTrackWidth;
        setVolumeValue(volume, true);
    }

    private void setVolumeValue(float value, boolean updateAudio) {
        float volume = MathUtils.clamp(value, 0f, 1f);

        volumeSliderFill.setWidth(sliderTrackWidth * volume);
        volumeSliderKnob.setPosition(
            sliderTrackX + sliderTrackWidth * volume - sliderKnobWidth / 2f,
            volumeSliderCover.getY() + (volumeSliderCover.getHeight() - volumeSliderKnob.getHeight()) / 2f
        );

        if (!updateAudio) return;

        AudioManager audioManager = game.audioManager;
        if (audioManager != null) {
            audioManager.setMasterVolume(volume);
        }
    }

    private float currentVolume() {
        AudioManager audioManager = game.audioManager;
        return audioManager == null ? 1f : audioManager.getMasterVolume();
    }

    private void toggleMusic() {
        AudioManager audioManager = game.audioManager;
        if (audioManager == null) return;

        audioManager.playButtonClick();
        audioManager.setMusicEnabled(!audioManager.isMusicEnabled());
        updateToggleState();
    }

    private void toggleSoundEffects() {
        AudioManager audioManager = game.audioManager;
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
        AudioManager audioManager = game.audioManager;
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

        float sliderCoverX = popupImage.getX() + popupImage.getWidth() * (145f / 689f);
        float sliderCoverY = popupImage.getY() + popupImage.getHeight() * (108f / 658f);
        float sliderCoverWidth = popupImage.getWidth() * (382f / 689f);
        float sliderCoverHeight = popupImage.getHeight() * (56f / 658f);
        float sliderTrackInset = sliderCoverWidth * 0.05f;
        float sliderTrackHeight = Math.max(4f, sliderCoverHeight * 0.18f);
        float sliderKnobHeight = sliderCoverHeight * 0.92f;

        sliderKnobWidth = sliderKnobHeight * (28f / 42f);
        sliderTrackX = sliderCoverX + sliderTrackInset + sliderKnobWidth / 2f;
        sliderTrackWidth = sliderCoverWidth - sliderTrackInset * 2f - sliderKnobWidth;

        volumeSliderCover.setSize(sliderCoverWidth, sliderCoverHeight);
        volumeSliderCover.setPosition(sliderCoverX, sliderCoverY);

        volumeSliderTrack.setSize(sliderTrackWidth, sliderTrackHeight);
        volumeSliderTrack.setPosition(
            sliderTrackX,
            sliderCoverY + (sliderCoverHeight - sliderTrackHeight) / 2f
        );

        volumeSliderFill.setHeight(sliderTrackHeight);
        volumeSliderFill.setPosition(volumeSliderTrack.getX(), volumeSliderTrack.getY());

        volumeSliderKnob.setSize(sliderKnobWidth, sliderKnobHeight);
        setVolumeValue(currentVolume(), false);

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
        addPopupActors(
            popupImage,
            musicToggle,
            soundEffectsToggle,
            volumeSliderCover,
            volumeSliderTrack,
            volumeSliderFill,
            volumeSliderKnob,
            closeBtn
        );

        updateToggleState();
        resize(stage.getViewport().getWorldWidth(), stage.getViewport().getWorldHeight());
    }

    @Override
    public void remove() {
        super.remove();
        removePopupActors(
            popupImage,
            musicToggle,
            soundEffectsToggle,
            volumeSliderCover,
            volumeSliderTrack,
            volumeSliderFill,
            volumeSliderKnob,
            closeBtn
        );
    }

    public void dispose() {
        popupTexture.dispose();
        checkedTexture.dispose();
        uncheckedTexture.dispose();
        sliderCoverTexture.dispose();
        sliderTrackTexture.dispose();
        sliderFillTexture.dispose();
        sliderKnobTexture.dispose();
    }
}
