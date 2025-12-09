package com.gdx.game.ui.popup;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
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
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.gdx.game.DetectiveGame;
import com.gdx.game.data.InvestigationState;
import com.gdx.game.utils.Assets;
import com.gdx.game.utils.FontScaler;
import com.gdx.game.utils.ScreenUtilsHelper;

public class AccusationPopup extends AbstractPopup {
    private final Image accusationBackground;
    private final Texture accusationTexture;
    private final Image accuseButton;
    private final Image closeButton;

    private final DetectiveGame game;
    private final Skin skin;

    private final Image[] portraits;
    private final Label[] names;

    private final String[] characterNames = {"Мара", "Ернст", "Ліам", "Клара", "Елена"};
    private final String[] characterTextures = {
        Assets.DOCTOR_PORTRAIT,
        Assets.OFFICER_PORTRAIT,
        Assets.STUDENT_PORTRAIT,
        Assets.SISTER_PORTRAIT,
        Assets.CASHIER_PORTRAIT
    };

    private final String[] npcIds = {
        "doctor",
        "officer",
        "student",
        "sister",
        "cashier"
    };

    private int selectedIndex = -1;

    public AccusationPopup(Stage stage, DetectiveGame game) {
        super(stage);
        this.game = game;

        accusationTexture = new Texture(Assets.ACCUSATION_POPUP);
        accusationBackground = new Image(accusationTexture);
        skin = new Skin(Gdx.files.internal("ui/uiskin.json"));

        accuseButton = game.getButtonFactory().createButton(Assets.ACCUSE, 60, 60, this::accuse);
        closeButton = game.getButtonFactory().createButton(Assets.CLOSE_BUTTON, 64, 64, this::remove);

        Label.LabelStyle labelStyle = new Label.LabelStyle();
        labelStyle.font = skin.getFont("default-font");
        labelStyle.fontColor = new Color(154 / 255f, 109 / 255f, 69 / 255f, 1f);

        portraits = new Image[characterNames.length];
        names = new Label[characterNames.length];

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

        resize(stage.getViewport().getWorldWidth(), stage.getViewport().getWorldHeight());
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

        if (game.getInvestigationState() != null) {
            InvestigationState inv = game.getInvestigationState();
            inv.accusedNpcId = accusedNpcId;
            inv.accusationDone = true;

            // TODO: accusation
        }

        remove();

        // TODO: epilog popup
    }

    private void updateAccuseButtonState() {
        boolean timeOver = false;
        if (game.overlay.getTimer() != null) {
            timeOver = game.overlay.getTimer().isTimeOver();
        }

        int revealed = game.getNpcDialogueService().getTotalRevealedFacts();

        boolean enoughFacts = revealed >= 10;

        boolean canAccuse = timeOver || enoughFacts;

        accuseButton.setTouchable(canAccuse ? Touchable.enabled : Touchable.disabled);
        accuseButton.setColor(1f, 1f, 1f, canAccuse ? 1f : 0.4f);
    }

    public void resize(float screenWidth, float screenHeight) {
        background.setSize(screenWidth, screenHeight);
        resizeCentered(accusationBackground, accusationTexture, screenWidth, screenHeight);

        FontScaler.applyScale(skin.getFont("default-font"));

        float paddingY = accusationBackground.getHeight() * 0.27f;
        float availableHeight = accusationBackground.getHeight() - 2 * paddingY;

        float maxPortraitHeight = availableHeight * 0.4f;

        float spacingX = maxPortraitHeight * 0.3f;
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

        float btnWidth = accusationBackground.getWidth() * 0.5f;
        float btnHeight = accusationBackground.getHeight() * 0.1f;
        float paddingBottom = accusationBackground.getHeight() * 0.1f;

        accuseButton.setSize(btnWidth, btnHeight);
        accuseButton.setPosition(
            accusationBackground.getX() + (accusationBackground.getWidth() - btnWidth) / 2f,
            accusationBackground.getY() + paddingBottom
        );

        ScreenUtilsHelper.scaleButton(closeButton, screenHeight * 0.12f, stage);
        closeButton.setPosition(10, screenHeight - closeButton.getHeight() - 10);
    }

    @Override
    public void show() {
        super.show();

        selectedIndex = -1;
        for (Image portrait : portraits) {
            portrait.setColor(1f, 1f, 1f, 1f);
            portrait.setScale(1f);
        }

        stage.addActor(accusationBackground);

        for (int i = 0; i < portraits.length; i++) {
            stage.addActor(portraits[i]);
            stage.addActor(names[i]);
        }

        stage.addActor(accuseButton);
        stage.addActor(closeButton);
        resize(stage.getViewport().getWorldWidth(), stage.getViewport().getWorldHeight());

        updateAccuseButtonState();
    }

    @Override
    public void remove() {
        super.remove();
        accusationBackground.remove();
        for (Image portrait : portraits) portrait.remove();
        for (Label name : names) name.remove();
        accuseButton.remove();
        closeButton.remove();
    }

    public void dispose() {
        accusationTexture.dispose();
        skin.dispose();
    }
}
