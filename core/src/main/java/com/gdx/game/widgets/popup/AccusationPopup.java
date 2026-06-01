package com.gdx.game.widgets.popup;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Cursor;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.gdx.game.app.model.GameContext;
import com.gdx.game.app.navigation.GameFlow;
import com.gdx.game.model.InvestigationState;
import com.gdx.game.shared.config.Assets;
import com.gdx.game.shared.ui.FontScaler;
import com.gdx.game.shared.config.UiLayout;
import com.gdx.game.shared.config.UiLayoutProfile;
import com.gdx.game.shared.ui.UiStyles;
import com.gdx.game.shared.lib.ScreenUtilsHelper;

public class AccusationPopup extends AbstractPopup {
    private final Image accusationBackground;
    private final Texture accusationTexture;
    private final Image accuseButton;
    private final Image closeButton;

    private final GameContext game;
    private final GameFlow flow;

    private final Image[] portraits;
    private final Label[] names;
    private final Label.LabelStyle labelStyle;

    private final String[] characterNames = {"Мара", "Ернст", "Ліам", "Клара", "Елена"};
    private final String[] characterTextures = {
        Assets.DOCTOR_PORTRAIT,
        Assets.OFFICER_PORTRAIT,
        Assets.STUDENT_PORTRAIT,
        Assets.SISTER_PORTRAIT,
        Assets.CASHIER_PORTRAIT
    };

    private final String[] npcIds = {
        "mara",
        "ernst",
        "liam",
        "clara",
        "elena"
    };

    private int selectedIndex = -1;

    public AccusationPopup(Stage stage, GameContext game, GameFlow flow) {
        super(stage);
        this.game = game;
        this.flow = flow;

        accusationTexture = new Texture(Assets.ACCUSATION_POPUP);
        accusationBackground = new Image(accusationTexture);

        accuseButton = game.buttonFactory.createButton(Assets.ACCUSE, 60, 60, this::accuse);
        closeButton = game.buttonFactory.createButton(Assets.CLOSE_BUTTON, 64, 64, this::remove);

        labelStyle = UiStyles.label(skin, UiStyles.parchmentText());

        portraits = new Image[characterNames.length];
        names = new Label[characterNames.length];

        createPortraitGrid(portraits, names);

        resize(stage.getViewport().getWorldWidth(), stage.getViewport().getWorldHeight());
    }

    public void resize(float screenWidth, float screenHeight) {
        UiLayoutProfile profile = UiLayout.current(screenWidth, screenHeight);
        resizeCentered(accusationBackground, accusationTexture, screenWidth, screenHeight);

        FontScaler.applyScale(skin.getFont("default-font"));

        float paddingY = accusationBackground.getHeight() * 0.27f;
        float availableHeight = accusationBackground.getHeight() - 2 * paddingY;

        float maxPortraitHeight = availableHeight * 0.4f;

        float spacingX = maxPortraitHeight * 0.2f;
        float spacingY = maxPortraitHeight * 0.3f;

        int[][] rows = {{0, 1, 2}, {3, 4}};

        float currentY = accusationBackground.getY() + accusationBackground.getHeight() - paddingY - maxPortraitHeight;

        for (int[] row : rows) {
            float rowWidth = 0;
            float[] rowWidths = new float[row.length];
            float[] rowHeights = new float[row.length];

            for (int i = 0; i < row.length; i++) {
                int idx = row[i];
                TextureRegionDrawable drawable = (TextureRegionDrawable) portraits[idx].getDrawable();
                TextureRegion region = drawable.getRegion();
                float aspect = (float) region.getRegionWidth() / region.getRegionHeight();

                float w = maxPortraitHeight * aspect;
                rowWidths[i] = w;
                rowHeights[i] = maxPortraitHeight;
                rowWidth += w;
            }
            rowWidth += spacingX * (row.length - 1);

            float startX = accusationBackground.getX() + (accusationBackground.getWidth() - rowWidth) / 2f;

            for (int i = 0; i < row.length; i++) {
                int idx = row[i];
                float portraitHeight = rowHeights[i];

                portraits[idx].setSize(rowWidths[i], portraitHeight);
                portraits[idx].setPosition(startX, currentY);

                names[idx].setWidth(rowWidths[i]);
                names[idx].setAlignment(Align.center);

                GlyphLayout layout = new GlyphLayout(names[idx].getStyle().font, names[idx].getText());
                float textHeight = layout.height;

                float LABEL_OFFSET = accusationBackground.getHeight() * 0.02f;

                names[idx].setPosition(
                    startX,
                    currentY - textHeight - LABEL_OFFSET
                );

                startX += rowWidths[i] + spacingX;
            }

            currentY -= maxPortraitHeight + spacingY + maxPortraitHeight * 0.1f;
        }

        ScreenUtilsHelper.scaleNavButton(accuseButton, accusationBackground);


        float targetHeight = screenHeight * profile.getPopupButtonHeightRatio();
        float margin = profile.scale(10f);

        ScreenUtilsHelper.scaleButton(closeButton, targetHeight, stage);
        closeButton.setPosition(margin, screenHeight - closeButton.getHeight() - margin);
    }

