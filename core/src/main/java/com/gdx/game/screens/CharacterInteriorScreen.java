package com.gdx.game.screens;

import com.badlogic.gdx.*;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.NinePatch;
import com.badlogic.gdx.input.GestureDetector;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.IntArray;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.gdx.game.DetectiveGame;
import com.gdx.game.data.DialogueHistory;
import com.gdx.game.data.DossierData;
import com.gdx.game.npc.NpcDialogueService;
import com.gdx.game.npc.NpcState;
import com.gdx.game.utils.Assets;
import com.gdx.game.utils.FontScaler;
import com.gdx.game.utils.ScreenUtilsHelper;
import com.gdx.game.utils.TiledTextureHelper;

import java.util.List;
import java.util.Locale;

public class CharacterInteriorScreen implements Screen, GestureDetector.GestureListener {

    private final DetectiveGame game;
    private final NpcDialogueService npcService;

    private final Texture background;
    private float imageWidth, imageHeight;
    private final TiledTextureHelper tiledHelper;

    private final String characterId;
    private final Texture characterTexture;
    private final Image characterImage;

    private final OrthographicCamera camera;
    private final ScreenViewport viewport;
    private final Skin skin;

    private final Stage dialogueStage;
    private TextField inputField;
    private Label dialogueLabel;

    private final Texture questionAreaTexture;
    private final Image questionAreaImage;

    private final Image sendButtonImage;

    private float questionAreaFixedHeight = -1f;

    private final Texture answerAreaTexture;
    private final Image answerAreaImage;

    private float drawWidth, drawHeight;

    private String currentResponse = "";
    private final GlyphLayout glyphLayout = new GlyphLayout();

    public CharacterInteriorScreen(DetectiveGame game, String backgroundPath, String characterId, String fullBody) {
        this.game = game;
        this.background = new Texture(backgroundPath);
        this.characterId = characterId;
        this.characterTexture = new Texture(fullBody);
        this.npcService = game.getNpcDialogueService();

        camera = new OrthographicCamera();
        viewport = new ScreenViewport(camera);
        tiledHelper = new TiledTextureHelper(background, 256);
        skin = new Skin(Gdx.files.internal("ui/uiskin.json"));
        dialogueStage = new Stage(new ScreenViewport());

        characterImage = new Image(characterTexture);
        dialogueStage.addActor(characterImage);

        questionAreaTexture = new Texture(Assets.QUESTION_AREA);

        int left   = 60;
        int right  = 60;
        int top    = 16;
        int bottom = 16;

        NinePatch questionAreaPatch = new NinePatch(questionAreaTexture, left, right, top, bottom);
        questionAreaImage = new Image(new NinePatchDrawable(questionAreaPatch));
        dialogueStage.addActor(questionAreaImage);

        answerAreaTexture = new Texture(Assets.ANSWER_AREA);
        answerAreaImage = new Image(answerAreaTexture);
        answerAreaImage.setVisible(false);
        dialogueStage.addActor(answerAreaImage);

        sendButtonImage = game.getButtonFactory().createButton(Assets.SEND_BUTTON, 64, 64, this::send);
        dialogueStage.addActor(sendButtonImage);
    }

    private void send() {
        if (inputField == null) return;

        String text = inputField.getText();
        if (text == null || text.trim().isEmpty()) return;

        handleQuestion(text.trim());
        inputField.setText("");
    }

    @Override
    public void show() {
        game.overlay.setVisible(true);
        game.overlay.setInInterior(true);
        game.overlay.setCurrentNpcId(characterId);


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
              send();
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

        if (questionAreaFixedHeight < 0f) {
            float designHeight = questionAreaTexture.getHeight();
            float uiScale = height / 1000f;
            questionAreaFixedHeight = designHeight * uiScale;
        }

        float desiredWidth  = width * 0.85f;
        float desiredHeight = questionAreaFixedHeight;

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

        float innerMarginX = desiredWidth * 0.04f;
        float bubbleX      = questionAreaImage.getX();
        float bubbleY      = questionAreaImage.getY();
        float bubbleH      = questionAreaImage.getHeight();
        float bubbleW      = questionAreaImage.getWidth();

        float sendTargetH = bubbleH * 0.6f;
        ScreenUtilsHelper.scaleButton(sendButtonImage, sendTargetH, dialogueStage);

        float sendX = bubbleX + bubbleW - innerMarginX - sendButtonImage.getWidth();
        float sendY = bubbleY + (bubbleH - sendButtonImage.getHeight()) / 2f;
        sendButtonImage.setPosition(sendX, sendY);


        float texW = characterTexture.getWidth();
        float texH = characterTexture.getHeight();

        float scaleByWidth  = (width  * 0.45f) / texW;
        float scaleByHeight = (height * 0.70f) / texH;

        float scale = Math.min(scaleByWidth, scaleByHeight);

        float characterDrawW = texW * scale;
        float characterDrawH = texH * scale;

        float charY = height * 0.1f;
        float charX = width  * 0.5f - characterDrawW / 2f;

        characterImage.setBounds(charX, charY, characterDrawW, characterDrawH);
        updateAnswerBubbleLayout(width, height);

        dialogueStage.getViewport().update(width, height, true);

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
        game.overlay.hideAllPopups();
        game.overlay.setInInterior(false);
    }

    @Override
    public void dispose() {
        background.dispose();
        characterTexture.dispose();
    }

