package com.gdx.game.utils;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.input.GestureDetector;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.viewport.Viewport;

public class MapInputController extends InputAdapter implements GestureDetector.GestureListener {
    private final OrthographicCamera camera;
    private final Viewport viewport;

    private float drawWidth, drawHeight;
    private float lastZoom = 1f;

    private static final float MIN_ZOOM = 0.5f;
    private static final float MAX_ZOOM = 3f;
    private static final float KEYBOARD_SPEED = 500f;
    private static final float SCROLL_SPEED = 50f;

    public MapInputController(OrthographicCamera camera, Viewport viewport) {
        this.camera = camera;
        this.viewport = viewport;
    }

    public void setMapSize(float width, float height) {
        this.drawWidth = width;
        this.drawHeight = height;
    }

    private void clampCamera() {
        float scaledWidth = drawWidth * camera.zoom;
        float scaledHeight = drawHeight * camera.zoom;

        float halfW = Math.min(scaledWidth, viewport.getWorldWidth()) / 2f;
        float halfH = Math.min(scaledHeight, viewport.getWorldHeight()) / 2f;

        camera.position.x = MathUtils.clamp(camera.position.x, halfW, scaledWidth - halfW);
        camera.position.y = MathUtils.clamp(camera.position.y, halfH, scaledHeight - halfH);
    }

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
        camera.zoom = MathUtils.clamp(lastZoom * ratio, MIN_ZOOM, MAX_ZOOM);
        clampCamera();
        return true;
    }

    @Override public void pinchStop() { lastZoom = camera.zoom; }
    @Override public boolean pinch(Vector2 v1, Vector2 v2, Vector2 v3, Vector2 v4) { return false; }
    @Override public boolean touchDown(float x, float y, int pointer, int button) { return false; }
    @Override public boolean tap(float x, float y, int count, int button) { return false; }
    @Override public boolean longPress(float x, float y) { return false; }
    @Override public boolean fling(float velocityX, float velocityY, int button) { return false; }
    @Override public boolean panStop(float x, float y, int pointer, int button) { return false; }

    @Override
    public boolean scrolled(float amountX, float amountY) {
        float moveSpeed = SCROLL_SPEED * camera.zoom;
        camera.translate(amountX * moveSpeed, amountY * moveSpeed);
        clampCamera();
        return true;
    }

    public void handleKeyboard(float delta) {
        float move = KEYBOARD_SPEED * delta * camera.zoom;

        if (Gdx.input.isKeyPressed(Input.Keys.LEFT))  camera.translate(-move, 0);
        if (Gdx.input.isKeyPressed(Input.Keys.RIGHT)) camera.translate(move, 0);
        if (Gdx.input.isKeyPressed(Input.Keys.UP))    camera.translate(0, move);
        if (Gdx.input.isKeyPressed(Input.Keys.DOWN))  camera.translate(0, -move);

        clampCamera();
    }
}
