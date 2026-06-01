package com.gdx.game.screens.interior;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.graphics.Cursor;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
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
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.gdx.game.model.LoreDatabase;
import com.gdx.game.features.investigation.model.NpcDialogueService;
import com.gdx.game.shared.config.Assets;
import com.gdx.game.app.model.GameContext;
import com.gdx.game.shared.config.UiLayout;
import com.gdx.game.shared.config.UiLayoutProfile;
import com.gdx.game.shared.ui.UiStyles;
import com.gdx.game.screens.interior.components.AnswerBubble;
import com.gdx.game.screens.interior.components.DialogueInputPanel;
import com.gdx.game.screens.interior.components.NpcConversationController;
import com.gdx.game.screens.interior.components.NpcStatsHud;
import com.gdx.game.shared.ui.rendering.ScaledBackground;

public class CharacterInteriorScreen implements Screen, GestureDetector.GestureListener {
    private static final String CRIME_SCENE_BUILDING_ID = "professor_house";
    private static final boolean DEBUG_SHOW_ALL_CRIME_SCENE_HINTS = false;
    private static final float HINT_ICON_IDLE_ALPHA = 0.48f;
    private static final float HINT_ICON_HOVER_ALPHA = 0.95f;

    private final GameContext game;
    private final NpcDialogueService npcService;

    private final ScaledBackground background;

    private final String characterId;
    private final String buildingId;
    private final Texture characterTexture;
    private final Image characterImage;

    private final OrthographicCamera camera;
    private final ScreenViewport viewport;
    private final Skin skin;

    private final Stage dialogueStage;
    private final DialogueInputPanel inputPanel;
    private final AnswerBubble answerBubble;
    private final NpcStatsHud statsHud;
    private final NpcConversationController conversationController;
    private final boolean accusationMode;
    private final Array<String> accusationLines = new Array<>();

    private Texture hintIconTexture;
    private Texture cluePopupTexture;
    private Image cluePopupBackground;
    private Label cluePopupLabel;
    private final Array<CrimeSceneHintMarker> crimeSceneHintMarkers = new Array<>();
    private LoreDatabase.CrimeSceneHint activeCrimeSceneHint;
    private final Vector3 projectedHintPosition = new Vector3();

    private float drawWidth, drawHeight;

    private boolean screenActive = false;
    private boolean accusationStarted = false;
    private boolean accusationLoaded = false;
    private boolean accusationAdvancing = false;
    private int accusationLineIndex = 0;

    public CharacterInteriorScreen(GameContext game, String backgroundPath, String characterId, String fullBody,
                                   String buildingId) {
        this(game, backgroundPath, characterId, fullBody, buildingId, false);
    }

    public CharacterInteriorScreen(
        GameContext game,
        String backgroundPath,
        String characterId,
        String fullBody,
        String buildingId,
        boolean accusationMode
    ) {
        this.game = game;
        this.background = new ScaledBackground(backgroundPath, true, false);
        this.characterId = characterId;
        this.buildingId = buildingId;
        this.accusationMode = accusationMode;
        this.characterTexture = fullBody != null && !fullBody.isEmpty()
            ? new Texture(fullBody)
            : null;
        this.npcService = game.getNpcDialogueService();

        camera = new OrthographicCamera();
        viewport = new ScreenViewport(camera);
        skin = new Skin(Gdx.files.internal("ui/uiskin.json"));
        dialogueStage = new Stage(new ScreenViewport());
        dialogueStage.getRoot().addCaptureListener(new InputListener() {
            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                if (!inputPanel.ownsTarget(event.getTarget())) {
                    clearInputFocus();
                }
                return false;
            }
        });

        characterImage = characterTexture != null ? new Image(characterTexture) : new Image();
        dialogueStage.addActor(characterImage);

