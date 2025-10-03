package com.gdx.game.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.input.GestureDetector;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.gdx.game.DetectiveGame;
import com.gdx.game.ui.popup.NotePopup;
import com.gdx.game.ui.popup.SettingsPopup;
import com.gdx.game.utils.FadeTransition;
import com.gdx.game.utils.MapInputController;

public class MapScreen implements Screen {
    private final DetectiveGame game;
    private final FadeTransition transition;

    private final OrthographicCamera camera;
    private final ScreenViewport viewport;
    private final Stage stage;

    private final Texture mapTexture;
    private float drawWidth, drawHeight;

    private final MapInputController inputController;

    private NotePopup notePopup;
    private SettingsPopup settingsPopup;

    private final Image notesButton;
    private final Image settingsButton;

    public MapScreen(DetectiveGame game, FadeTransition transition) {
        this.game = game;
        this.transition = transition;

        mapTexture = new Texture("img.png");

        camera = new OrthographicCamera();
        viewport = new ScreenViewport(camera);
        stage = new Stage(new ScreenViewport(), game.batch);

        inputController = new MapInputController(camera, viewport);
        GestureDetector gestureDetector = new GestureDetector(inputController);
        Gdx.input.setInputProcessor(new InputMultiplexer(stage, gestureDetector, inputController));

        notesButton = createNotesButton();
        settingsButton = createSettingsButton();

        stage.addActor(notesButton);
        stage.addActor(settingsButton);

        resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
    }

    private Image createNotesButton() {
        return game.getButtonFactory().createButton(
            "menu/note/note_icon.png", 64, 64,
            () -> {
                if (notePopup == null) {
                    notePopup = new NotePopup(stage,
                        new Skin(Gdx.files.internal("ui/uiskin.json")), game);
                }
                notePopup.show();
            });
    }

    private Image createSettingsButton() {
        return game.getButtonFactory().createButton(
            "menu/settings/settings_btn.png", 64, 64,
            () -> {
                if (settingsPopup == null) {
                    settingsPopup = new SettingsPopup(stage, "menu/settings/settings.png", game, transition);
                }
                settingsPopup.show();
            });
    }

    private void resizeButton(Image button, float targetHeight, float x, float y) {
        float aspect = button.getDrawable().getMinWidth() / button.getDrawable().getMinHeight();
        button.setSize(targetHeight * aspect, targetHeight);
        button.setPosition(x, y);
    }

    @Override
    public void render(float delta) {
        ScreenUtils.clear(0, 0, 0, 1);

        inputController.handleKeyboard(delta);
        camera.update();

        game.batch.setProjectionMatrix(camera.combined);
        game.batch.begin();
        game.batch.draw(mapTexture, 0, 0, drawWidth, drawHeight);
        game.batch.end();

        stage.act(delta);
        stage.draw();

        transition.update(delta);
        transition.render();
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
        stage.getViewport().update(width, height, true);

        float scaleX = viewport.getWorldWidth() / mapTexture.getWidth();
        float scaleY = viewport.getWorldHeight() / mapTexture.getHeight();
        float baseScale = Math.max(1f, Math.max(scaleX, scaleY));

        drawWidth = mapTexture.getWidth() * baseScale;
        drawHeight = mapTexture.getHeight() * baseScale;

        camera.position.set(drawWidth / 2f, drawHeight / 2f, 0);
        camera.update();

        inputController.setMapSize(drawWidth, drawHeight);

        float targetHeight = height * 0.12f;
        resizeButton(notesButton, targetHeight, 10,
            stage.getViewport().getWorldHeight() - notesButton.getHeight() - 10);

        resizeButton(settingsButton, targetHeight,
            stage.getViewport().getWorldWidth() - settingsButton.getWidth() - 10,
            stage.getViewport().getWorldHeight() - settingsButton.getHeight() - 10);

        if (notePopup != null) notePopup.resize(width, height);
        if (settingsPopup != null) settingsPopup.resize(width, height);
    }

    @Override
    public void dispose() {
        mapTexture.dispose();
        stage.dispose();
        if (notePopup != null) notePopup.dispose();
        if (settingsPopup != null) settingsPopup.dispose();
    }

    @Override public void show() {}
    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}
}
