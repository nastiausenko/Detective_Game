package com.gdx.game.ui.component.popup;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.utils.Align;
import com.gdx.game.DetectiveGame;
import com.gdx.game.infrastructure.Assets;

public class StoryPopup extends AbstractPopup {
    private final Image storyImage;
    private final Texture storyTexture;
    private final Label storyLabel;
    private final Image continueButton;

    private final String[] pages = new String[] {
        "Розенфельд завжди здавався спокійним провінційним містечком: тихі вулички, охайні фасади, люди, які вітаються по імені. " +
            "У центрі цієї ідилії стояла лікарня доктора Адріана Вальтера - місце, де рятували життя і будували легенди. " +
            "Для міста він був \"совістю Розенфельда\": лікар, що не відмовляв у допомозі, викладач, який виховував нове " +
            "покоління медиків, і людина, якій довіряли навіть свої найтемніші страхи.",

        "Три ночі тому Вальтера знайшли мертвим у його кабінеті у старому крилі лікарні. " +
            "Він сидів у кріслі, наче просто заснув посеред роботи. Зброї поруч не було. " +
            "На стіні за його спиною кров'ю було виведено: \"Він був тут\". " +
            "Офіційно все намагаються назвати нещасним випадком або виснаженням. " +
            "Але лікарі говорять пошепки, свідки плутаються в деталях, а місто занадто поспішно хоче забути цю ніч.",

        "Тебе викликали сюди не для того, щоб погодитись з офіційною версією. " +
            "Мер боїться скандалу й дає тобі лише три дні - не більше. " +
            "За цей час ти маєш поговорити з тими, хто був найближчим до Вальтера: його сестрою, колегами, учнем та його знайомими. " +
            "Тут кожне запитання має ціну. І кожна відповідь може наблизити тебе або до правди, або до чужої версії реальності."
    };

    private final StringBuilder sb = new StringBuilder();
    private float charTimer = 0f;
    private final float charDelay = 0.04f;
    private int charIndex = 0;
    private int currentPage = 0;
    private boolean finishedPage = false;

    private final DetectiveGame game;
    private final GlyphLayout layout;

    public StoryPopup(Stage stage, DetectiveGame game) {
        super(stage);
        this.game = game;
        layout = new GlyphLayout();

        storyTexture = new Texture(Assets.PROLOGUE);
        storyImage = new Image(storyTexture);

        Label.LabelStyle labelStyle = new Label.LabelStyle();
        labelStyle.font = skin.getFont("default-font");
        labelStyle.fontColor = new Color(154 / 255f, 109 / 255f, 69 / 255f, 1f);

        storyLabel = new Label("", labelStyle);
        storyLabel.setWrap(true);
        storyLabel.setAlignment(Align.center);

        continueButton = game.getButtonFactory().createButton(
            Assets.CONTINUE_BUTTON, 60, 60,
            () -> {
                if (!finishedPage) {
                    finishCurrentPageText();
                } else if (currentPage < pages.length - 1) {
                    nextPage();
                } else {
                    this.remove();
                }
            }
        );
    }

    private String getCurrentPageText() {
        return pages[currentPage];
    }

    public void update(float delta) {
        if (finishedPage) return;

        charTimer += delta;
        String fullText = getCurrentPageText();

        while (charTimer >= charDelay && charIndex < fullText.length()) {
            sb.append(fullText.charAt(charIndex));
            charIndex++;
            storyLabel.setText(sb.toString());
            charTimer -= charDelay;
        }

        if (charIndex >= fullText.length()) {
            finishedPage = true;
        }
    }

    private void finishCurrentPageText() {
        String fullText = getCurrentPageText();
        sb.setLength(0);
        sb.append(fullText);
        storyLabel.setText(fullText);
        finishedPage = true;
        charIndex = fullText.length();
    }

    private void nextPage() {
        currentPage++;
        if (currentPage >= pages.length) {
            currentPage = pages.length - 1;
            return;
        }

        sb.setLength(0);
        storyLabel.setText("");
        charIndex = 0;
        charTimer = 0f;
        finishedPage = false;

        resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
    }

    private void rescaleFontToFit() {
        if (storyImage.getWidth() == 0 || storyImage.getHeight() == 0) return;

        Label.LabelStyle style = storyLabel.getStyle();
        BitmapFont font = style.font;

        float labelWidth = storyImage.getWidth() * 0.7f;
        float availableHeight = storyImage.getHeight() * 0.35f;

        String fullText = getCurrentPageText();

        font.getData().setScale(1f);

        layout.setText(font, fullText, style.fontColor, labelWidth, Align.center, true);
        float prefHeight = layout.height;
        if (prefHeight <= 0f) prefHeight = font.getCapHeight();

        float scale = availableHeight / prefHeight;
        scale = MathUtils.clamp(scale, 0.6f, 1.4f);

        font.getData().setScale(scale);
        storyLabel.invalidateHierarchy();
    }

    public void resize(float screenWidth, float screenHeight) {
        background.setSize(screenWidth, screenHeight);
        resizeCentered(storyImage, storyTexture, screenWidth, screenHeight);

        float btnWidth = storyImage.getWidth() * 0.5f;
        float btnHeight = storyImage.getHeight() * 0.1f;
        float paddingBottom = storyImage.getHeight() * 0.1f;

        float labelWidth = storyImage.getWidth() * 0.7f;
        storyLabel.setWidth(labelWidth);
        storyLabel.setPosition(
                storyImage.getX() + storyImage.getWidth() * 0.15f,
                storyImage.getY() + storyImage.getHeight() * 0.52f
        );

        rescaleFontToFit();

        continueButton.setSize(btnWidth, btnHeight);
        continueButton.setPosition(
            storyImage.getX() + (storyImage.getWidth() - btnWidth) / 2f,
            storyImage.getY() + paddingBottom
        );
    }

    @Override
    public void show() {
        super.show();

        currentPage = 0;
        sb.setLength(0);
        storyLabel.setText("");
        charIndex = 0;
        charTimer = 0f;
        finishedPage = false;

        stage.addActor(storyImage);
        stage.addActor(storyLabel);
        stage.addActor(continueButton);

        resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
    }

    @Override
    public void remove() {
        super.remove();
        storyImage.remove();
        storyLabel.remove();
        continueButton.remove();
    }
}
