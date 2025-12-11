package com.gdx.game.ui.screens;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.input.GestureDetector;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.gdx.game.DetectiveGame;
import com.gdx.game.domain.world.BuildingData;
import com.gdx.game.ui.component.chars.BuildingLoader;
import com.gdx.game.ui.component.chars.CharacterIcon;
import com.gdx.game.ui.component.chars.CharacterLoader;
import com.gdx.game.infra.assets.Assets;
import com.gdx.game.ui.overlay.FadeTransition;
import com.gdx.game.utils.MapInputController;
import com.gdx.game.utils.TiledTextureHelper;

import java.util.List;
import java.util.Map;

public class MapScreen implements Screen {
    private final DetectiveGame game;
    private final FadeTransition transition;
    private final TiledTextureHelper tiledHelper;

    private final OrthographicCamera camera;
    private final ScreenViewport viewport;
    private final Stage mapStage;

    private final Texture mapTexture;
    private float drawWidth, drawHeight;

    private final MapInputController inputController;

    private final List<CharacterIcon> icons;
    private final List<BuildingData> buildings;
    private final Map<String, BuildingData> buildingMap;

    private boolean firstShow = true;

    public MapScreen(DetectiveGame game, FadeTransition transition) {
        this.game = game;
        this.transition = transition;

        mapTexture = new Texture(Assets.MAP_BACKGROUND);
        tiledHelper = new TiledTextureHelper(mapTexture, 256);

        camera = new OrthographicCamera();
        viewport = new ScreenViewport(camera);
        mapStage = new Stage(viewport, game.batch);

        inputController = new MapInputController(camera, viewport);

        icons = CharacterLoader.loadMarkers(game, "characters.json");
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

       game.overlay.showProloguePublic();

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
            tiledHelper.renderTiled(game.batch, drawWidth, drawHeight);
        } else {
            game.batch.begin();
            game.batch.draw(mapTexture, 0, 0, drawWidth, drawHeight);
            game.batch.end();
        }

        mapStage.act(delta);
        mapStage.draw();

        game.overlay.render(delta);

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
}
