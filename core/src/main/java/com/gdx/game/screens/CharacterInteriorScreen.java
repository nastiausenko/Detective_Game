package com.gdx.game.screens;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.input.GestureDetector;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.gdx.game.DetectiveGame;
import com.gdx.game.utils.Assets;
import com.gdx.game.utils.FontScaler;
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
    private final Skin skin;

    private final Stage dialogueStage;
    private TextField inputField;
    private Label dialogueLabel;
    private final Texture questionAreaTexture;
    private final Image questionAreaImage;

    private final Texture answerAreaTexture;
    private final Image answerAreaImage;

    private float drawWidth, drawHeight;
    private float imageWidth, imageHeight;
    private String currentResponse = "";
    private float bubblePadding = 50f;
    private float bubbleMaxWidthRatio = 0.4f;

    public CharacterInteriorScreen(DetectiveGame game, String backgroundPath, String buildingId, String characterName, String fullBody) {
        this.game = game;
        this.background = new Texture(backgroundPath);
        this.buildingId = buildingId;
        this.characterName = characterName;
        this.characterTexture = new Texture(fullBody);

        camera = new OrthographicCamera();
        viewport = new ScreenViewport(camera);
        tiledHelper = new TiledTextureHelper(background, 256);
        skin = new Skin(Gdx.files.internal("ui/uiskin.json"));

        dialogueStage = new Stage(new ScreenViewport());
        questionAreaTexture = new Texture(Assets.QUESTION_AREA);
        questionAreaImage = new Image(questionAreaTexture);
        dialogueStage.addActor(questionAreaImage);

        answerAreaTexture = new Texture(Assets.ANSWER_AREA);
        answerAreaImage = new Image(answerAreaTexture);
        answerAreaImage.setVisible(false);
        dialogueStage.addActor(answerAreaImage);
    }

    @Override
    public void show() {
        game.overlay.setVisible(true);
        game.overlay.setInInterior(true);

        imageWidth = background.getWidth();
        imageHeight = background.getHeight();

        dialogueLabel = new Label("", skin);
        dialogueLabel.setWrap(true);
        dialogueLabel.setColor(Color.BLACK);
        dialogueLabel.setVisible(false);

        inputField = new TextField("", createTextFieldStyle());
        inputField.setMessageText("Запитайте персонажа...");
        inputField.setWidth(Gdx.graphics.getWidth() * 0.8f);
        inputField.setPosition(Gdx.graphics.getWidth() * 0.1f, 40);

        inputField.setTextFieldListener((textField, c) -> {
            if (c == '\n' || c == '\r') {
                handleQuestion(inputField.getText());
                inputField.setText("");
            }
        });

        dialogueStage.addActor(dialogueLabel);
        dialogueStage.addActor(inputField);

        Gdx.input.setInputProcessor(new InputMultiplexer(
            dialogueStage,
            game.overlay.getStage(),
            new GestureDetector(this)
        ));
    }

    private TextField.TextFieldStyle createTextFieldStyle() {
        TextField.TextFieldStyle style = new TextField.TextFieldStyle();
        style.font = skin.getFont("default-font");
        FontScaler.applyScale(style.font);
        style.fontColor = Color.BLACK;
        style.messageFontColor = Color.BLACK;
        style.cursor = skin.newDrawable("cursor", Color.BLACK);
        style.background = null;
        style.selection = skin.newDrawable("white", new Color(0.3f, 0.5f, 1f, 0.5f));
        return style;
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

        dialogueStage.act(delta);
        dialogueStage.draw();

        game.overlay.render(delta);
    }

    // TODO fix bubble and input scaling
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

        float baseWidth = questionAreaTexture.getWidth();
        float baseHeight = questionAreaTexture.getHeight();
        float aspect = baseWidth / baseHeight;

        float desiredWidth = width * 0.8f;
        float desiredHeight = desiredWidth / aspect * 0.5f;

        questionAreaImage.setSize(desiredWidth, desiredHeight);
        questionAreaImage.setPosition(
            (width - desiredWidth) / 2f,
            20f
        );

        float inputPadding = 50f * screenAspect;
        float inputWidth = desiredWidth - inputPadding * 2f;
        float inputHeight = desiredHeight * 0.4f;

        inputField.setSize(inputWidth, inputHeight);
        inputField.setPosition(
            questionAreaImage.getX() + (desiredWidth - inputWidth) / 2f,
            questionAreaImage.getY() + (desiredHeight - inputHeight) / 2f
        );

        inputField.setStyle(createTextFieldStyle());

        dialogueStage.getViewport().update(width, height, true);

        camera.position.set(drawWidth / 2f, drawHeight / 2f, 0);
        camera.update();

        game.overlay.resize(width, height);
        updateAnswerBubbleLayout(width, height);
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

    private void handleQuestion(String question) {
        if (question.isEmpty()) return;

        if (question.toLowerCase().contains("жертву")) {
            currentResponse = "Так, я знав жертву. Вона приходила сюди кілька разів...";
        } else if (question.toLowerCase().contains("де ти був")) {
            currentResponse = "Я... був удома. Один.";
        } else {
            currentResponse = "Не розумію, до чого це питання.";
        }

        dialogueLabel.setText(currentResponse);
        dialogueLabel.setVisible(true);
        answerAreaImage.setVisible(true);

        updateAnswerBubbleLayout(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
    }

    private void updateAnswerBubbleLayout(float width, float height) {
        if (!dialogueLabel.isVisible()) return;

        float originalBubbleWidth = answerAreaTexture.getWidth();

        float scale = Math.min(width / 1920f, height / 1080f);
        float scaledBubbleWidth = originalBubbleWidth * scale;

        float innerPaddingX = bubblePadding * 0.8f;
        float innerPaddingY = bubblePadding;

        float maxTextWidth = scaledBubbleWidth - innerPaddingX * 2f;

        dialogueLabel.setWrap(true);
        dialogueLabel.setAlignment(Align.center);
        dialogueLabel.setWidth(maxTextWidth);
        dialogueLabel.layout();

        float textWidth = dialogueLabel.getPrefWidth();
        float textHeight = dialogueLabel.getPrefHeight();

        float bubbleWidth = Math.max(textWidth + innerPaddingX * 2f, scaledBubbleWidth * 0.5f);

        bubbleWidth = Math.min(bubbleWidth, scaledBubbleWidth);

        float bubbleHeight = textHeight + innerPaddingY * 2f;

        float worldHeight = viewport.getWorldHeight();
        float charHeight = characterTexture.getHeight();
        float charWidth = characterTexture.getWidth();
        float charScale = (worldHeight * 0.8f) / charHeight;
        float drawW = charWidth * charScale;
        float drawH = charHeight * charScale;

        float camCenterX = camera.position.x;
        float charX = camCenterX - (drawW / 2f);

        float bubbleX = charX - drawW * 0.9f;
        float bubbleY = drawH - bubbleHeight * 0.9f;

        answerAreaImage.setSize(bubbleWidth, bubbleHeight);
        answerAreaImage.setPosition(bubbleX, bubbleY);

        dialogueLabel.setSize(bubbleWidth - innerPaddingX * 2f, textHeight);

        float tailOffset = bubbleHeight * 0.05f;

        dialogueLabel.setPosition(
            bubbleX + innerPaddingX,
            bubbleY + (bubbleHeight - textHeight) / 2f + tailOffset
        );
    }
}
