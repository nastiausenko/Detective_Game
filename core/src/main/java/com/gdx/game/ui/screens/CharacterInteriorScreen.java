package com.gdx.game.ui.screens;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.graphics.Cursor;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.NinePatch;
import com.badlogic.gdx.input.GestureDetector;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.IntArray;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.gdx.game.DetectiveGame;
import com.gdx.game.domain.investigation.DialogueHistory;
import com.gdx.game.domain.character.DossierData;
import com.gdx.game.domain.world.LoreDatabase;
import com.gdx.game.ai.NpcDialogueService;
import com.gdx.game.domain.character.NpcState;
import com.gdx.game.infrastructure.Assets;
import com.gdx.game.infrastructure.UiLayout;
import com.gdx.game.infrastructure.UiLayoutProfile;
import com.gdx.game.utils.ScreenUtilsHelper;
import com.gdx.game.utils.TiledTextureHelper;

import java.util.Locale;

public class CharacterInteriorScreen implements Screen, GestureDetector.GestureListener {
    private static final String DOCTOR_ID = "walter";
    private static final String CRIME_SCENE_BUILDING_ID = "professor_house";
    private static final boolean DEBUG_SHOW_ALL_CRIME_SCENE_HINTS = false;
    private static final float HINT_ICON_IDLE_ALPHA = 0.48f;
    private static final float HINT_ICON_HOVER_ALPHA = 0.95f;

    private final DetectiveGame game;
    private final NpcDialogueService npcService;

    private final Texture background;
    private float imageWidth, imageHeight;
    private final TiledTextureHelper tiledHelper;

    private final String characterId;
    private final String buildingId;
    private final Texture characterTexture;
    private final Image characterImage;

    private final OrthographicCamera camera;
    private final ScreenViewport viewport;
    private final Skin skin;

    private final Stage dialogueStage;
    private TextField inputField;
    private Label dialogueLabel;
    private Label lastQuestionLabel;
    private Label charCounterLabel;

    private final Texture questionAreaTexture;
    private final Image questionAreaImage;

    private final Image sendButtonImage;

    private final Texture answerAreaTexture;
    private final Image answerAreaImage;

    private final Image statsPanelBackground;
    private final Image fearImage;
    private final Image trustImage;
    private final Image moodImage;
    private Texture hintIconTexture;
    private Image cluePopupBackground;
    private Label cluePopupLabel;
    private final Array<CrimeSceneHintMarker> crimeSceneHintMarkers = new Array<>();
    private LoreDatabase.CrimeSceneHint activeCrimeSceneHint;
    private final Vector3 projectedHintPosition = new Vector3();

    private final Label fearLabel;
    private final Label trustLabel;
    private final Label moodLabel;

    private float drawWidth, drawHeight;

    private String currentResponse = "";
    private String lastSubmittedQuestion = "";
    private boolean waitingForAnswer = false;
    private boolean screenActive = false;
    private final GlyphLayout glyphLayout = new GlyphLayout();

    public CharacterInteriorScreen(DetectiveGame game, String backgroundPath, String characterId, String fullBody,
                                   String buildingId) {
        this.game = game;
        this.background = new Texture(backgroundPath);
        this.characterId = characterId;
        this.buildingId = buildingId;
        this.characterTexture = fullBody != null && !fullBody.isEmpty()
            ? new Texture(fullBody)
            : null;
        this.npcService = game.getNpcDialogueService();

        camera = new OrthographicCamera();
        viewport = new ScreenViewport(camera);
        tiledHelper = new TiledTextureHelper(background, 256);
        skin = new Skin(Gdx.files.internal("ui/uiskin.json"));
        dialogueStage = new Stage(new ScreenViewport());
        dialogueStage.getRoot().addCaptureListener(new InputListener() {
            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                if (!isInputFieldTarget(event.getTarget())) {
                    clearInputFocus();
                }
                return false;
            }
        });

        Texture statsTex = new Texture(Assets.STATISTICS);
        NinePatch statsPatch = new NinePatch(statsTex, 32, 32, 32, 32);
        statsPanelBackground = new Image(new NinePatchDrawable(statsPatch));
        dialogueStage.addActor(statsPanelBackground);
        statsPanelBackground.toBack();

        characterImage = characterTexture != null ? new Image(characterTexture) : new Image();
        dialogueStage.addActor(characterImage);

        questionAreaTexture = new Texture(Assets.QUESTION_AREA);

        int left = 60;
        int right = 60;
        int top = 16;
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

        fearImage = new Image(new Texture(Assets.FEAR_ICON));
        trustImage = new Image(new Texture(Assets.TRUST_ICON));
        moodImage = new Image(new Texture(Assets.MOOD_ICON));

        fearLabel = new Label("", skin);
        trustLabel = new Label("", skin);
        moodLabel = new Label("", skin);

