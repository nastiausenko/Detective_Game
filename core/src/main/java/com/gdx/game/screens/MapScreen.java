package com.gdx.game.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.input.GestureDetector;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.gdx.game.DetectiveGame;
import com.gdx.game.data.BuildingData;
import com.gdx.game.ui.chars.BuildingLoader;
import com.gdx.game.ui.chars.CharacterIcon;
import com.gdx.game.ui.chars.CharacterLoader;
import com.gdx.game.ui.popup.NotePopup;
import com.gdx.game.ui.popup.SettingsPopup;
import com.gdx.game.ui.popup.StoryPopup;
import com.gdx.game.utils.FadeTransition;
import com.gdx.game.utils.MapInputController;
import com.gdx.game.utils.ScreenUtilsHelper;

import java.util.List;
import java.util.Map;

public class MapScreen implements Screen {
    private final DetectiveGame game;
    private final FadeTransition transition;

    private final OrthographicCamera camera;
    private final ScreenViewport viewport;
    private final Stage mapStage;
    private final Stage uiStage;

    private final Texture mapTexture;
    private float drawWidth, drawHeight;

    private final MapInputController inputController;

    private NotePopup notePopup;
    private SettingsPopup settingsPopup;
    private StoryPopup storyPopup;

    private final Image notesButton;
    private final Image settingsButton;

    private final List<CharacterIcon> icons;
    private final List<BuildingData> buildings;
    private final Map<String, BuildingData> buildingMap;


    public MapScreen(DetectiveGame game, FadeTransition transition) {
        this.game = game;
        this.transition = transition;

        mapTexture = new Texture("img.png");

        camera = new OrthographicCamera();
        viewport = new ScreenViewport(camera);
        mapStage = new Stage(viewport, game.batch);
        uiStage = new Stage(new ScreenViewport(), game.batch);

        //TODO refs for prologue
        storyPopup = new StoryPopup(uiStage, game);
        storyPopup.show();

        inputController = new MapInputController(camera, viewport);
        GestureDetector gestureDetector = new GestureDetector(inputController);
        Gdx.input.setInputProcessor(new InputMultiplexer(uiStage, mapStage, gestureDetector, inputController));

        notesButton = createNotesButton();
        settingsButton = createSettingsButton();

        uiStage.addActor(notesButton);
        uiStage.addActor(settingsButton);

        icons = CharacterLoader.loadMarkers("characters.json");
        buildings = BuildingLoader.loadBuildings("buildings.json");
        buildingMap = BuildingLoader.toMap(buildings);

        for (CharacterIcon icon : icons) {
            BuildingData b = buildingMap.get(icon.getBuildingId());
            icon.setBuilding(b);
            mapStage.addActor(icon);
        }

        resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
    }

    private Image createNotesButton() {
        return game.getButtonFactory().createButton(
            "menu/note/note_icon.png", 64, 64,
            () -> {
                if (notePopup == null) {
                    notePopup = new NotePopup(uiStage,
                        new Skin(Gdx.files.internal("ui/uiskin.json")), game);
                }
                notePopup.show();
            });
    }

    private Image createSettingsButton() {
        return game.getButtonFactory().createButton(
            "menu/settings/settings_btn.png", 64, 64,
            () -> {
                if (settingsPopup == null) {
                    settingsPopup = new SettingsPopup(uiStage, "menu/settings/settings.png", game, transition);
                }
                settingsPopup.show();
            });
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

//        drawBuildingDebugRects(); //temporary

        mapStage.act(delta);
        mapStage.draw();

        uiStage.act(delta);
        uiStage.draw();

        if (storyPopup != null) storyPopup.update(delta);

        transition.update(delta);
        transition.render();
    }


    @Override
    public void resize(int width, int height) {
        float relativeX = camera.position.x / drawWidth;
        float relativeY = camera.position.y / drawHeight;

        viewport.update(width, height);
        uiStage.getViewport().update(width, height, true);

        float[] size = ScreenUtilsHelper.calculateDrawSize(
            mapTexture.getWidth(),
            mapTexture.getHeight(),
            viewport.getWorldWidth(),
            viewport.getWorldHeight()
        );

        drawWidth = size[0];
        drawHeight = size[1];

        camera.position.set(drawWidth * relativeX, drawHeight * relativeY, 0);
        camera.update();

        inputController.setMapSize(drawWidth, drawHeight);

        float targetHeight = height * 0.12f;

        ScreenUtilsHelper.scaleAndPositionButton(notesButton, targetHeight, 10,
            uiStage.getViewport().getWorldHeight() - targetHeight - 10);

        ScreenUtilsHelper.scaleAndPositionButton(settingsButton, targetHeight,
            uiStage.getViewport().getWorldWidth() - settingsButton.getWidth() - 10,
            uiStage.getViewport().getWorldHeight() - targetHeight - 10
        );

        for (CharacterIcon marker : icons) {
            marker.updatePositionFromBuilding(drawWidth, drawHeight, Math.max(1f, Math.max(
                viewport.getWorldWidth() / mapTexture.getWidth(),
                viewport.getWorldHeight() / mapTexture.getHeight()
            )));
        }

        if (notePopup != null) notePopup.resize(width, height);
        if (settingsPopup != null) settingsPopup.resize(width, height);
        if (storyPopup != null) storyPopup.resize(width, height);
    }

    @Override
    public void dispose() {
        mapTexture.dispose();
        uiStage.dispose();
        mapStage.dispose();
        if (notePopup != null) notePopup.dispose();
        if (settingsPopup != null) settingsPopup.dispose();
        if (transition != null) transition.dispose();
        if (settingsPopup != null) settingsPopup.dispose();
    }

    @Override public void show() {}
    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}

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
