package com.gdx.game.screens.menu;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.gdx.game.shared.config.Assets;
import com.gdx.game.shared.ui.BackgroundFactory;
import com.gdx.game.app.model.GameContext;
import com.gdx.game.app.navigation.GameFlow;
import com.gdx.game.shared.config.UiLayout;
import com.gdx.game.shared.config.UiLayoutProfile;
import com.gdx.game.shared.ui.rendering.ScaledBackground;
import com.gdx.game.shared.lib.ScreenUtilsHelper;

public class MenuScreen implements Screen {
    private final GameContext game;
    private final GameFlow flow;

    private final OrthographicCamera camera;
    private final ScreenViewport viewport;
    private final Stage stage;
    private final ScaledBackground background;
    private final Image dimOverlay;
    private final Image startBtn;
    private final Image exitBtn;
    private final Image newGameBtn;
    private final Image gameTitleImage;
    private final Texture startTexture;
    private final Texture gameTitleTexture;

    public MenuScreen(GameContext game, GameFlow flow) {
        this.game = game;
        this.flow = flow;

        camera = new OrthographicCamera();
        viewport = new ScreenViewport(camera);
        viewport.apply();

        stage = new Stage(new ScreenViewport(), game.batch);

        background = new ScaledBackground(Assets.MAP_BACKGROUND, true, true);
        dimOverlay = BackgroundFactory.createDimBackground(
            stage.getViewport().getWorldWidth(),
            stage.getViewport().getWorldHeight(),
            0.58f
        );
        stage.addActor(dimOverlay);

        startTexture = new Texture(Assets.START_BUTTON);
        gameTitleTexture = new Texture(Assets.GAME_TITLE);

        gameTitleImage = new Image(gameTitleTexture);
        stage.addActor(gameTitleImage);

        startBtn = createStartButton();
        exitBtn = createExitButton();
        newGameBtn = createNewGameButton();

        stage.addActor(startBtn);
        stage.addActor(newGameBtn);
        stage.addActor(exitBtn);
    }

    private Image createStartButton() {
        return game.buttonFactory.createButton(
            Assets.START_BUTTON, startTexture.getWidth(), startTexture.getHeight(),
            flow::resumeOrStartGame
        );
    }

    private Image createNewGameButton() {
        return game.buttonFactory.createButton(
            Assets.NEW_GAME_BUTTON, startTexture.getWidth(), startTexture.getHeight(),
            flow::startNewGame
        );
    }

    private Image createExitButton() {
        return game.buttonFactory.createButton(
            Assets.EXIT_MENU_BUTTON, startTexture.getWidth(), startTexture.getHeight(),
            () -> Gdx.app.exit()
        );
    }

    @Override
    public void show() {
        flow.setOverlayVisible(false);

        Gdx.input.setInputProcessor(stage);
        resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
    }

    @Override
    public void render(float delta) {
        ScreenUtils.clear(0, 0, 0, 1);

        camera.update();
        game.batch.setProjectionMatrix(camera.combined);
        background.render(game.batch);

        stage.act(delta);
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        UiLayoutProfile profile = UiLayout.current(width, height);

        viewport.update(width, height, true);
        stage.getViewport().update(width, height, true);

        background.resizeToCover(viewport.getWorldWidth(), viewport.getWorldHeight());
        dimOverlay.setSize(stage.getViewport().getWorldWidth(), stage.getViewport().getWorldHeight());

        camera.position.set(background.getDrawWidth() / 2f, background.getDrawHeight() / 2f, 0);
        camera.update();

        float targetHeight = height * profile.getMenuButtonHeightRatio();
        float spacing = height * profile.getMenuSpacingRatio();
        float startY = stage.getViewport().getWorldHeight() * profile.getMenuStartYRatio() + targetHeight;

        ScreenUtilsHelper.scaleButton(startBtn, targetHeight, stage);
        ScreenUtilsHelper.scaleButton(newGameBtn, targetHeight, stage);
        ScreenUtilsHelper.scaleButton(exitBtn, targetHeight, stage);

        startBtn.setPosition(
                ScreenUtilsHelper.centerX(startBtn, stage.getViewport()),
                startY
        );
        newGameBtn.setPosition(
                ScreenUtilsHelper.centerX(newGameBtn, stage.getViewport()),
                startBtn.getY() - newGameBtn.getHeight() - spacing
        );
        exitBtn.setPosition(
                ScreenUtilsHelper.centerX(exitBtn, stage.getViewport()),
                newGameBtn.getY() - exitBtn.getHeight() - spacing
        );

        float maxTitleWidth = Math.min(
            viewport.getWorldWidth() * profile.getTitleWidthRatio(),
            viewport.getWorldHeight() * 0.75f
        );

        float texW = gameTitleTexture.getWidth();
        float texH = gameTitleTexture.getHeight();
        float aspect = texH / texW;

        float titleHeight = maxTitleWidth * aspect;

        gameTitleImage.setSize(maxTitleWidth, titleHeight);

        float titleY = viewport.getWorldHeight() - viewport.getWorldHeight() * profile.getTitleTopInsetRatio();

        gameTitleImage.setPosition(ScreenUtilsHelper.centerX(gameTitleImage, viewport), titleY);
    }

    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}

    @Override
    public void dispose() {
        stage.dispose();
        startTexture.dispose();
        background.dispose();
        gameTitleTexture.dispose();
    }
}