    @Override
    public void show() {
        super.show();

        selectedIndex = -1;
        for (Image portrait : portraits) {
            portrait.setColor(1f, 1f, 1f, 1f);
            portrait.setScale(1f);
        }

        addPopupActors(accusationBackground);

        for (int i = 0; i < portraits.length; i++) {
            addPopupActors(portraits[i], names[i]);
        }

        addPopupActors(accuseButton, closeButton);
        resize(stage.getViewport().getWorldWidth(), stage.getViewport().getWorldHeight());

        updateAccuseButtonState();
        updateCloseButtonState();
    }

    @Override
    public void remove() {
        super.remove();
        removePopupActors(accusationBackground);
        for (Image portrait : portraits) removePopupActors(portrait);
        for (Label name : names) removePopupActors(name);
        removePopupActors(accuseButton, closeButton);
    }

    public void dispose() {
        accusationTexture.dispose();
        skin.dispose();
    }

    private void createPortraitGrid(Image[] portraits, Label[] names) {
        for (int i = 0; i < characterNames.length; i++) {
            portraits[i] = new Image(new Texture(characterTextures[i]));
            names[i] = new Label(characterNames[i], labelStyle);
            names[i].setAlignment(Align.center);

            final int idx = i;
            portraits[i].addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    onPortraitSelected(idx);
                    event.stop();
                }

                @Override
                public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                    Gdx.graphics.setSystemCursor(Cursor.SystemCursor.Hand);
                }

                @Override
                public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
                    Gdx.graphics.setSystemCursor(Cursor.SystemCursor.Arrow);
                }
            });
        }
    }

    private void onPortraitSelected(int index) {
        selectedIndex = index;

        for (int i = 0; i < portraits.length; i++) {
            if (i == selectedIndex) {
                portraits[i].setColor(1f, 1f, 1f, 1f);
                portraits[i].setScale(1.05f);
            } else {
                portraits[i].setColor(1f, 1f, 1f, 0.55f);
                portraits[i].setScale(1f);
            }
        }
    }

    private void accuse() {
        if (selectedIndex < 0) {
            Gdx.app.log("ACCUSATION", "No suspect selected, ignoring.");
            return;
        }

        String accusedNpcId = npcIds[selectedIndex];
        Gdx.app.log("ACCUSATION", "Accuse button clicked, npcId=" + accusedNpcId);

        if (game.investigationState != null) {
            InvestigationState inv = game.investigationState;
            inv.accusedNpcId = accusedNpcId;
            inv.accusationDone = true;
        }

        remove();
        flow.enterAccusationConfrontation(accusedNpcId);
    }

    private void updateCloseButtonState() {
        boolean timeOver = false;
        if (flow.timer() != null) {
            timeOver = flow.timer().isTimeOver();
        }

        closeButton.setVisible(!timeOver);
    }

    private void updateAccuseButtonState() {
        boolean timeOver = false;
        if (flow.timer() != null) {
            timeOver = flow.timer().isTimeOver();
        }

        int revealed = game.npcDialogueService.getTotalRevealedFacts();

        boolean enoughFacts = revealed >= Assets.FACTS_TO_REVEAL;

        boolean canAccuse = timeOver || enoughFacts;

        accuseButton.setTouchable(canAccuse ? Touchable.enabled : Touchable.disabled);
        accuseButton.setColor(1f, 1f, 1f, canAccuse ? 1f : 0.4f);
    }
}