        fearLabel.setColor(Color.BLACK);
        trustLabel.setColor(Color.BLACK);
        moodLabel.setColor(Color.BLACK);

        fearLabel.setAlignment(Align.left);
        trustLabel.setAlignment(Align.left);
        moodLabel.setAlignment(Align.left);

        dialogueStage.addActor(fearImage);
        dialogueStage.addActor(trustImage);
        dialogueStage.addActor(moodImage);

        dialogueStage.addActor(fearLabel);
        dialogueStage.addActor(trustLabel);
        dialogueStage.addActor(moodLabel);
    }

    private void send() {
        if (inputField == null) {
            return;
        }

        if (waitingForAnswer) {
            return;
        }

        String text = inputField.getText();

        if (text == null || text.trim().isEmpty()) {
            inputField.setMessageText("Введіть питання");
            inputField.setText("");
            updateCharCounter();
            return;
        }

        inputField.setMessageText("Запитайте персонажа...");
        handleQuestion(text.trim());
    }

    private boolean isInputFieldTarget(Actor target) {
        return inputField != null
            && target != null
            && (target == inputField || target.isDescendantOf(inputField));
    }

    private void clearInputFocus() {
        dialogueStage.setKeyboardFocus(null);
        Gdx.input.setOnscreenKeyboardVisible(false);
    }

    private void updateInputField() {
        if (inputField == null) {
            return;
        }
        inputField.setTextFieldFilter((textField, c) -> {
            if (c == '\n' || c == '\r') return true;
            return inputField.getText().length() < Assets.MAX_CHARS_INPUT;
        });
        updateCharCounter();
    }

    private void updateCharCounter() {
        if (charCounterLabel == null || inputField == null) {
            return;
        }
        int length = inputField.getText() != null ? inputField.getText().length() : 0;
        charCounterLabel.setText(length + "/" + Assets.MAX_CHARS_INPUT);
    }

    private void updateLastQuestionLabel() {
        if (lastQuestionLabel == null) {
            return;
        }
        lastQuestionLabel.setText(lastSubmittedQuestion);
        lastQuestionLabel.setVisible(lastSubmittedQuestion != null && !lastSubmittedQuestion.isEmpty());
    }

    private boolean hasInteractiveNpc() {
        return characterId != null && !characterId.isEmpty() && characterTexture != null;
    }

    private boolean isCrimeSceneScreen() {
        return CRIME_SCENE_BUILDING_ID.equals(buildingId) && !hasInteractiveNpc();
    }

    @Override
    public void show() {
        screenActive = true;
        game.overlay.setVisible(true);
        game.overlay.setInInterior(true);
        game.overlay.setCurrentNpcId(hasInteractiveNpc() ? characterId : null);
        game.overlay.setCurrentInteriorBuildingId(buildingId);
        playLocationAmbience();

        imageWidth = background.getWidth();
        imageHeight = background.getHeight();

        dialogueLabel = new Label("", skin);
        dialogueLabel.setWrap(true);
        dialogueLabel.setColor(Color.BLACK);
        dialogueLabel.setVisible(false);

        lastQuestionLabel = new Label("", skin);
        lastQuestionLabel.setWrap(true);
        lastQuestionLabel.setAlignment(Align.left);
        lastQuestionLabel.setColor(new Color(0.82f, 0.69f, 0.47f, 1f));
        lastQuestionLabel.setVisible(false);

        charCounterLabel = new Label("", skin);
        charCounterLabel.setAlignment(Align.right);
        charCounterLabel.setColor(new Color(0.32f, 0.18f, 0.08f, 0.85f));

        inputField = new TextField("", createTextFieldStyle());
        inputField.setMessageText("Запитайте персонажа...");
        inputField.setWidth(Gdx.graphics.getWidth() * 0.8f);
        inputField.setPosition(Gdx.graphics.getWidth() * 0.1f, 40);
        updateInputField();
        updateLastQuestionLabel();

        inputField.setTextFieldListener((textField, c) -> {
            if (c == '\n' || c == '\r') {
                send();
                return;
            }
            inputField.setMessageText("Запитайте персонажа...");
            updateCharCounter();
        });

        dialogueStage.addActor(dialogueLabel);
        dialogueStage.addActor(lastQuestionLabel);
        dialogueStage.addActor(inputField);
        dialogueStage.addActor(charCounterLabel);

        if (!hasInteractiveNpc()) {
            questionAreaImage.setVisible(false);
            sendButtonImage.setVisible(false);
            inputField.setVisible(false);
            inputField.setDisabled(true);
            lastQuestionLabel.setVisible(false);
            charCounterLabel.setVisible(false);
            answerAreaImage.setVisible(false);
            dialogueLabel.setVisible(false);
            statsPanelBackground.setVisible(false);
            trustImage.setVisible(false);
            fearImage.setVisible(false);
            moodImage.setVisible(false);
            trustLabel.setVisible(false);
            fearLabel.setVisible(false);
            moodLabel.setVisible(false);
            characterImage.setVisible(false);
        }

        if (isCrimeSceneScreen()) {
            setupCrimeSceneHints();
            if (game.getCrimeSceneService() != null) {
                game.getCrimeSceneService().clearPendingForLocation(buildingId);
            }
        }

        resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        Gdx.input.setInputProcessor(new InputMultiplexer(
            game.overlay.getStage(),
            dialogueStage,
            new GestureDetector(this)
        ));
    }

    private void setupCrimeSceneHints() {
        hintIconTexture = new Texture(Assets.HINT_ICON);

        NinePatch cluePatch = new NinePatch(questionAreaTexture, 60, 60, 16, 16);
        cluePopupBackground = new Image(new NinePatchDrawable(cluePatch));
        cluePopupBackground.setVisible(false);
        dialogueStage.addActor(cluePopupBackground);

        Label.LabelStyle clueStyle = new Label.LabelStyle();
        clueStyle.font = skin.getFont("default-font");
        clueStyle.fontColor = Color.BLACK;

        cluePopupLabel = new Label("", clueStyle);
        cluePopupLabel.setWrap(true);
        cluePopupLabel.setAlignment(Align.left);
        cluePopupLabel.setVisible(false);
        dialogueStage.addActor(cluePopupLabel);

        Array<LoreDatabase.CrimeSceneHint> visibleHints = game.getCrimeSceneService() != null
            ? (DEBUG_SHOW_ALL_CRIME_SCENE_HINTS
                ? game.getCrimeSceneService().getHintsForLocation(buildingId)
                : game.getCrimeSceneService().getUnlockedHintsForLocation(buildingId))
            : new Array<>();

        for (LoreDatabase.CrimeSceneHint hint : visibleHints) {
            createCrimeSceneHintMarker(hint);
        }
    }

    private void createCrimeSceneHintMarker(LoreDatabase.CrimeSceneHint hint) {
        if (hint == null) {
            return;
        }

        Image hintIcon = new Image(hintIconTexture);
        hintIcon.getColor().a = HINT_ICON_IDLE_ALPHA;
        hintIcon.addListener(new InputListener() {
            @Override
            public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                hintIcon.getColor().a = HINT_ICON_HOVER_ALPHA;
                Gdx.graphics.setSystemCursor(Cursor.SystemCursor.Hand);
            }

            @Override
            public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
                if (activeCrimeSceneHint == hint) {
                    hintIcon.getColor().a = HINT_ICON_HOVER_ALPHA;
                } else {
                    hintIcon.getColor().a = HINT_ICON_IDLE_ALPHA;
                }
                Gdx.graphics.setSystemCursor(Cursor.SystemCursor.Arrow);
            }

            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                toggleCrimeSceneHint(hint);
                event.stop();
                return true;
            }
        });

        crimeSceneHintMarkers.add(new CrimeSceneHintMarker(hint, hintIcon));
        dialogueStage.addActor(hintIcon);
    }

    private void toggleCrimeSceneHint(LoreDatabase.CrimeSceneHint hint) {
        if (hint == null) {
            hideCrimeSceneHintPopup();
            return;
        }

        if (activeCrimeSceneHint == hint) {
            hideCrimeSceneHintPopup();
            return;
        }

        activeCrimeSceneHint = hint;
        if (cluePopupLabel != null) {
            cluePopupLabel.setText(hint.text != null ? hint.text : "");
            cluePopupLabel.setVisible(true);
        }
        if (cluePopupBackground != null) {
            cluePopupBackground.setVisible(true);
            cluePopupBackground.toFront();
        }
        if (cluePopupLabel != null) {
            cluePopupLabel.toFront();
        }

        for (CrimeSceneHintMarker marker : crimeSceneHintMarkers) {
            marker.icon.getColor().a = marker.hint == hint ? HINT_ICON_HOVER_ALPHA : HINT_ICON_IDLE_ALPHA;
        }

        updateCrimeSceneHintLayout(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
    }

    private void hideCrimeSceneHintPopup() {
        activeCrimeSceneHint = null;
        if (cluePopupBackground != null) {
            cluePopupBackground.setVisible(false);
        }
        if (cluePopupLabel != null) {
            cluePopupLabel.setVisible(false);
        }
        for (CrimeSceneHintMarker marker : crimeSceneHintMarkers) {
            marker.icon.getColor().a = HINT_ICON_IDLE_ALPHA;
        }
    }

    private void playLocationAmbience() {
        if (game.getAudioManager() == null || buildingId == null) return;

        switch (buildingId) { //TODO change switch case
            case "hospital":
                game.getAudioManager().playAmbience(Assets.SOUND_HOSPITAL);
                break;
            case "cafe":
                game.getAudioManager().playAmbience(Assets.SOUND_CAFE);
                break;
            default:
                break;
        }
    }

    private TextField.TextFieldStyle createTextFieldStyle() {
        TextField.TextFieldStyle style = new TextField.TextFieldStyle();
        style.font = skin.getFont("default-font");
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

        updateNpcStateHud();
        updateCrimeSceneHintLayout(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        dialogueStage.act(delta);
        dialogueStage.draw();

        game.overlay.render(delta);
    }

    private void updateNpcStateHud() {
        if (!hasInteractiveNpc()) return;
        if (trustLabel == null || fearLabel == null || moodLabel == null) return;

        NpcState state = npcService.getStateForUi(characterId);

        if (state == null) return;
        float trust = state.trust;
        float fear  = state.fear;

        int trustPercent = Math.round(trust * 100f);
        int fearPercent = Math.round(fear  * 100f);

        trustLabel.setText("Довіра: " + trustPercent + "%");
        fearLabel.setText("Страх: " + fearPercent + "%");

        String mood;
        if (fear > 0.75f) {
            mood = "Наляканий";
        } else if (trust > 0.75f && fear < 0.4f) {
            mood = "Довірливий";
        } else if (trust < 0.3f && fear < 0.4f) {
            mood = "Закритий";
        } else {
            mood = "Напружений";
        }
        moodLabel.setText("Стан: " + mood);

        layoutStatsPanel();
    }

    @Override
    public void resize(int width, int height) {
        UiLayoutProfile profile = UiLayout.current(width, height);
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

        float desiredWidth = width * profile.getQuestionAreaWidthRatio();
        float designHeight = questionAreaTexture.getHeight() * (height / 1000f);
        float desiredHeight = Math.max(height * profile.getQuestionAreaHeightRatio(), designHeight);
        float questionBottom = height * profile.getQuestionAreaBottomMarginRatio();

        questionAreaImage.setSize(desiredWidth, desiredHeight);
        questionAreaImage.setPosition(
            (width - desiredWidth) / 2f,
            questionBottom
        );

        float innerMarginX = desiredWidth * 0.04f;
        float bubbleX = questionAreaImage.getX();
        float bubbleY = questionAreaImage.getY();
        float bubbleH = questionAreaImage.getHeight();
        float bubbleW = questionAreaImage.getWidth();

        float sendTargetH = bubbleH * 0.6f;
        ScreenUtilsHelper.scaleButton(sendButtonImage, sendTargetH, dialogueStage);

        float sendX = bubbleX + bubbleW - innerMarginX - sendButtonImage.getWidth();
        float sendY = bubbleY + (bubbleH - sendButtonImage.getHeight()) / 2f;
        sendButtonImage.setPosition(sendX, sendY);

        float sidePadding = Math.max(profile.scale(18f), desiredWidth * 0.05f);
        float gapToSend = profile.scale(14f);
        float inputX = bubbleX + sidePadding;
        float inputWidth = Math.max(profile.scale(120f), sendX - gapToSend - inputX);

        float topPadding = profile.scale(12f);
        float bottomPadding = profile.scale(10f);
        float questionGap = profile.scale(8f);
        float counterGap = profile.scale(4f);
        float questionMaxHeight = bubbleH * 0.34f;

        lastQuestionLabel.setFontScale(profile.getBubbleFontScale() * 0.78f);
        lastQuestionLabel.setWidth(inputWidth);
        lastQuestionLabel.invalidateHierarchy();
        float questionHeight = 0f;
        if (lastQuestionLabel.isVisible()) {
            questionHeight = Math.min(lastQuestionLabel.getPrefHeight(), questionMaxHeight);
        }

        float questionY = bubbleY + bubbleH - topPadding - questionHeight;
        if (lastQuestionLabel.isVisible()) {
            lastQuestionLabel.setBounds(inputX, questionY, inputWidth, questionHeight);
        } else {
            lastQuestionLabel.setBounds(inputX, bubbleY + bubbleH, inputWidth, 0f);
        }

        charCounterLabel.setFontScale(profile.getBubbleFontScale() * 0.52f);
        charCounterLabel.invalidateHierarchy();
        float counterWidth = charCounterLabel.getPrefWidth();
        float counterHeight = charCounterLabel.getPrefHeight();
        float counterX = inputX + inputWidth - counterWidth;
        float counterY = bubbleY + bottomPadding;
        charCounterLabel.setBounds(counterX, counterY, counterWidth, counterHeight);

        float inputTop = (lastQuestionLabel.isVisible() ? questionY - questionGap : bubbleY + bubbleH - topPadding);
        float inputBottom = counterY + counterHeight + counterGap;
        float inputHeight = Math.max(profile.scale(28f), inputTop - inputBottom);

        inputField.setSize(inputWidth, inputHeight);
        inputField.setPosition(inputX, inputBottom);
        inputField.setStyle(createTextFieldStyle());

        if (hasInteractiveNpc()) {
            float texW = characterTexture.getWidth();
            float texH = characterTexture.getHeight();

            float scaleByWidth = (width * profile.getCharacterWidthRatio()) / texW;
            float scaleByHeight = (height * profile.getCharacterHeightRatio()) / texH;

            float scale = Math.min(scaleByWidth, scaleByHeight);

            float characterDrawW = texW * scale;
            float characterDrawH = texH * scale;

            float iconSize = characterDrawH * 0.10f;
            float lineGap = iconSize + profile.scale(8f);

            updateStatsFontScale(iconSize);

            float charY = height * profile.getCharacterBottomRatio();
            float charX = width  * 0.5f - characterDrawW / 2f;

            float panelX = charX + characterDrawW + profile.scale(20f);
            float panelY = charY + characterDrawH * 0.6f;

            if (panelX + iconSize + profile.scale(80f) > width) {
                panelX = charX - iconSize - profile.scale(90f);
            }

            trustImage.setBounds(panelX, panelY + lineGap, iconSize, iconSize);
            trustLabel.setPosition(
                trustImage.getX() + iconSize + profile.scale(6f),
                trustImage.getY() + iconSize * 0.4f
            );

            fearImage.setBounds(panelX, panelY, iconSize, iconSize);
            fearLabel.setPosition(
                fearImage.getX() + iconSize + profile.scale(6f),
                fearImage.getY() + iconSize * 0.4f
            );

            moodImage.setBounds(panelX, panelY - lineGap, iconSize, iconSize);
            moodLabel.setPosition(
                moodImage.getX() + iconSize + profile.scale(6f),
                moodImage.getY() + iconSize * 0.4f
            );

            characterImage.setBounds(charX, charY, characterDrawW, characterDrawH);
            updateAnswerBubbleLayout(width, height);
            layoutStatsPanel();
        }

        dialogueStage.getViewport().update(width, height, true);

        camera.position.set(drawWidth / 2f, drawHeight / 2f, 0);
        camera.update();
        updateCrimeSceneHintLayout(width, height);

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
        screenActive = false;
        hideCrimeSceneHintPopup();
        game.overlay.hideAllPopups();
        game.overlay.setInInterior(false);
    }

    @Override
    public void dispose() {
        screenActive = false;
        background.dispose();
        if (characterTexture != null) {
            characterTexture.dispose();
        }
        if (hintIconTexture != null) {
            hintIconTexture.dispose();
        }
    }

    private void handleQuestion(String question) {
        if (question == null || question.trim().isEmpty()) return;

        currentResponse = "...";
        dialogueLabel.setText(currentResponse);
        dialogueLabel.setVisible(true);
        answerAreaImage.setVisible(true);
        updateAnswerBubbleLayout(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        final String q = question.trim();
        lastSubmittedQuestion = q;
        updateLastQuestionLabel();

        if (inputField != null) {
            inputField.setText("");
            updateCharCounter();
        }

        waitingForAnswer = true;
        inputField.setDisabled(true);

        final DossierData npcData = game.getDossierDb().characters.get(characterId);
        final DossierData doctorData = game.getDossierDb().characters.get(DOCTOR_ID);

        npcService.askNpcAsync(characterId, q, buildingId).whenComplete((answer, error) -> {
            String response = answer != null ? answer : "";

            if (error != null) {
                error.printStackTrace();
                response = "Вибач, але я зараз не можу відповісти.";
            }

            final String finalAnswer = response;
            showAnswer(q, finalAnswer);

            if (error == null) {
                scheduleFactRevealCheck(q, finalAnswer, npcData, doctorData);
            }
        });
    }

    private void showAnswer(String question, String answer) {
        Gdx.app.postRunnable(() -> {
            waitingForAnswer = false;

            if (inputField != null) {
                inputField.setDisabled(false);
            }

            if (screenActive) {
                currentResponse = answer;
                dialogueLabel.setText(currentResponse);
                dialogueLabel.setVisible(true);
                answerAreaImage.setVisible(true);
                updateAnswerBubbleLayout(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
                DialogueHistory.append(characterId, question, answer);
            }
        });
    }

    private void scheduleFactRevealCheck(
        String question,
        String answer,
        DossierData npcData,
        DossierData doctorData
    ) {
        npcService.runFactRevealCheckAsync(() -> {
            IntArray candidateFactsNpc = collectSemanticCandidateFacts(
                characterId, npcData, question, answer, "npc=" + characterId
            );
            IntArray candidateFactsDoctor = collectSemanticCandidateFacts(
                DOCTOR_ID, doctorData, question, answer, "DOCTOR"
            );

            int newlyRevealedTotal = 0;

            newlyRevealedTotal += revealFactsAfterExchange(characterId, npcData, question, answer,
                candidateFactsNpc, "npc=" + characterId
            );

            newlyRevealedTotal += revealFactsAfterExchange(DOCTOR_ID, doctorData, question, answer,
                candidateFactsDoctor, "DOCTOR");

            if (newlyRevealedTotal > 0 && game.getCrimeSceneService() != null) {
                game.getCrimeSceneService().syncUnlockedHints();
            }

            if (newlyRevealedTotal > 0) {
                int finalNew = newlyRevealedTotal;
                Gdx.app.postRunnable(() ->
                    game.overlay.onNewFactsDiscovered(finalNew));
            }
        }).whenComplete((ignored, error) -> {
            if (error != null) {
                error.printStackTrace();
            }
        });
    }

    private IntArray collectSemanticCandidateFacts(
        String npcId,
        DossierData data,
        String question,
        String answer,
        String debugPrefix
    ) {
        try {
            IntArray candidates = npcService.findRelevantHiddenFacts(npcId, data, question, answer);
            if (candidates.size == 0) {
                Gdx.app.log("FACT_DEBUG", "No semantic candidates (" + debugPrefix + ")");
            }
            return candidates;
        } catch (Exception ex) {
            ex.printStackTrace();
            Gdx.app.log("FACT_DEBUG",
                "Semantic retrieval failed; skipping fact check (" + debugPrefix + ")");
            return new IntArray();
        }
    }

    private int revealFactsAfterExchange(String npcId, DossierData data, String question, String answer,
                                         IntArray candidateIndexes, String debugPrefix) {
        if (data == null || data.hiddenFacts == null || data.hiddenFacts.isEmpty()) return 0;
        if (candidateIndexes == null || candidateIndexes.size == 0) return 0;

        IntArray toReveal = new IntArray();

        for (int i = 0; i < candidateIndexes.size; i++) {
            int idx = candidateIndexes.get(i);
            if (idx < 0 || idx >= data.hiddenFacts.size()) continue;

            DossierData.HiddenFactData hiddenFact = data.getHiddenFact(idx);
            String hidden = data.getHiddenFactText(idx);
            if (hidden.isEmpty()) continue;

            if (shouldVetoRevealByLocalEvidence(hiddenFact, answer)) {
                Gdx.app.log("FACT_DEBUG",
                    "LOCAL_REVEAL_VETO (" + debugPrefix + ") fact #" + idx + " -> true");
                Gdx.app.log("FACT_DEBUG",
                    "REVEAL_CHECK (" + debugPrefix + ") fact #" + idx + " -> false");
                continue;
            }

            boolean logical = false;
            try {
                logical = npcService.shouldRevealFactFromExchange(question, answer, hidden);
            } catch (Exception ex) {
                ex.printStackTrace();
            }

            Gdx.app.log("FACT_DEBUG",
                "REVEAL_CHECK (" + debugPrefix + ") fact #" + idx + " -> " + logical);

            if (logical) {
                toReveal.add(idx);
            }
        }

        if (toReveal.size > 0) {
            npcService.markFactsRevealed(npcId, toReveal);
        }

        return toReveal.size;
    }

    private boolean shouldVetoRevealByLocalEvidence(DossierData.HiddenFactData hiddenFact, String answer) {
        if (hiddenFact == null) return true;

        String answerLower = normalizeForFactMatch(answer);
        if (answerLower.isEmpty()) return true;
        if (!hasEvidenceRequirements(hiddenFact)) return false;

        boolean anyOk = hiddenFact.requiredEvidenceAny == null || hiddenFact.requiredEvidenceAny.isEmpty();
        if (!anyOk) {
            anyOk = containsAny(answerLower, hiddenFact.requiredEvidenceAny);
        }

        boolean groupsOk = true;
        if (hiddenFact.requiredEvidenceAllGroups != null && !hiddenFact.requiredEvidenceAllGroups.isEmpty()) {
            for (java.util.List<String> group : hiddenFact.requiredEvidenceAllGroups) {
                if (group == null || group.isEmpty()) {
                    continue;
                }
                if (!containsAny(answerLower, group)) {
                    groupsOk = false;
                    break;
                }
            }
        }

        return !(anyOk && groupsOk);
    }

    private boolean hasEvidenceRequirements(DossierData.HiddenFactData hiddenFact) {
        return hiddenFact != null && (
            (hiddenFact.requiredEvidenceAny != null && !hiddenFact.requiredEvidenceAny.isEmpty())
                || (hiddenFact.requiredEvidenceAllGroups != null && !hiddenFact.requiredEvidenceAllGroups.isEmpty())
        );
    }

    private boolean containsAny(String text, String... needles) {
        if (text == null || text.isEmpty() || needles == null) return false;
        for (String needle : needles) {
            if (needle == null || needle.isEmpty()) continue;
            if (text.contains(needle)) return true;
        }
        return false;
    }

    private boolean containsAny(String text, java.util.List<String> needles) {
        if (text == null || text.isEmpty() || needles == null || needles.isEmpty()) return false;
        for (String needle : needles) {
            if (needle == null || needle.isEmpty()) continue;
            if (text.contains(needle)) return true;
        }
        return false;
    }

    private String normalizeForFactMatch(String text) {
        if (text == null) return "";
        return text.toLowerCase(Locale.ROOT)
            .replace('’', '\'')
            .replace('`', '\'')
            .trim();
    }

    private void updateAnswerBubbleLayout(float screenWidth, float screenHeight) {
        if (!dialogueLabel.isVisible()) return;

        UiLayoutProfile profile = UiLayout.current(screenWidth, screenHeight);
        float paddingX = profile.scale(40f);
        float paddingY = profile.scale(16f);
        float tailHeight = profile.scale(21f);

        boolean portrait = screenHeight > screenWidth;

        float charW = characterImage.getWidth();
        float charH = characterImage.getHeight();
        float charX = characterImage.getX();
        float charY = characterImage.getY();

        float anchorRight = charX + charW * 0.5f - paddingX;

        float maxBubbleWidth = answerAreaTexture.getWidth() * 0.4f;
        float screenLimit = screenWidth * (portrait
            ? profile.getBubbleWidthPortraitRatio()
            : profile.getBubbleWidthLandscapeRatio());
        maxBubbleWidth = Math.min(maxBubbleWidth, screenLimit);
        maxBubbleWidth = Math.min(maxBubbleWidth, anchorRight - paddingX);

        Label.LabelStyle style = dialogueLabel.getStyle();
        String text = currentResponse != null ? currentResponse : "";

        glyphLayout.setText(style.font, text);
        float singleLineWidth = glyphLayout.width;

        float maxInnerWidth = maxBubbleWidth - paddingX * 2f;
        if (maxInnerWidth < 0f) maxInnerWidth = 0f;

        float innerWidth = Math.min(singleLineWidth, maxInnerWidth);

        float maxBubbleHeight = screenHeight * 0.45f;
        float maxInnerHeight  = maxBubbleHeight - paddingY * 2f - tailHeight;

        dialogueLabel.setWrap(true);
        dialogueLabel.setAlignment(Align.center);
        dialogueLabel.setFontScale(profile.getBubbleFontScale());
        dialogueLabel.setWidth(innerWidth);
        dialogueLabel.setText(text);
        dialogueLabel.layout();
        float baseTextHeight = dialogueLabel.getPrefHeight();

        float scale;
        float textHeight = baseTextHeight;

        if (baseTextHeight > maxInnerHeight && baseTextHeight > 0f) {
            scale = maxInnerHeight / baseTextHeight;
            scale = MathUtils.clamp(scale, 0.5f, 1f);

            dialogueLabel.setFontScale(scale);
            dialogueLabel.invalidateHierarchy();
            dialogueLabel.layout();
            textHeight = dialogueLabel.getPrefHeight();
        }

        float bubbleWidth  = innerWidth + paddingX * 2f;
        float bubbleHeight = textHeight + paddingY * 2f + tailHeight;

        float bubbleX = anchorRight - bubbleWidth;
        float bubbleY = charY + charH * 0.6f;

        bubbleY = MathUtils.clamp(bubbleY, 0, screenHeight - bubbleHeight);

        answerAreaImage.setBounds(bubbleX, bubbleY, bubbleWidth, bubbleHeight);

        float innerHeightActual = bubbleHeight - paddingY * 2f - tailHeight;

        float textX = bubbleX + paddingX;
        float textY = bubbleY + tailHeight + paddingY
            + (innerHeightActual - textHeight) / 2f;

        dialogueLabel.setBounds(textX, textY, innerWidth, textHeight);
    }

    private void layoutStatsPanel() {
        if (statsPanelBackground == null) return;

        float padX = 25f;
        float padY = 25f;

        float minX = Math.min(trustImage.getX(),
            Math.min(fearImage.getX(), moodImage.getX())) - padX;

        float trustRight = trustLabel.getX() + trustLabel.getPrefWidth();
        float fearRight  = fearLabel.getX()  + fearLabel.getPrefWidth();
        float moodRight  = moodLabel.getX()  + moodLabel.getPrefWidth();

        float maxRight = Math.max(trustRight, Math.max(fearRight, moodRight)) + padX;

        float minY = moodImage.getY() - padY;

        float maxTop = trustImage.getY() + trustImage.getHeight() + padY;

        float panelW = maxRight - minX;
        float panelH = maxTop - minY;

        statsPanelBackground.setBounds(minX, minY, panelW, panelH);
        statsPanelBackground.toBack();
    }

    private void updateStatsFontScale(float iconSize) {
        UiLayoutProfile profile = UiLayout.current();
        float baseIconSize = 80f;
        float scale = (iconSize / baseIconSize) * profile.getFontScaleMultiplier();

        scale = MathUtils.clamp(scale, 0.6f, 1.8f);

        trustLabel.setFontScale(scale);
        fearLabel.setFontScale(scale);
        moodLabel.setFontScale(scale);

        trustLabel.invalidateHierarchy();
        fearLabel.invalidateHierarchy();
        moodLabel.invalidateHierarchy();
    }

    private void updateCrimeSceneHintLayout(int screenWidth, int screenHeight) {
        if (!isCrimeSceneScreen()) {
            return;
        }

        UiLayoutProfile profile = UiLayout.current(screenWidth, screenHeight);
        float iconSize = Math.max(profile.scale(24f), screenHeight * 0.042f);

        CrimeSceneHintMarker activeMarker = null;
        for (CrimeSceneHintMarker marker : crimeSceneHintMarkers) {
            float worldX = marker.hint.iconX * drawWidth;
            float worldY = marker.hint.iconY * drawHeight;

            projectedHintPosition.set(worldX, worldY, 0f);
            camera.project(projectedHintPosition, 0, 0, screenWidth, screenHeight);

            boolean onScreen =
                projectedHintPosition.x + iconSize >= 0f
                    && projectedHintPosition.x - iconSize <= screenWidth
                    && projectedHintPosition.y + iconSize >= 0f
                    && projectedHintPosition.y - iconSize <= screenHeight;

            marker.icon.setVisible(onScreen);
            marker.icon.setSize(iconSize, iconSize);
            marker.icon.setPosition(
                projectedHintPosition.x - iconSize * 0.5f,
                projectedHintPosition.y - iconSize * 0.5f
            );

            if (marker.hint == activeCrimeSceneHint) {
                activeMarker = marker;
            }
        }

        if (activeMarker == null || !activeMarker.icon.isVisible()) {
            if (cluePopupBackground != null) {
                cluePopupBackground.setVisible(false);
            }
            if (cluePopupLabel != null) {
                cluePopupLabel.setVisible(false);
            }
            return;
        }

        layoutActiveCrimeScenePopup(activeMarker, screenWidth, screenHeight, profile);
    }

    private void layoutActiveCrimeScenePopup(
        CrimeSceneHintMarker marker,
        int screenWidth,
        int screenHeight,
        UiLayoutProfile profile
    ) {
        if (cluePopupBackground == null || cluePopupLabel == null || activeCrimeSceneHint == null) {
            return;
        }

        float maxBubbleWidth = Math.min(screenWidth * 0.34f, profile.scale(360f));
        float padX = profile.scale(16f);
        float padY = profile.scale(12f);
        float margin = profile.scale(10f);
        float iconGap = profile.scale(8f);

        cluePopupLabel.setFontScale(profile.getBubbleFontScale() * 0.72f);
        cluePopupLabel.setWidth(maxBubbleWidth - padX * 2f);
        cluePopupLabel.setText(activeCrimeSceneHint.text != null ? activeCrimeSceneHint.text : "");
        cluePopupLabel.invalidateHierarchy();
        cluePopupLabel.layout();

        float textHeight = cluePopupLabel.getPrefHeight();
        float bubbleWidth = cluePopupLabel.getWidth() + padX * 2f;
        float bubbleHeight = textHeight + padY * 2f;

        float bubbleX = marker.icon.getX() + marker.icon.getWidth() + iconGap;
        if (bubbleX + bubbleWidth > screenWidth - margin) {
            bubbleX = marker.icon.getX() - bubbleWidth - iconGap;
        }
        bubbleX = MathUtils.clamp(bubbleX, margin, screenWidth - bubbleWidth - margin);

        float bubbleY = marker.icon.getY() + marker.icon.getHeight() * 0.15f;
        bubbleY = MathUtils.clamp(bubbleY, margin, screenHeight - bubbleHeight - margin);

        cluePopupBackground.setVisible(true);
        cluePopupLabel.setVisible(true);
        cluePopupBackground.setBounds(bubbleX, bubbleY, bubbleWidth, bubbleHeight);
        cluePopupLabel.setBounds(
            bubbleX + padX,
            bubbleY + padY,
            bubbleWidth - padX * 2f,
            textHeight
        );
        cluePopupBackground.toFront();
        cluePopupLabel.toFront();
        marker.icon.toFront();
    }

    private static class CrimeSceneHintMarker {
        final LoreDatabase.CrimeSceneHint hint;
        final Image icon;

        CrimeSceneHintMarker(LoreDatabase.CrimeSceneHint hint, Image icon) {
            this.hint = hint;
            this.icon = icon;
        }
    }
}
