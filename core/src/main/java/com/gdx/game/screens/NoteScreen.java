package com.gdx.game.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.gdx.game.DetectiveGame;

public class NoteScreen implements Screen {
    private final Texture noteTexture;
    private final Stage stage;

    public NoteScreen(DetectiveGame game) {
        this.noteTexture = new Texture("menu/note_icon.png");
        this.stage = new Stage(new ScreenViewport(), game.batch);

        Image notesImage = new Image(noteTexture);
        notesImage.setSize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        stage.addActor(notesImage);

        notesImage.addListener(event -> {
            if (event.toString().equals("touchDown")) {
                game.setScreen(new MapScreen(game));
                return true;
            }
            return false;
        });
    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(stage);
    }

    @Override
    public void render(float v) {
        ScreenUtils.clear(0.1f, 0.1f, 0.1f, 1);
        stage.act(v);
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    @Override
    public void pause() {

    }

    @Override
    public void resume() {

    }

    @Override
    public void hide() {

    }

    @Override
    public void dispose() {
        noteTexture.dispose();
        stage.dispose();
    }
}
