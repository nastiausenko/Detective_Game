package com.gdx.game.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.gdx.game.DetectiveGame;
import com.gdx.game.utils.FadeTransition;

public class MenuScreen implements Screen {
    private final DetectiveGame game;
    private final OrthographicCamera camera;
    private final ScreenViewport viewport;
    private final Stage stage;

    private final Texture backgroundTexture;
    private final Texture startBtnTexture;

    private final Image background;
    private final Image startBtn;

    private final FadeTransition transition;

    public MenuScreen(DetectiveGame game) {
        this.game = game;
        this.transition = new FadeTransition();

        camera = new OrthographicCamera();
        viewport = new ScreenViewport(camera);
        viewport.apply();

        stage = new Stage(viewport, game.batch);

        backgroundTexture = new Texture("background.png");
        startBtnTexture = new Texture("start_btn.png");

        background = new Image(backgroundTexture);
        background.setFillParent(true);
        stage.addActor(background);

        startBtn = new Image(startBtnTexture);
        startBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (!transition.isTransitioning()) {
                    transition.startFadeOut(0.7f, () -> {
                        game.setScreen(new MapScreen(game, transition));
                        transition.startFadeIn(0.7f);
                    });
                }
            }
        });

        stage.addActor(startBtn);
    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(stage);
    }

    @Override
    public void render(float delta) {
        ScreenUtils.clear(0, 0, 0, 1);

        stage.act(delta);
        stage.draw();

        transition.update(delta);
        transition.render();
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);

        float targetHeight = height*1f;
        float aspect = startBtn.getDrawable().getMinWidth() / startBtn.getDrawable().getMinHeight();
        startBtn.setSize(targetHeight * aspect, targetHeight);
        startBtn.setPosition(
            (viewport.getWorldWidth() - startBtn.getWidth()) / 2f,
            viewport.getWorldHeight() * 0.2f
        );
    }

    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}

    @Override
    public void dispose() {
        stage.dispose();
        backgroundTexture.dispose();
        startBtnTexture.dispose();
        transition.dispose();
    }
}
