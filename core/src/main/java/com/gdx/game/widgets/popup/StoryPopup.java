package com.gdx.game.widgets.popup;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.utils.Align;
import com.gdx.game.app.DetectiveGame;
import com.gdx.game.shared.config.Assets;
import com.gdx.game.shared.ui.UiStyles;
import com.gdx.game.shared.ui.TypewriterText;
import com.gdx.game.shared.lib.ScreenUtilsHelper;

public class StoryPopup extends AbstractPopup {
    private final Image storyImage;
    private final Texture storyTexture;
    private final Label storyLabel;
    private final Image continueButton;

    private final String[] pages = new String[] {
        "Розенфельд завжди здавався спокійним провінційним містечком: тихі вулички, охайні фасади, люди, які вітаються по імені. " +
            "У центрі цієї ідилії стояла лікарня доктора Адріана Вальтера - місце, де рятували життя і будували легенди. " +
            "Для міста він був \"совістю Розенфельда\": лікар, що не відмовляв у допомозі, викладач, який виховував нове " +
            "покоління медиків, і людина, якій довіряли навіть те, про що не говорять у родині.",

        "У ніч перед твоїм приїздом Вальтера знайшли мертвим у приватному кабінеті його будинку. " +
            "Поліція говорить обережно, родина мовчить, а лікарня поводиться так, ніби втратила не людину, а власну легенду. " +
            "Офіційно все намагаються назвати виснаженням, провиною або самогубством. " +
            "Але місто занадто поспішно приймає версію, яка дозволяє не ставити болючих питань.",

        "Тебе викликали сюди не для того, щоб погодитись з офіційною версією. " +
            "Мер боїться скандалу й дає тобі зовсім мало часу, перш ніж справу назвуть особистою трагедією. " +
            "За цей час ти маєш поговорити з тими, хто був найближчим до Вальтера: його сестрою, колегами, учнем та його знайомими. " +
            "Тут кожне запитання має ціну. І кожна відповідь може наблизити тебе або до правди, або до чужої версії реальності."
    };

    private int currentPage = 0;

    private final GlyphLayout layout;
    private final TypewriterText typewriterText;

    public StoryPopup(Stage stage, DetectiveGame game) {
        super(stage);
        layout = new GlyphLayout();

        storyTexture = new Texture(Assets.PROLOGUE);
        storyImage = new Image(storyTexture);

        storyLabel = new Label("", UiStyles.label(skin, UiStyles.parchmentText()));
        storyLabel.setWrap(true);
        storyLabel.setAlignment(Align.center);
        typewriterText = new TypewriterText(storyLabel);

        continueButton = game.getButtonFactory().createButton(
            Assets.CONTINUE_BUTTON, 60, 60,
            () -> {
                if (!typewriterText.isFinished()) {
                    typewriterText.finish();
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
        typewriterText.update(delta);
    }

    private void nextPage() {
        currentPage++;
        if (currentPage >= pages.length) {
            currentPage = pages.length - 1;
            return;
        }

        typewriterText.start(getCurrentPageText());

        resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
    }

    private void rescaleFontToFit() {
        if (storyImage.getWidth() == 0 || storyImage.getHeight() == 0) return;

        float labelWidth = storyImage.getWidth() * 0.7f;
        float availableHeight = storyImage.getHeight() * 0.35f;
        PopupTextScaler.scaleToFitCentered(storyLabel, layout, getCurrentPageText(), labelWidth, availableHeight);
    }

    public void resize(float screenWidth, float screenHeight) {
        resizeCentered(storyImage, storyTexture, screenWidth, screenHeight);

        ScreenUtilsHelper.scaleNavButton(continueButton, storyImage);

        float labelWidth = storyImage.getWidth() * 0.7f;
        storyLabel.setWidth(labelWidth);
        storyLabel.setPosition(
                storyImage.getX() + storyImage.getWidth() * 0.15f,
                storyImage.getY() + storyImage.getHeight() * 0.52f
        );

        rescaleFontToFit();
    }

    @Override
    public void show() {
        super.show();

        currentPage = 0;
        typewriterText.start(getCurrentPageText());

        addPopupActors(storyImage, storyLabel, continueButton);

        resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
    }

    @Override
    public void remove() {
        super.remove();
        removePopupActors(storyImage, storyLabel, continueButton);
    }
}
