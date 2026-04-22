package com.gdx.game.ui.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.gdx.game.DetectiveGame;
import com.gdx.game.GameData;
import com.gdx.game.domain.investigation.InvestigationState;
import com.gdx.game.infrastructure.Assets;
import com.gdx.game.infrastructure.UiLayout;
import com.gdx.game.infrastructure.UiLayoutProfile;
import com.gdx.game.ui.overlay.FadeTransition;
import com.gdx.game.utils.ScreenUtilsHelper;

public class MenuScreen implements Screen {
    private final DetectiveGame game;
    private final FadeTransition transition;

    private final OrthographicCamera camera;
    private final ScreenViewport viewport;
    private final Stage stage;
    private final Texture backgroundTexture;
    private final Texture startTexture;
    private final Image startBtn;
    private final Image exitBtn;
    private final Image newGameBtn;
    private final Image gameTitleImage;
    private final Texture gameTitleTexture;
    private float drawWidth, drawHeight;

    public MenuScreen(DetectiveGame game, FadeTransition transition) {
        this.game = game;
        this.transition = transition;

        camera = new OrthographicCamera();
        viewport = new ScreenViewport(camera);
        viewport.apply();

        stage = new Stage(new ScreenViewport(), game.batch);

        backgroundTexture = new Texture(Assets.MENU_BACKGROUND);

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
        return game.getButtonFactory().createButton(
            Assets.START_BUTTON, startTexture.getWidth(), startTexture.getHeight(),
            () -> {
                if (!transition.isTransitioning()) {
                    transition.startFadeOut(0.7f, () -> {
                        if (game.overlay.getTimer().isTimeOver()) {
                          handleNewGame();
                        } else {
                            game.setScreen(new MapScreen(game, transition));
                            transition.startFadeIn(0.7f);
                        }
                    });
                }
            }
        );
    }

    private Image createNewGameButton() {
        return game.getButtonFactory().createButton(
            Assets.NEW_GAME_BUTTON, startTexture.getWidth(), startTexture.getHeight(),
            () -> {
                if (!transition.isTransitioning()) {
                    transition.startFadeOut(0.7f, this::handleNewGame);
                }
            }
        );
    }

    private void handleNewGame() {
        GameData.clearAll();
        game.getNpcDialogueService().resetAllNpcState();
        game.overlay.resetTimer();

        InvestigationState inv = game.getInvestigationState();
        if (inv != null) {
            inv.accusationDone = false;
            inv.accusedNpcId = null;
        }

        game.setScreen(new MapScreen(game, transition));
        transition.startFadeIn(0.7f);
    }

    private Image createExitButton() {
        return game.getButtonFactory().createButton(
            Assets.EXIT_MENU_BUTTON, startTexture.getWidth(), startTexture.getHeight(),
            () -> Gdx.app.exit()
        );
    }

    @Override
    public void show() {
        game.overlay.setVisible(false);

        Gdx.input.setInputProcessor(stage);
        resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
    }

    @Override
    public void render(float delta) {
        ScreenUtils.clear(0, 0, 0, 1);

        camera.update();
        game.batch.setProjectionMatrix(camera.combined);
        game.batch.begin();
        game.batch.draw(backgroundTexture, 0, 0, drawWidth, drawHeight);
        game.batch.end();

        stage.act(delta);
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        UiLayoutProfile profile = UiLayout.current(width, height);

        viewport.update(width, height, true);
        stage.getViewport().update(width, height, true);

        float[] size = ScreenUtilsHelper.calculateDrawSize(
            backgroundTexture.getWidth(),
            backgroundTexture.getHeight(),
            viewport.getWorldWidth(),
            viewport.getWorldHeight()
        );

        drawWidth = size[0];
        drawHeight = size[1];

        camera.position.set(drawWidth / 2f, drawHeight / 2f, 0);
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
        backgroundTexture.dispose();
        gameTitleTexture.dispose();
        transition.dispose();
    }
}
