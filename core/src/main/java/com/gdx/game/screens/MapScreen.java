package com.gdx.game.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.input.GestureDetector;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.gdx.game.DetectiveGame;
import com.gdx.game.utils.MapInputController;

public class MapScreen implements Screen {
    private final DetectiveGame game;
    private final OrthographicCamera camera;
    private final Texture mapTexture;
    private final ScreenViewport viewport;

    private float drawWidth, drawHeight;
    private final Stage stage;

    private final MapInputController inputController;
    private NotePopup notePopup;
    private final Image notesButton;

    public MapScreen(DetectiveGame game) {
        this.game = game;
        mapTexture = new Texture("img.png");

        camera = new OrthographicCamera();
        viewport = new ScreenViewport(camera);
        viewport.apply();

        camera.position.set(mapTexture.getWidth() / 2f, mapTexture.getHeight() / 2f, 0);
        camera.update();

        stage = new Stage(new ScreenViewport(), game.batch);

        inputController = new MapInputController(camera, viewport);
        GestureDetector gd = new GestureDetector(inputController);

        Gdx.input.setInputProcessor(new InputMultiplexer(stage, gd, inputController));

        notesButton = new Image(new Texture("menu/note/note_icon.png"));
        notesButton.setPosition(10, Gdx.graphics.getHeight() - notesButton.getHeight() - 10);

        notesButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (notePopup == null) {
                    Skin skin = new Skin(Gdx.files.internal("ui/uiskin.json"));
                    notePopup = new NotePopup(stage, skin, "menu/img.png");
                    notePopup.show();
                } else {
                    notePopup.show();
                }
            }
        });

        stage.addActor(notesButton);
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

        float targetHeight = height * 0.15f;
        float aspect = notesButton.getDrawable().getMinWidth() / notesButton.getDrawable().getMinHeight();
        notesButton.setSize(targetHeight * aspect, targetHeight);
        notesButton.setPosition(10, stage.getViewport().getWorldHeight() - notesButton.getHeight() - 10);

        if (notePopup != null) {
            notePopup.resize(width, height);
        }
    }

    @Override
    public void dispose() {
        mapTexture.dispose();
        stage.dispose();
        if (notePopup != null) {
            notePopup.dispose();
        }
    }

    @Override public void show() {}
    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}
}
