package com.gdx.game.infrastructure;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;

public class AudioManager {
    private Sound buttonClick;
    private Sound homeTransition;
    private Sound shopTransition;
    private Music currentAmbience;
    private String currentAmbiencePath;
    private boolean currentAmbienceLooping;

    public void load() {
        buttonClick = Gdx.audio.newSound(Gdx.files.internal(Assets.SOUND_BUTTON));
        homeTransition = Gdx.audio.newSound(Gdx.files.internal(Assets.SOUND_DOOR));
        shopTransition = Gdx.audio.newSound(Gdx.files.internal(Assets.SOUND_DOORBELL));
    }

    public void playButtonClick() {
        if (buttonClick != null) {
            buttonClick.play(0.45f);
        }
    }

    public void playAmbience(String path) {
        playAmbience(path, true);
    }

    public void playAmbience(String path, boolean looping) {
        if (path == null) return;
        if (looping && currentAmbienceLooping && path.equals(currentAmbiencePath)) {
            return;
        }

        if (currentAmbience != null) {
            currentAmbience.stop();
            currentAmbience.dispose();
        }

        currentAmbiencePath = path;
        currentAmbienceLooping = looping;
        currentAmbience = Gdx.audio.newMusic(Gdx.files.internal(path));
        currentAmbience.setLooping(looping);
        currentAmbience.setVolume(0.35f);
        currentAmbience.play();
    }

    public void stopAmbience() {
        if (currentAmbience == null) return;

        currentAmbience.stop();
        currentAmbience.dispose();
        currentAmbience = null;
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
        if (sound != null) {
            sound.play(0.55f);
        }
    }

    public void dispose() {
        if (buttonClick != null) buttonClick.dispose();
        if (homeTransition != null) homeTransition.dispose();
        if (shopTransition != null) shopTransition.dispose();
        if (currentAmbience != null) currentAmbience.dispose();
    }
}
