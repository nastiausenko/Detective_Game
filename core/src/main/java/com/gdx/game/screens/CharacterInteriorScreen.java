package com.gdx.game.screens;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.input.GestureDetector;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.gdx.game.DetectiveGame;
import com.gdx.game.utils.TiledTextureHelper;

public class CharacterInteriorScreen implements Screen, GestureDetector.GestureListener {

    private final DetectiveGame game;
    private final Texture background;
    private Image backButton;
    private final String buildingId;
    private final String characterName;
    private final TiledTextureHelper tiledHelper;
    private final Texture characterTexture;

    private final OrthographicCamera camera;
    private final ScreenViewport viewport;

    private float drawWidth, drawHeight;
    private float imageWidth, imageHeight;

    public CharacterInteriorScreen(DetectiveGame game, String backgroundPath, String buildingId, String characterName, String fullBody) {
        this.game = game;
        this.background = new Texture(backgroundPath);
        this.buildingId = buildingId;
        this.characterName = characterName;
        this.characterTexture = new Texture(fullBody);

        camera = new OrthographicCamera();
        viewport = new ScreenViewport(camera);
        tiledHelper = new TiledTextureHelper(background, 256);
    }

    @Override
    public void show() {
        game.overlay.setVisible(true);
        game.overlay.setInInterior(true);

        imageWidth = background.getWidth();
        imageHeight = background.getHeight();

        Gdx.input.setInputProcessor(new InputMultiplexer(
            game.overlay.getStage(),
            new GestureDetector(this)
        ));
    }

    @Override
    public void render(float delta) {
        boolean isIOS = Gdx.app.getType() == Application.ApplicationType.iOS;
        ScreenUtils.clear(0, 0, 0, 1);

        camera.update();
        game.batch.setProjectionMatrix(camera.combined);

        if (isIOS) {
            tiledHelper.renderTiled(game.batch, drawWidth, drawHeight);
        } else {
            game.batch.begin();
            game.batch.draw(background, 0, 0, drawWidth, drawHeight);
            game.batch.end();
        }

        game.batch.begin();

        float charWidth = characterTexture.getWidth();
        float charHeight = characterTexture.getHeight();

        float worldHeight = viewport.getWorldHeight();
        float scale = (worldHeight * 0.8f) / charHeight;
        float drawW = charWidth * scale;
        float drawH = charHeight * scale;

        float camCenterX = camera.position.x;
        float camCenterY = camera.position.y;

        float y = camCenterY - (viewport.getWorldHeight() / 2f) + 20f;
        float x = camCenterX - (drawW / 2f);

        game.batch.draw(characterTexture, x, y, drawW, drawH);

        game.batch.end();

        game.overlay.render(delta);
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height);

        float imageAspect = imageWidth / imageHeight;
        float screenAspect = (float) width / height;

        if (imageAspect < screenAspect) {
            float scale = (float) width / imageWidth;
            drawWidth = width;
            drawHeight = imageHeight * scale;
        } else {
            float scale = (float) height / imageHeight;
            drawHeight = height;
            drawWidth = imageWidth * scale;
        }

        camera.position.set(drawWidth / 2f, drawHeight / 2f, 0);
        camera.update();

        game.overlay.resize(width, height);
    }

    @Override
    public boolean pan(float x, float y, float deltaX, float deltaY) {
        float screenWidth = Gdx.graphics.getWidth();

        if (drawWidth > screenWidth) {
            camera.position.x -= deltaX;
            float halfScreen = screenWidth / 2f;
            float maxX = drawWidth - halfScreen;
            camera.position.x = MathUtils.clamp(camera.position.x, halfScreen, maxX);
            return true;
        }
        return false;
    }

    @Override public boolean touchDown(float x, float y, int pointer, int button) { return false; }
    @Override public boolean tap(float x, float y, int count, int button) { return false; }
    @Override public boolean longPress(float x, float y) { return false; }
    @Override public boolean fling(float velocityX, float velocityY, int button) { return false; }
    @Override public boolean panStop(float x, float y, int pointer, int button) { return false; }
    @Override public boolean zoom(float initialDistance, float distance) { return false; }
    @Override public boolean pinch(Vector2 initialPointer1, Vector2 initialPointer2, Vector2 pointer1, Vector2 pointer2) { return false; }
    @Override public void pinchStop() {}

    @Override public void pause() {}
    @Override public void resume() {}

    @Override
    public void hide() {
        if (backButton != null) {
            backButton.remove();
            backButton = null;
        }
        game.overlay.hideAllPopups();
        game.overlay.setInInterior(false);
    }

    @Override
    public void dispose() {
        background.dispose();
        characterTexture.dispose();
    }
}
