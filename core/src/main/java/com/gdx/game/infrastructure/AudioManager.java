package com.gdx.game.infrastructure;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.math.MathUtils;

public class AudioManager {
    private static final String PREFS_NAME = "audio_settings";
    private static final String PREF_MUSIC_ENABLED = "musicEnabled";
    private static final String PREF_SOUND_EFFECTS_ENABLED = "soundEffectsEnabled";
    private static final String PREF_MASTER_VOLUME = "masterVolume";
    private static final float MUSIC_VOLUME = 0.35f;
    private static final float BUTTON_VOLUME = 0.45f;
    private static final float TRANSITION_VOLUME = 0.55f;

    private Sound buttonClick;
    private Sound homeTransition;
    private Sound shopTransition;
    private Music currentAmbience;
    private String currentAmbiencePath;
    private boolean currentAmbienceLooping;
    private boolean musicEnabled = true;
    private boolean soundEffectsEnabled = true;
    private float masterVolume = 1f;

    public void load() {
        Preferences prefs = Gdx.app.getPreferences(PREFS_NAME);
        musicEnabled = prefs.getBoolean(PREF_MUSIC_ENABLED, true);
        soundEffectsEnabled = prefs.getBoolean(PREF_SOUND_EFFECTS_ENABLED, true);
        masterVolume = MathUtils.clamp(prefs.getFloat(PREF_MASTER_VOLUME, 1f), 0f, 1f);

        buttonClick = Gdx.audio.newSound(Gdx.files.internal(Assets.SOUND_BUTTON));
        homeTransition = Gdx.audio.newSound(Gdx.files.internal(Assets.SOUND_DOOR));
        shopTransition = Gdx.audio.newSound(Gdx.files.internal(Assets.SOUND_DOORBELL));
    }

    public void playButtonClick() {
        if (!soundEffectsEnabled || masterVolume <= 0f) return;

        if (buttonClick != null) {
            buttonClick.play(BUTTON_VOLUME * masterVolume);
        }
    }

    public void playAmbience(String path) {
        playAmbience(path, true);
    }

    public void playAmbience(String path, boolean looping) {
        if (path == null) return;
        if (musicEnabled && looping && currentAmbienceLooping && path.equals(currentAmbiencePath)) {
            return;
        }

        stopCurrentAmbience();

        currentAmbiencePath = path;
        currentAmbienceLooping = looping;
        if (!musicEnabled) return;

        playCurrentAmbience();
    }

    public boolean isMusicEnabled() {
        return musicEnabled;
    }

    public void setMusicEnabled(boolean enabled) {
        if (musicEnabled == enabled) return;

        musicEnabled = enabled;
        saveSettings();

        if (musicEnabled) {
            playCurrentAmbience();
        } else {
            stopCurrentAmbience();
        }
    }

    public boolean isSoundEffectsEnabled() {
        return soundEffectsEnabled;
    }

    public void setSoundEffectsEnabled(boolean enabled) {
        if (soundEffectsEnabled == enabled) return;

        soundEffectsEnabled = enabled;
        saveSettings();

        if (!soundEffectsEnabled) {
            stopSoundEffects();
        }
    }

    public float getMasterVolume() {
        return masterVolume;
    }

    public void setMasterVolume(float volume) {
        float clampedVolume = MathUtils.clamp(volume, 0f, 1f);
        if (Math.abs(masterVolume - clampedVolume) < 0.001f) return;

        masterVolume = clampedVolume;
        saveSettings();

        if (currentAmbience != null) {
            currentAmbience.setVolume(MUSIC_VOLUME * masterVolume);
        }
    }

    private void playCurrentAmbience() {
        if (currentAmbiencePath == null) return;

        stopCurrentAmbience();
        currentAmbience = Gdx.audio.newMusic(Gdx.files.internal(currentAmbiencePath));
        currentAmbience.setLooping(currentAmbienceLooping);
        currentAmbience.setVolume(MUSIC_VOLUME * masterVolume);
        currentAmbience.play();
    }

    public void stopAmbience() {
        stopCurrentAmbience();
        currentAmbiencePath = null;
        currentAmbienceLooping = false;
    }

    public void playLocationTransition(String buildingId) {
        if (buildingId == null || buildingId.isEmpty()) return;

        if ("shop".equals(buildingId) || "cafe".equals(buildingId)) {
            playOneShot(shopTransition);
        } else {
            playOneShot(homeTransition);
        }
    }

    private void playOneShot(Sound sound) {
        if (!soundEffectsEnabled || masterVolume <= 0f) return;

        if (sound != null) {
            sound.play(TRANSITION_VOLUME * masterVolume);
        }
    }

    private void stopCurrentAmbience() {
        if (currentAmbience == null) return;

        currentAmbience.stop();
        currentAmbience.dispose();
        currentAmbience = null;
    }

    private void stopSoundEffects() {
        if (buttonClick != null) buttonClick.stop();
        if (homeTransition != null) homeTransition.stop();
        if (shopTransition != null) shopTransition.stop();
    }

    private void saveSettings() {
        Preferences prefs = Gdx.app.getPreferences(PREFS_NAME);
        prefs.putBoolean(PREF_MUSIC_ENABLED, musicEnabled);
        prefs.putBoolean(PREF_SOUND_EFFECTS_ENABLED, soundEffectsEnabled);
        prefs.putFloat(PREF_MASTER_VOLUME, masterVolume);
        prefs.flush();
    }

    public void dispose() {
        if (buttonClick != null) buttonClick.dispose();
        if (homeTransition != null) homeTransition.dispose();
        if (shopTransition != null) shopTransition.dispose();
        stopCurrentAmbience();
    }
}
