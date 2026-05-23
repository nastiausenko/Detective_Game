package com.gdx.game.ui.screens;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.input.GestureDetector;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.gdx.game.DetectiveGame;
import com.gdx.game.domain.world.BuildingData;
import com.gdx.game.ui.component.chars.BuildingLoader;
import com.gdx.game.ui.component.chars.CharacterIcon;
import com.gdx.game.ui.component.chars.CharacterLoader;
import com.gdx.game.infrastructure.Assets;
import com.gdx.game.ui.overlay.FadeTransition;
import com.gdx.game.utils.MapInputController;
import com.gdx.game.utils.TiledTextureHelper;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public class MapScreen implements Screen {
    private static final boolean DEBUG_BUILDING_RECTS = false;
    private static final float FOUNTAIN_FRAME_DURATION = 0.14f;

    private final DetectiveGame game;
    private final FadeTransition transition;
    private final TiledTextureHelper tiledHelper;

    private final OrthographicCamera camera;
    private final ScreenViewport viewport;
    private final Stage mapStage;
    private final ShapeRenderer shapeRenderer;

    private final Texture mapTexture;
    private final Texture[] fountainTextures;
    private final Animation<TextureRegion> fountainAnimation;
    private float drawWidth, drawHeight;
    private float fountainAnimationTime;

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
        fountainTextures = loadFountainTextures();
        fountainAnimation = createFountainAnimation(fountainTextures);

        camera = new OrthographicCamera();
        viewport = new ScreenViewport(camera);
        mapStage = new Stage(viewport, game.batch);
        shapeRenderer = new ShapeRenderer();

        inputController = new MapInputController(camera, viewport);

        icons = CharacterLoader.loadMarkers(game, "characters.json");
        buildings = BuildingLoader.loadBuildings("buildings.json");
        buildingMap = BuildingLoader.toMap(buildings);
    }

    @Override
    public void show() {
        game.overlay.setVisible(true);
        game.getAudioManager().playAmbience(Assets.SOUND_MAP);

        if (!firstShow) return;
        firstShow = false;

        GestureDetector gestureDetector = new GestureDetector(inputController);
        Gdx.input.setInputProcessor(new InputMultiplexer(game.overlay.getStage(), mapStage, gestureDetector, inputController));

        for (CharacterIcon icon : icons) {
            applyNpcLocation(icon);
            mapStage.addActor(icon);
        }

       game.overlay.showProloguePublic();

        resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
    }

    @Override
    public void render(float delta) {
        boolean isIOS = Gdx.app.getType() == Application.ApplicationType.iOS;

        inputController.handleKeyboard(delta);
        syncIconsWithNpcLocations();
        camera.update();

        game.batch.setProjectionMatrix(camera.combined);
        if (isIOS) {
            tiledHelper.renderTiled(game.batch, drawWidth, drawHeight);
        } else {
            game.batch.begin();
            game.batch.draw(mapTexture, 0, 0, drawWidth, drawHeight);
            game.batch.end();
        }

        drawFountainAnimation(delta);

        if (DEBUG_BUILDING_RECTS) {
            drawBuildingDebugRects();
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
            marker.updatePositionFromBuilding(drawWidth, drawHeight, getMapScale());
        }
    }

    @Override
    public void dispose() {
        mapTexture.dispose();
        for (Texture texture : fountainTextures) {
            texture.dispose();
        }
        mapStage.dispose();
        shapeRenderer.dispose();
        if (transition != null) transition.dispose();
    }

    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {
        game.overlay.hideAllPopups();
    }

    private void syncIconsWithNpcLocations() {
        for (CharacterIcon icon : icons) {
            String targetBuildingId = game.getNpcLocationService().getCurrentBuildingId(icon.getId());
            if (Objects.equals(icon.getBuildingId(), targetBuildingId)) {
                continue;
            }

            BuildingData building = buildingMap.get(targetBuildingId);
            if (drawWidth > 0 && drawHeight > 0) {
                icon.moveToBuilding(building, drawWidth, drawHeight, getMapScale());
            } else {
                icon.setBuilding(building);
            }
        }
    }

    private void applyNpcLocation(CharacterIcon icon) {
        String buildingId = game.getNpcLocationService().getCurrentBuildingId(icon.getId());
        BuildingData building = buildingMap.get(buildingId);
        icon.setBuilding(building);
    }

    private float getMapScale() {
        return Math.max(1f, Math.max(
            viewport.getWorldWidth() / mapTexture.getWidth(),
            viewport.getWorldHeight() / mapTexture.getHeight()
        ));
    }

    private Texture[] loadFountainTextures() {
        return new Texture[]{
            new Texture(Assets.FOUNTAIN_FRAME_1),
            new Texture(Assets.FOUNTAIN_FRAME_2),
            new Texture(Assets.FOUNTAIN_FRAME_3),
            new Texture(Assets.FOUNTAIN_FRAME_4),
            new Texture(Assets.FOUNTAIN_FRAME_5)
        };
    }

    private Animation<TextureRegion> createFountainAnimation(Texture[] textures) {
        TextureRegion[] frames = new TextureRegion[textures.length];
        for (int i = 0; i < textures.length; i++) {
            frames[i] = new TextureRegion(textures[i]);
        }

        Animation<TextureRegion> animation = new Animation<>(FOUNTAIN_FRAME_DURATION, frames);
        animation.setPlayMode(Animation.PlayMode.LOOP_PINGPONG);
        return animation;
    }

    private void drawFountainAnimation(float delta) {
        fountainAnimationTime += delta;

        game.batch.begin();
        game.batch.draw(fountainAnimation.getKeyFrame(fountainAnimationTime), 0, 0, drawWidth, drawHeight);
        game.batch.end();
    }

    //TODO temporary method for visualizing building coords
    private void drawBuildingDebugRects() {
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