        statsHud = new NpcStatsHud(dialogueStage, skin);
        inputPanel = new DialogueInputPanel(dialogueStage, skin, game, this::submitQuestion);
        answerBubble = new AnswerBubble(dialogueStage, skin);
        conversationController = new NpcConversationController(game, characterId, buildingId);
    }

    private void clearInputFocus() {
        dialogueStage.setKeyboardFocus(null);
        Gdx.input.setOnscreenKeyboardVisible(false);
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
        conversationController.setActive(!accusationMode);
        game.overlay.setVisible(!accusationMode);
        game.overlay.setInInterior(true);
        game.overlay.setCurrentNpcId(hasInteractiveNpc() ? characterId : null);
        game.overlay.setCurrentInteriorBuildingId(buildingId);
        if (game.getAudioManager() != null) {
            game.getAudioManager().playAmbienceForLocation(buildingId);
        }

        if (!hasInteractiveNpc()) {
            inputPanel.setVisible(false);
            answerBubble.setVisible(false);
            statsHud.setVisible(false);
            characterImage.setVisible(false);
        } else if (accusationMode) {
            inputPanel.setVisible(false);
            statsHud.setVisible(false);
            answerBubble.showThinking();
        }

        if (isCrimeSceneScreen()) {
            setupCrimeSceneHints();
            if (game.getCrimeSceneService() != null) {
                game.getCrimeSceneService().clearPendingForLocation(buildingId);
            }
        }

        resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        if (accusationMode) {
            startAccusationConfrontation();
        }

        Gdx.input.setInputProcessor(new InputMultiplexer(
            game.overlay.getStage(),
            dialogueStage,
            new GestureDetector(this)
        ));
    }

    private void setupCrimeSceneHints() {
        hintIconTexture = new Texture(Assets.HINT_ICON);
        cluePopupTexture = new Texture(Assets.STATISTICS);

        cluePopupBackground = new Image(new NinePatchDrawable(new NinePatch(cluePopupTexture, 32, 32, 32, 32)));
        cluePopupBackground.setVisible(false);
        dialogueStage.addActor(cluePopupBackground);

        cluePopupLabel = new Label("", UiStyles.label(skin, UiStyles.ink()));
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

    @Override
    public void render(float delta) {
        ScreenUtils.clear(0, 0, 0, 1);

        camera.update();
        game.batch.setProjectionMatrix(camera.combined);
        background.render(game.batch);

        updateNpcStateHud();
        updateCrimeSceneHintLayout(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        answerBubble.update(delta);

        dialogueStage.act(delta);
        dialogueStage.draw();

        game.overlay.render(delta);
    }

    private void updateNpcStateHud() {
        if (!hasInteractiveNpc()) return;
        statsHud.update(npcService.getStateForUi(characterId));
    }

    @Override
    public void resize(int width, int height) {
        UiLayoutProfile profile = UiLayout.current(width, height);
        viewport.update(width, height);

        background.resizeToCover(width, height);
        drawWidth = background.getDrawWidth();
        drawHeight = background.getDrawHeight();

        inputPanel.layout(width, height, profile);

        if (hasInteractiveNpc()) {
            float texW = characterTexture.getWidth();
            float texH = characterTexture.getHeight();

            float scaleByWidth = (width * profile.getCharacterWidthRatio()) / texW;
            float scaleByHeight = (height * profile.getCharacterHeightRatio()) / texH;

            float scale = Math.min(scaleByWidth, scaleByHeight);

            float characterDrawW = texW * scale;
            float characterDrawH = texH * scale;

            float charY = height * profile.getCharacterBottomRatio();
            float charX = width  * 0.5f - characterDrawW / 2f;

            characterImage.setBounds(charX, charY, characterDrawW, characterDrawH);
            answerBubble.layout(characterImage, width, height);
            statsHud.layout(charX, charY, characterDrawW, characterDrawH, width, profile);
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
    @Override public boolean tap(float x, float y, int count, int button) {
        if (accusationMode) {
            advanceAccusationLine();
            return true;
        }
        return false;
    }
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
        conversationController.setActive(false);
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
        if (cluePopupTexture != null) {
            cluePopupTexture.dispose();
        }
        inputPanel.dispose();
        answerBubble.dispose();
        statsHud.dispose();
    }

    private void submitQuestion(String question) {
        if (accusationMode) return;

        inputPanel.showQuestion(question);
        inputPanel.setWaiting(true);
        answerBubble.showThinking();
        answerBubble.layout(characterImage, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        conversationController.ask(question, new NpcConversationController.Listener() {
            @Override
            public void onAnswer(String question, String answer) {
                inputPanel.setWaiting(false);
                if (!screenActive) return;

                answerBubble.showText(answer);
                answerBubble.layout(characterImage, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
            }

            @Override
            public void onFactsDiscovered(int count) {
                game.overlay.onNewFactsDiscovered(count);
            }
        });
    }

    private void startAccusationConfrontation() {
        if (accusationStarted) return;
        accusationStarted = true;

        game.getEpilogueService().prewarmEpilogue(game.getInvestigationState());

        new Thread(() -> {
            String text;
            try {
                text = game.getEpilogueService().generateAccusationConfrontation(game.getInvestigationState());
            } catch (Exception e) {
                e.printStackTrace();
                text = "Я сказав усе, що міг.|||Тепер робіть із цим що хочете.";
            }

            Array<String> lines = parseAccusationLines(text);
            Gdx.app.postRunnable(() -> {
                if (!screenActive) return;

                accusationLines.clear();
                accusationLines.addAll(lines);
                accusationLoaded = true;
                accusationLineIndex = 0;
                showCurrentAccusationLine();
            });
        }, "accusation-confrontation-worker").start();
    }

    private Array<String> parseAccusationLines(String text) {
        Array<String> lines = new Array<>();
        if (text == null) {
            lines.add("Я не знаю, що ще вам сказати.");
            return lines;
        }

        String normalized = text.replace("\r", "").trim();
        String[] parts = normalized.contains("|||")
            ? normalized.split("\\|\\|\\|")
            : normalized.split("\n+");

        for (String part : parts) {
            String line = cleanAccusationLine(part);
            if (!line.isEmpty()) {
                lines.add(line);
            }
            if (lines.size >= 3) {
                break;
            }
        }

        if (lines.size == 0) {
            lines.add("Я не знаю, що ще вам сказати.");
        }

        return lines;
    }

    private String cleanAccusationLine(String raw) {
        if (raw == null) return "";

        String line = raw.trim();
        line = line.replaceAll("(?i)^(репліка|зізнання|правда|відповідь)\\s*\\d*\\s*[:\\-—]\\s*", "");
        line = line.replaceAll("^\\d+[.)]\\s*", "");
        line = line.trim();

        while (line.length() >= 2 && (
            (line.startsWith("\"") && line.endsWith("\""))
                || (line.startsWith("“") && line.endsWith("”"))
                || (line.startsWith("«") && line.endsWith("»"))
        )) {
            line = line.substring(1, line.length() - 1).trim();
        }

        return line;
    }

    private void showCurrentAccusationLine() {
        if (accusationLines.size == 0) return;
        String line = accusationLines.get(MathUtils.clamp(accusationLineIndex, 0, accusationLines.size - 1));
        answerBubble.showTypewriterText(line);
        answerBubble.layout(characterImage, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
    }

    private void advanceAccusationLine() {
        if (!accusationLoaded || accusationAdvancing) return;

        if (answerBubble.isTyping()) {
            answerBubble.finishTyping();
            answerBubble.layout(characterImage, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
            return;
        }

        accusationLineIndex++;
        if (accusationLineIndex < accusationLines.size) {
            showCurrentAccusationLine();
            return;
        }

        accusationAdvancing = true;
        game.getNavigator().returnToMapThenShowEpilogue();
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
