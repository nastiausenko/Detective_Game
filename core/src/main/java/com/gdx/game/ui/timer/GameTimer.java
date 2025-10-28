package com.gdx.game.ui.timer;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.utils.Align;
import com.gdx.game.utils.Assets;
import com.gdx.game.utils.ScreenUtilsHelper;

public class GameTimer {
    private final Stage stage;

    private final Image timerBackground;
    private final Label countdownLabel;
    private final Label gameTimeLabel;
    private final BitmapFont font;

    private final Preferences prefs;

    private float elapsedRealTime = 0f;
    private final float totalRealSeconds;
    private boolean timeOver = false;
    private boolean paused = false;

    public GameTimer(Stage stage, float totalRealSeconds) {
        this.stage = stage;
        this.totalRealSeconds = totalRealSeconds;
        prefs = Gdx.app.getPreferences("game_timer");

        elapsedRealTime = prefs.getFloat("elapsedTime", 0f);

        timerBackground = new Image(new Texture(Assets.TIMER));
        stage.addActor(timerBackground);

        font = new BitmapFont(Gdx.files.internal("fonts/8bold.fnt"));

        Label.LabelStyle labelStyle = new Label.LabelStyle(font, new Color(154 / 255f, 109 / 255f, 69 / 255f, 1f));

        countdownLabel = new Label("Time Left: 60:00", labelStyle);
        countdownLabel.setAlignment(Align.center);
        stage.addActor(countdownLabel);

        gameTimeLabel = new Label("Day 1 09:00", labelStyle);
        gameTimeLabel.setAlignment(Align.right);
        stage.addActor(gameTimeLabel);
    }

    // TODO fix game time
    public void update(float delta) {
        if (paused || timeOver) {
            // TODO time over logic
            return;
        }

        elapsedRealTime += delta;
        if (elapsedRealTime >= totalRealSeconds) {
            elapsedRealTime = totalRealSeconds;
            timeOver = true;
        }

        float remaining = totalRealSeconds - elapsedRealTime;
        int minutes = (int) (remaining / 60);
        int seconds = (int) (remaining % 60);
        countdownLabel.setText(String.format("Time Left: %02d:%02d", minutes, seconds));

        float realToGameMinutes = 1.2f;
        int elapsedGameMinutes = (int) (elapsedRealTime * realToGameMinutes);

        int startHour = 9;
        int minutesInDay = elapsedGameMinutes + startHour * 60;

        int day = (minutesInDay / (24 * 60)) + 1;
        int minutesOfCurrentDay = minutesInDay % (24 * 60);
        int hour = minutesOfCurrentDay / 60;
        int minute = minutesOfCurrentDay % 60;

        gameTimeLabel.setText(String.format("Day %d %02d:%02d", day, hour, minute));
    }

    public void setPositions(float targetHeight) {
        float worldWidth = stage.getViewport().getWorldWidth();
        float worldHeight = stage.getViewport().getWorldHeight();
        boolean isMobile = Gdx.app.getType() == Application.ApplicationType.iOS
                || Gdx.app.getType() == Application.ApplicationType.Android;

        float scale;
        if (isMobile) {
            scale = (worldHeight / 1100f) * Gdx.graphics.getDensity() * 0.5f;
        } else {
            scale = worldHeight / 800f;
        }

        font.getData().setScale(scale);

        ScreenUtilsHelper.scaleAndPositionButton(timerBackground, targetHeight, 0, 0);

        timerBackground.setPosition(
                (worldWidth - timerBackground.getWidth()) / 2f,
                worldHeight - timerBackground.getHeight() - 10
        );

        countdownLabel.setSize(timerBackground.getWidth(), timerBackground.getHeight());
        countdownLabel.setPosition(timerBackground.getX(), timerBackground.getY());
        countdownLabel.setAlignment(Align.center);

        // 4️⃣ Game time — у правому нижньому куті
        float padding = 20f;
        gameTimeLabel.pack();
        gameTimeLabel.setPosition(
                worldWidth - gameTimeLabel.getWidth() - padding,
                padding
        );
    }

    public void pause() {
        paused = true;
    }

    public void resume() {
        paused = false;
    }

    public void saveTime() {
        prefs.putFloat("elapsedTime", elapsedRealTime);
        prefs.flush();
    }

    public void reset() {
        elapsedRealTime = 0f;
        timeOver = false;
        paused = false;
    }

    public boolean isTimeOver() {
        return timeOver;
    }
}
