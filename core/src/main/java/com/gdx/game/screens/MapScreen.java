package com.gdx.game.screens;

import com.badlogic.gdx.*;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.input.GestureDetector;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.gdx.game.DetectiveGame;
import com.gdx.game.data.BuildingData;
import com.gdx.game.ui.chars.BuildingLoader;
import com.gdx.game.ui.chars.CharacterIcon;
import com.gdx.game.ui.chars.CharacterLoader;
import com.gdx.game.ui.popup.PopupFactory;
import com.gdx.game.ui.popup.StoryPopup;
import com.gdx.game.utils.Assets;
import com.gdx.game.utils.FadeTransition;
import com.gdx.game.utils.MapInputController;

import java.util.List;
import java.util.Map;

public class MapScreen implements Screen {
    private final DetectiveGame game;
    private final FadeTransition transition;

    private final OrthographicCamera camera;
    private final ScreenViewport viewport;
    private final Stage mapStage;

    private final Texture mapTexture;
    private float drawWidth, drawHeight;

    private final MapInputController inputController;

    private final StoryPopup storyPopup;

    private final List<CharacterIcon> icons;
    private final List<BuildingData> buildings;
    private final Map<String, BuildingData> buildingMap;

    private boolean firstShow = true;

    public MapScreen(DetectiveGame game, FadeTransition transition) {
        this.game = game;
        this.transition = transition;

        mapTexture = new Texture(Assets.MAP_BACKGROUND);

        camera = new OrthographicCamera();
        viewport = new ScreenViewport(camera);
        mapStage = new Stage(viewport, game.batch);

        inputController = new MapInputController(camera, viewport);

        PopupFactory popupFactory = new PopupFactory(game.overlay.getStage(), game, transition);
        storyPopup = popupFactory.createStoryPopup();

        icons = CharacterLoader.loadMarkers("characters.json");
        buildings = BuildingLoader.loadBuildings("buildings.json");
        buildingMap = BuildingLoader.toMap(buildings);
    }

    @Override
    public void show() {
        game.overlay.setVisible(true);

        if (!firstShow) return;
        firstShow = false;

        GestureDetector gestureDetector = new GestureDetector(inputController);
        Gdx.input.setInputProcessor(new InputMultiplexer(game.overlay.getStage(), mapStage, gestureDetector, inputController));

        for (CharacterIcon icon : icons) {
            BuildingData b = buildingMap.get(icon.getBuildingId());
            icon.setBuilding(b);
            mapStage.addActor(icon);
        }

        Preferences prefs = Gdx.app.getPreferences("game_data");
        boolean isFirstRun = prefs.getBoolean("isFirstRun", true);
        if (isFirstRun) {
            storyPopup.show();
            prefs.putBoolean("isFirstRun", false);
            prefs.flush();
        }

        resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
    }

    @Override
    public void render(float delta) {
        boolean isIOS = Gdx.app.getType() == Application.ApplicationType.iOS;
        ScreenUtils.clear(0, 0, 0, 1);

        inputController.handleKeyboard(delta);
        camera.update();

        game.batch.setProjectionMatrix(camera.combined);
        if (isIOS) {
            renderTiledMap();
        } else {
            renderSingleTexture();
        }

        mapStage.act(delta);
        mapStage.draw();

        storyPopup.update(delta);

        game.overlay.render(delta);

        if (!storyPopup.isVisible()) {
            game.overlay.resumeTimer();
        } else {
            game.overlay.pauseTimer();
        }
    }

    @Override
    public void resize(int width, int height) {
        boolean firstResize = (drawWidth == 0 || drawHeight == 0);

        float relativeX = camera.position.x / (drawWidth == 0 ? 1 : drawWidth);
        float relativeY = camera.position.y / (drawHeight == 0 ? 1 : drawHeight);

        viewport.update(width, height);
        game.overlay.resize(width, height);

        float scaleX = viewport.getWorldWidth() / mapTexture.getWidth();
        float scaleY = viewport.getWorldHeight() / mapTexture.getHeight();
        float baseScale = Math.max(1f, Math.max(scaleX, scaleY));

        drawWidth = mapTexture.getWidth() * baseScale;
        drawHeight = mapTexture.getHeight() * baseScale;

        if (firstResize) {
            camera.position.set(drawWidth / 2f, drawHeight / 2f, 0);
        } else {
            camera.position.set(drawWidth * relativeX, drawHeight * relativeY, 0);
        }
        camera.update();

        inputController.setMapSize(drawWidth, drawHeight);

        for (CharacterIcon marker : icons) {
            marker.updatePositionFromBuilding(drawWidth, drawHeight, Math.max(1f, Math.max(
                viewport.getWorldWidth() / mapTexture.getWidth(),
                viewport.getWorldHeight() / mapTexture.getHeight()
            )));
        }

        if (storyPopup != null) storyPopup.resize(width, height);
    }

    @Override
    public void dispose() {
        mapTexture.dispose();
        mapStage.dispose();
        if (transition != null) transition.dispose();
    }

    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {
        game.overlay.hideAllPopups();
    }

    //TODO temporary method for visualizing building coords
    private void drawBuildingDebugRects() {
        ShapeRenderer shapeRenderer = new ShapeRenderer();
        shapeRenderer.setProjectionMatrix(camera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(1, 1, 0, 1);

        for (BuildingData b : buildings) {
            float x, y, w, h;

            x = b.x * drawWidth;
            y = b.y * drawHeight;
            w = b.width * drawWidth;
            h = b.height * drawHeight;

            shapeRenderer.rect(x, y, w, h);
        }

        shapeRenderer.end();
    }

    private void renderSingleTexture() {
        game.batch.begin();
        game.batch.draw(mapTexture, 0, 0, drawWidth, drawHeight);
        game.batch.end();
    }

    private TextureRegion[][] splitWithRemainder(Texture texture, int tileSize) {
        int texWidth = texture.getWidth();
        int texHeight = texture.getHeight();

        int cols = (int)Math.ceil((float)texWidth / tileSize);
        int rows = (int)Math.ceil((float)texHeight / tileSize);

        TextureRegion[][] regions = new TextureRegion[rows][cols];

        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                int x = col * tileSize;
                int y = row * tileSize;
                int w = Math.min(tileSize, texWidth - x);
                int h = Math.min(tileSize, texHeight - y);
                regions[row][col] = new TextureRegion(texture, x, y, w, h);
            }
        }
        return regions;
    }

    private void renderTiledMap() {
        int TILE_SIZE = 256;
        TextureRegion[][] tiles = splitWithRemainder(mapTexture, TILE_SIZE);

        int rows = tiles.length;
        int cols = tiles[0].length;

        float scale = Math.min(drawWidth / mapTexture.getWidth(), drawHeight / mapTexture.getHeight());
        float offsetX = (drawWidth - mapTexture.getWidth() * scale) / 2f;
        float offsetY = (drawHeight - mapTexture.getHeight() * scale) / 2f;

        game.batch.begin();
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                TextureRegion region = tiles[row][col];
                float x = offsetX + col * TILE_SIZE * scale;
                float y = offsetY + (rows - 1 - row) * TILE_SIZE * scale;
                float w = region.getRegionWidth() * scale;
                float h = region.getRegionHeight() * scale;
                game.batch.draw(region, x, y, w, h);
            }
        }
        game.batch.end();
    }
}