    private void handleQuestion(String question) {
        if (question == null || question.trim().isEmpty()) return;

        currentResponse = "…";
        dialogueLabel.setText(currentResponse);
        dialogueLabel.setVisible(true);
        answerAreaImage.setVisible(true);
        updateAnswerBubbleLayout(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        final String q = question.trim();

        new Thread(() -> {
            String answer;
            IntArray factsToReveal = new IntArray();

            try {
                DossierData data = game.getDossierDb().characters.get(characterId);
                if (data != null && data.hiddenFacts != null && !data.hiddenFacts.isEmpty()) {

                    String qLower = q.toLowerCase(Locale.ROOT);

                    for (int i = 0; i < data.hiddenFacts.size(); i++) {
                        String hidden = data.hiddenFacts.get(i);
                        if (hidden == null || hidden.isEmpty()) continue;

                        List<List<String>> allTriggers = data.hiddenFactTriggers;
                        List<String> triggersForFact = null;
                        if (allTriggers != null && i < allTriggers.size()) {
                            triggersForFact = allTriggers.get(i);
                        }

                        if (triggersForFact == null || triggersForFact.isEmpty()) {
                            continue;
                        }

                        if (!matchesAnyTrigger(triggersForFact, qLower)) {
                            Gdx.app.log("FACT_DEBUG", "No trigger for fact #" + i);
                            continue;
                        }

                        Gdx.app.log("FACT_DEBUG", "Trigger hit for fact #" + i + " : " + hidden);

                        boolean logical = false;
                        try {
                            logical = npcService.isQuestionLogicalForHiddenFact(characterId, q, hidden);
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }

                        Gdx.app.log("FACT_DEBUG", "LLM logical? " + logical + " for fact #" + i);

                        if (logical) {
                            factsToReveal.add(i);
                        }
                    }

                    npcService.markFactsRevealed(characterId, factsToReveal);
                }

                answer = npcService.askNpcSync(characterId, q);

            } catch (Exception e) {
                e.printStackTrace();
                answer = "Вибач, але я зараз не можу відповісти.";
            }

            final String finalAnswer = answer;
            final IntArray revealedCopy = new IntArray(factsToReveal);

            Gdx.app.postRunnable(() -> {
                currentResponse = finalAnswer;
                dialogueLabel.setText(currentResponse);
                dialogueLabel.setVisible(true);
                answerAreaImage.setVisible(true);
                updateAnswerBubbleLayout(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
                DialogueHistory.append(characterId, q, finalAnswer);

                DossierData data = game.getDossierDb().characters.get(characterId);
                if (data != null && data.hiddenFacts != null) {
                    NpcState state = game.getNpcStateManager()
                        .getOrCreate(characterId, data.hiddenFacts.size());

                    for (int i = 0; i < revealedCopy.size; i++) {
                        int idx = revealedCopy.get(i);
                        if (idx >= 0 && idx < state.hiddenRevealed.length) {
                            state.hiddenRevealed[idx] = true;
                        }
                    }
                }
            });
        }).start();
    }

    private boolean matchesAnyTrigger(List<String> triggers, String questionLower) {
        if (triggers == null || triggers.isEmpty()) return false;

        for (String t : triggers) {
            if (t == null || t.isEmpty()) continue;
            if (questionLower.contains(t.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private void updateAnswerBubbleLayout(float screenWidth, float screenHeight) {
        if (!dialogueLabel.isVisible()) return;

        float paddingX  = 22f;
        float paddingY  = 16f;
        float tailHeight = 21f;

        boolean portrait = screenHeight > screenWidth;

        float charW = characterImage.getWidth();
        float charH = characterImage.getHeight();
        float charX = characterImage.getX();
        float charY = characterImage.getY();

        float anchorRight = charX + charW * 0.5f - paddingX;

        float maxBubbleWidth = answerAreaTexture.getWidth()*0.4f;

        float screenLimit = screenWidth * (portrait ? 0.85f : 0.8f);
        maxBubbleWidth = Math.min(maxBubbleWidth, screenLimit);

        maxBubbleWidth = Math.min(maxBubbleWidth, anchorRight-paddingX);

        Label.LabelStyle style = dialogueLabel.getStyle();
        String text = currentResponse != null ? currentResponse : "";

        glyphLayout.setText(style.font, text);
        float singleLineWidth = glyphLayout.width;

        float maxInnerWidth = maxBubbleWidth - paddingX * 2f;
        if (maxInnerWidth < 0) maxInnerWidth = 0;

        float innerWidth = Math.min(singleLineWidth, maxInnerWidth);

        dialogueLabel.setWrap(true);
        dialogueLabel.setAlignment(Align.center);
        dialogueLabel.setWidth(innerWidth);
        dialogueLabel.setText(text);
        dialogueLabel.layout();
        float textHeight = dialogueLabel.getPrefHeight();

        float bubbleWidth  = innerWidth + paddingX * 2f;
        float bubbleHeight = textHeight + paddingY * 2f + tailHeight;

        float maxBubbleHeight = screenHeight * 0.45f;
        if (bubbleHeight > maxBubbleHeight) {
            bubbleHeight = maxBubbleHeight;
        }

        float bubbleX = anchorRight - bubbleWidth;
        float bubbleY = charY + charH * 0.7f;

        bubbleY = MathUtils.clamp(bubbleY, 0, screenHeight - bubbleHeight);

        answerAreaImage.setBounds(bubbleX, bubbleY, bubbleWidth, bubbleHeight);

        float innerHeight = bubbleHeight - paddingY * 2f - tailHeight;
        if (textHeight > innerHeight) textHeight = innerHeight;

        float textX = bubbleX + paddingX;
        float textY = bubbleY + tailHeight + paddingY
            + (innerHeight - textHeight) / 2f;

        dialogueLabel.setBounds(textX, textY, innerWidth, textHeight);
    }
}
