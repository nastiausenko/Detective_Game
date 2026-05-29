package com.gdx.game.screens.map;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.input.GestureDetector;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.gdx.game.model.BuildingData;
import com.gdx.game.app.model.GameContext;
import com.gdx.game.features.worldmap.model.BuildingLoader;
import com.gdx.game.features.worldmap.ui.CharacterIcon;
import com.gdx.game.features.worldmap.model.CharacterLoader;
import com.gdx.game.shared.config.Assets;
import com.gdx.game.shared.ui.rendering.ScaledBackground;
import com.gdx.game.shared.lib.MapInputController;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public class MapScreen implements Screen {
    private static final boolean DEBUG_BUILDING_RECTS = false;
    private static final String CRIME_SCENE_BUILDING_ID = "professor_house";
    private static final float MAP_ANIMATION_FRAME_DURATION = 0.14f;
    private static final float MAX_NIGHT_ALPHA = 0.58f;
    private static final float MAX_LIGHT_ALPHA = 0.82f;
    private static final int DAWN_START_MINUTE = 5 * 60;
    private static final int DAY_START_MINUTE = 7 * 60;
    private static final int DUSK_START_MINUTE = 17 * 60;
    private static final int NIGHT_START_MINUTE = 20 * 60;

    private final GameContext game;
    private final ScaledBackground mapBackground;

    private final OrthographicCamera camera;
    private final ScreenViewport viewport;
    private final Stage mapStage;
    private final ShapeRenderer shapeRenderer;

    private final Texture lightOverlayTexture;
    private final Texture crimeSceneBadgeTexture;
    private final Texture[] fountainTextures;
    private final Texture[] lakeTextures;
    private final Animation<TextureRegion> fountainAnimation;
    private final Animation<TextureRegion> lakeAnimation;
    private float drawWidth, drawHeight;
    private float fountainAnimationTime;
    private float lakeAnimationTime;

    private final MapInputController inputController;

    private final List<CharacterIcon> icons;
    private final CharacterIcon crimeSceneIcon;
    private final Image crimeSceneBadge;
    private final List<BuildingData> buildings;
    private final Map<String, BuildingData> buildingMap;

    private boolean firstShow = true;

    public MapScreen(GameContext game) {
        this.game = game;

        mapBackground = new ScaledBackground(Assets.MAP_BACKGROUND, true, true);
        lightOverlayTexture = new Texture(Assets.MAP_LIGHT_OVERLAY);
        crimeSceneBadgeTexture = new Texture(Assets.BADGE);
        fountainTextures = loadTextures(
            Assets.FOUNTAIN_FRAME_1,
            Assets.FOUNTAIN_FRAME_2,
            Assets.FOUNTAIN_FRAME_3,
            Assets.FOUNTAIN_FRAME_4,
            Assets.FOUNTAIN_FRAME_5
        );
        fountainAnimation = createPingPongAnimation(fountainTextures);
        lakeTextures = loadTextures(
            Assets.LAKE_FRAME_1,
            Assets.LAKE_FRAME_2,
            Assets.LAKE_FRAME_3
        );
        lakeAnimation = createPingPongAnimation(lakeTextures);

        camera = new OrthographicCamera();
        viewport = new ScreenViewport(camera);
        mapStage = new Stage(viewport, game.batch);
        shapeRenderer = new ShapeRenderer();

        inputController = new MapInputController(camera, viewport);

        icons = CharacterLoader.loadMarkers(game, "characters.json");
        crimeSceneIcon = new CharacterIcon(
            game,
            "crime_scene",
            Assets.CRIME_SCENE_ICON,
            null,
            CRIME_SCENE_BUILDING_ID
        );
        crimeSceneBadge = new Image(crimeSceneBadgeTexture);
        crimeSceneBadge.setVisible(false);
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

        BuildingData crimeSceneBuilding = buildingMap.get(CRIME_SCENE_BUILDING_ID);
        crimeSceneIcon.setBuilding(crimeSceneBuilding);
        mapStage.addActor(crimeSceneIcon);
        mapStage.addActor(crimeSceneBadge);

        game.overlay.showPrologue();

        resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
    }

    @Override
    public void render(float delta) {
        inputController.handleKeyboard(delta);
        syncIconsWithNpcLocations();
        camera.update();

        game.batch.setProjectionMatrix(camera.combined);
        mapBackground.render(game.batch);

        drawFountainAnimation(delta);
        drawLakeAnimation(delta);
        drawDayNightEffect();

        if (DEBUG_BUILDING_RECTS) {
            drawBuildingDebugRects();
        }

        updateCrimeSceneBadge();

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

        mapBackground.resizeToCover(viewport.getWorldWidth(), viewport.getWorldHeight());
        drawWidth = mapBackground.getDrawWidth();
        drawHeight = mapBackground.getDrawHeight();

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
        crimeSceneIcon.updatePositionFromBuilding(drawWidth, drawHeight, getMapScale());
        layoutCrimeSceneBadge();
    }

    @Override
    public void dispose() {
        mapBackground.dispose();
        lightOverlayTexture.dispose();
        crimeSceneBadgeTexture.dispose();
        for (Texture texture : fountainTextures) {
            texture.dispose();
        }
        for (Texture texture : lakeTextures) {
            texture.dispose();
        }
        mapStage.dispose();
        shapeRenderer.dispose();
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

    private void updateCrimeSceneBadge() {
        boolean hasPendingCrimeSceneHints =
            game.getCrimeSceneService() != null
                && game.getCrimeSceneService().hasPendingHints(CRIME_SCENE_BUILDING_ID);

        crimeSceneBadge.setVisible(hasPendingCrimeSceneHints);
        if (hasPendingCrimeSceneHints) {
            layoutCrimeSceneBadge();
            crimeSceneBadge.toFront();
        }
    }

    private void layoutCrimeSceneBadge() {
        float badgeSize = crimeSceneIcon.getWidth() * 0.48f;
        crimeSceneBadge.setSize(badgeSize, badgeSize);
        crimeSceneBadge.setPosition(
            crimeSceneIcon.getX() + crimeSceneIcon.getWidth() - badgeSize * 0.72f,
            crimeSceneIcon.getY() + crimeSceneIcon.getHeight() - badgeSize * 0.68f
        );
    }

    private float getMapScale() {
        return Math.max(1f, Math.max(
            viewport.getWorldWidth() / mapBackground.getTextureWidth(),
            viewport.getWorldHeight() / mapBackground.getTextureHeight()
        ));
    }

    private Texture[] loadTextures(String... paths) {
        Texture[] textures = new Texture[paths.length];
        for (int i = 0; i < paths.length; i++) {
            textures[i] = new Texture(paths[i]);
        }
        return textures;
    }

    private Animation<TextureRegion> createPingPongAnimation(Texture[] textures) {
        TextureRegion[] frames = new TextureRegion[textures.length];
        for (int i = 0; i < textures.length; i++) {
            frames[i] = new TextureRegion(textures[i]);
        }

        Animation<TextureRegion> animation = new Animation<>(MAP_ANIMATION_FRAME_DURATION, frames);
        animation.setPlayMode(Animation.PlayMode.LOOP_PINGPONG);
        return animation;
    }

    private void drawFountainAnimation(float delta) {
        fountainAnimationTime += delta;

        game.batch.begin();
        game.batch.draw(fountainAnimation.getKeyFrame(fountainAnimationTime), 0, 0, drawWidth, drawHeight);
        game.batch.end();
    }

    private void drawLakeAnimation(float delta) {
        lakeAnimationTime += delta;

        game.batch.begin();
        game.batch.draw(lakeAnimation.getKeyFrame(lakeAnimationTime), 0, 0, drawWidth, drawHeight);
        game.batch.end();
    }

    private void drawDayNightEffect() {
        float nightAlpha = calculateNightAlpha();
        float lightAlpha = calculateLightAlpha();

        if (nightAlpha <= 0f && lightAlpha <= 0f) {
            return;
        }

        drawNightOverlay(nightAlpha);
        drawLightOverlay(lightAlpha);
    }

    private float calculateNightAlpha() {
        return MAX_NIGHT_ALPHA * calculateNightProgress();
    }

    private float calculateLightAlpha() {
        return MAX_LIGHT_ALPHA * calculateNightProgress();
    }

    private float calculateNightProgress() {
        int minute = game.overlay.getTimer().getMinutesOfCurrentDay();

        if (minute >= NIGHT_START_MINUTE || minute < DAWN_START_MINUTE) {
            return 1f;
        }

        if (minute < DAY_START_MINUTE) {
            return 1f - smoothStep(DAWN_START_MINUTE, DAY_START_MINUTE, minute);
        }

        if (minute < DUSK_START_MINUTE) {
            return 0f;
        }

        return smoothStep(DUSK_START_MINUTE, NIGHT_START_MINUTE, minute);
    }

    private float smoothStep(int start, int end, int value) {
        float progress = MathUtils.clamp((value - start) / (float) (end - start), 0f, 1f);
        return progress * progress * (3f - 2f * progress);
    }

    private void drawNightOverlay(float alpha) {
        if (alpha <= 0f) return;

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        shapeRenderer.setProjectionMatrix(camera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0f, 0f, 0f, alpha);
        shapeRenderer.rect(0, 0, drawWidth, drawHeight);
        shapeRenderer.end();

        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    private void drawLightOverlay(float alpha) {
        if (alpha <= 0f) return;

        game.batch.begin();
        game.batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
        game.batch.setColor(1f, 1f, 1f, alpha);
        game.batch.draw(lightOverlayTexture, 0, 0, drawWidth, drawHeight);
        game.batch.setColor(1f, 1f, 1f, 1f);
        game.batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
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
