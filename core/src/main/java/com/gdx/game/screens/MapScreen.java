package com.gdx.game.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.input.GestureDetector;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.InputMultiplexer;
import com.gdx.game.DetectiveGame;

public class MapScreen implements Screen, GestureDetector.GestureListener {
    private final DetectiveGame game;
    private final OrthographicCamera camera;
    private final Texture mapTexture;
    private final ScreenViewport viewport;

    private float lastZoom = 1f;
    private float drawWidth, drawHeight;

    private final Stage stage;

    public MapScreen(DetectiveGame game) {
        this.game = game;
        mapTexture = new Texture("map.png");

        camera = new OrthographicCamera();
        viewport = new ScreenViewport(camera);
        viewport.apply();

        camera.position.set(mapTexture.getWidth() / 2f, mapTexture.getHeight() / 2f, 0);
        camera.update();

        GestureDetector gd = new GestureDetector(this);
        stage = new Stage(new ScreenViewport(), game.batch);
        initInput(stage, gd);

        Texture icon = new Texture("menu/img.png");
        Image notesButton = new Image(icon);

        float scale = 1f;
        notesButton.setSize(notesButton.getWidth() * scale, notesButton.getHeight() * scale);

        notesButton.setPosition(10, Gdx.graphics.getHeight() - notesButton.getHeight() -10);

        // TODO: пізніше сюди додати listener для анімації та відкриття нотаток
        stage.addActor(notesButton);
    }

    @Override
    public void show() {}

    @Override
    public void render(float delta) {
        ScreenUtils.clear(0, 0, 0, 1);
        handleInput(delta);

        camera.update();
        game.batch.setProjectionMatrix(camera.combined);

        game.batch.begin();
        game.batch.draw(mapTexture, 0, 0, drawWidth, drawHeight);
        game.batch.end();

        stage.act(delta);
        stage.draw();
    }

    private void handleInput(float delta) {
        float moveSpeed = 500 * delta;

        if (Gdx.input.isKeyPressed(com.badlogic.gdx.Input.Keys.LEFT)) {
            camera.translate(-moveSpeed * camera.zoom, 0);
        }
        if (Gdx.input.isKeyPressed(com.badlogic.gdx.Input.Keys.RIGHT)) {
            camera.translate(moveSpeed * camera.zoom, 0);
        }
        if (Gdx.input.isKeyPressed(com.badlogic.gdx.Input.Keys.UP)) {
            camera.translate(0, moveSpeed * camera.zoom);
        }
        if (Gdx.input.isKeyPressed(com.badlogic.gdx.Input.Keys.DOWN)) {
            camera.translate(0, -moveSpeed * camera.zoom);
        }

        clampCamera();
    }

    private void initInput(Stage stage, GestureDetector gd) {
        InputAdapter scrollHandler = new InputAdapter() {
            @Override
            public boolean scrolled(float amountX, float amountY) {
                float moveSpeed = 10 * camera.zoom;
                camera.translate(amountX * moveSpeed, amountY * moveSpeed);
                clampCamera();
                return true;
            }
        };

        Gdx.input.setInputProcessor(new InputMultiplexer(stage, gd, scrollHandler));
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
        stage.getViewport().update(width, height, true);

        float scaleX = viewport.getWorldWidth() / mapTexture.getWidth();
        float scaleY = viewport.getWorldHeight() / mapTexture.getHeight();

        float baseScale = 1f;
        if (viewport.getWorldWidth() > mapTexture.getWidth() || viewport.getWorldHeight() > mapTexture.getHeight()) {
            baseScale = Math.max(scaleX, scaleY);
        }

        drawWidth = mapTexture.getWidth() * baseScale;
        drawHeight = mapTexture.getHeight() * baseScale;

        camera.position.set(drawWidth / 2f, drawHeight / 2f, 0);
        camera.update();

        lastZoom = 1f;

        Image notesButton = (Image) stage.getActors().first();

        float targetHeight = height * 1f;
        float aspect = notesButton.getDrawable().getMinWidth() / notesButton.getDrawable().getMinHeight();
        float targetWidth = targetHeight * aspect;

        notesButton.setSize(targetWidth, targetHeight);

        notesButton.setPosition(10, stage.getViewport().getWorldHeight() -
                stage.getActors().first().getHeight() -10);
    }

    private void clampCamera() {
        float scaledWidth = drawWidth * camera.zoom;
        float scaledHeight = drawHeight * camera.zoom;

        float halfW = Math.min(scaledWidth, viewport.getWorldWidth()) / 2f;
        float halfH = Math.min(scaledHeight, viewport.getWorldHeight()) / 2f;

        camera.position.x = MathUtils.clamp(camera.position.x, halfW, scaledWidth - halfW);
        camera.position.y = MathUtils.clamp(camera.position.y, halfH, scaledHeight - halfH);
    }

    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}
    @Override public void dispose() {
        mapTexture.dispose();
        stage.dispose();
    }

    // --- GestureDetector callbacks ---
    @Override
    public boolean pan(float x, float y, float deltaX, float deltaY) {
        camera.translate(-deltaX * camera.zoom, deltaY * camera.zoom);
        clampCamera();
        return true;
    }

    @Override
    public boolean zoom(float initialDistance, float distance) {
        if (distance == 0) return false;

        float ratio = initialDistance / distance;
        camera.zoom = MathUtils.clamp(lastZoom * ratio, 0.5f, 3f);
        clampCamera();
        return true;
    }

    @Override public boolean pinch(Vector2 v1, Vector2 v2, Vector2 v3, Vector2 v4) { return false; }
    @Override public void pinchStop() { lastZoom = camera.zoom; }
    @Override public boolean touchDown(float x, float y, int pointer, int button) { return false; }
    @Override public boolean tap(float x, float y, int count, int button) { return false; }
    @Override public boolean longPress(float x, float y) { return false; }
    @Override public boolean fling(float velocityX, float velocityY, int button) { return false; }
    @Override public boolean panStop(float x, float y, int pointer, int button) { return false; }
}
