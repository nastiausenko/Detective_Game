package com.gdx.game.ui.component.interior;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.NinePatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable;
import com.badlogic.gdx.utils.Align;
import com.gdx.game.domain.character.NpcState;
import com.gdx.game.infrastructure.Assets;
import com.gdx.game.infrastructure.UiLayout;
import com.gdx.game.infrastructure.UiLayoutProfile;
import com.gdx.game.infrastructure.UiStyles;

public class NpcStatsHud {
    private final Texture statsTexture;
    private final Texture fearTexture;
    private final Texture trustTexture;
    private final Texture moodTexture;

    private final Image background;
    private final Image fearImage;
    private final Image trustImage;
    private final Image moodImage;
    private final Label fearLabel;
    private final Label trustLabel;
    private final Label moodLabel;

    public NpcStatsHud(Stage stage, Skin skin) {
        statsTexture = new Texture(Assets.STATISTICS);
        fearTexture = new Texture(Assets.FEAR_ICON);
        trustTexture = new Texture(Assets.TRUST_ICON);
        moodTexture = new Texture(Assets.MOOD_ICON);

        background = new Image(new NinePatchDrawable(new NinePatch(statsTexture, 32, 32, 32, 32)));
        fearImage = new Image(fearTexture);
        trustImage = new Image(trustTexture);
        moodImage = new Image(moodTexture);

        fearLabel = createLabel(skin);
        trustLabel = createLabel(skin);
        moodLabel = createLabel(skin);

        stage.addActor(background);
        stage.addActor(fearImage);
        stage.addActor(trustImage);
        stage.addActor(moodImage);
        stage.addActor(fearLabel);
        stage.addActor(trustLabel);
        stage.addActor(moodLabel);
        background.toBack();
    }

    private Label createLabel(Skin skin) {
        Label label = new Label("", skin);
        label.setColor(UiStyles.ink());
        label.setAlignment(Align.left);
        return label;
    }

    public void setVisible(boolean visible) {
        background.setVisible(visible);
        fearImage.setVisible(visible);
        trustImage.setVisible(visible);
        moodImage.setVisible(visible);
        fearLabel.setVisible(visible);
        trustLabel.setVisible(visible);
        moodLabel.setVisible(visible);
    }

    public void update(NpcState state) {
        if (state == null) return;

        float trust = state.trust;
        float fear = state.fear;

        trustLabel.setText("Довіра: " + Math.round(trust * 100f) + "%");
        fearLabel.setText("Страх: " + Math.round(fear * 100f) + "%");
        moodLabel.setText("Стан: " + resolveMood(trust, fear));
        layoutPanel();
    }

    private String resolveMood(float trust, float fear) {
        if (fear > 0.75f) {
            return "Наляканий";
        }
        if (trust > 0.75f && fear < 0.4f) {
            return "Довірливий";
        }
        if (trust < 0.3f && fear < 0.4f) {
            return "Закритий";
        }
        return "Напружений";
    }

    public void layout(
        float characterX,
        float characterY,
        float characterWidth,
        float characterHeight,
        float screenWidth,
        UiLayoutProfile profile
    ) {
        float iconSize = characterHeight * 0.10f;
        float lineGap = iconSize + profile.scale(8f);
        updateFontScale(iconSize);

        float panelX = characterX + characterWidth + profile.scale(20f);
        float panelY = characterY + characterHeight * 0.6f;

        if (panelX + iconSize + profile.scale(80f) > screenWidth) {
            panelX = characterX - iconSize - profile.scale(90f);
        }

        setRow(trustImage, trustLabel, panelX, panelY + lineGap, iconSize, profile);
        setRow(fearImage, fearLabel, panelX, panelY, iconSize, profile);
        setRow(moodImage, moodLabel, panelX, panelY - lineGap, iconSize, profile);
        layoutPanel();
    }

    private void setRow(Image icon, Label label, float x, float y, float iconSize, UiLayoutProfile profile) {
        icon.setBounds(x, y, iconSize, iconSize);
        label.setPosition(
            icon.getX() + iconSize + profile.scale(6f),
            icon.getY() + iconSize * 0.4f
        );
    }

    private void updateFontScale(float iconSize) {
        UiLayoutProfile profile = UiLayout.current();
        float scale = MathUtils.clamp((iconSize / 80f) * profile.getFontScaleMultiplier(), 0.6f, 1.8f);

        trustLabel.setFontScale(scale);
        fearLabel.setFontScale(scale);
        moodLabel.setFontScale(scale);

        trustLabel.invalidateHierarchy();
        fearLabel.invalidateHierarchy();
        moodLabel.invalidateHierarchy();
    }

    private void layoutPanel() {
        float padX = 25f;
        float padY = 25f;

        float minX = Math.min(trustImage.getX(), Math.min(fearImage.getX(), moodImage.getX())) - padX;
        float maxRight = Math.max(
            trustLabel.getX() + trustLabel.getPrefWidth(),
            Math.max(fearLabel.getX() + fearLabel.getPrefWidth(), moodLabel.getX() + moodLabel.getPrefWidth())
        ) + padX;
        float minY = moodImage.getY() - padY;
        float maxTop = trustImage.getY() + trustImage.getHeight() + padY;

        background.setBounds(minX, minY, maxRight - minX, maxTop - minY);
        background.toBack();
    }

    public void dispose() {
        statsTexture.dispose();
        fearTexture.dispose();
        trustTexture.dispose();
        moodTexture.dispose();
    }
}
