package com.gdx.game.widgets.popup;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.utils.Align;
import com.gdx.game.app.model.GameContext;
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
        "Розенфельд — невелике провінційне місто, де більшість людей знають одне одного. " +
            "Одним із найвідоміших його мешканців був доктор Адріан Вальтер: лікар, викладач і власник приватної лікарні.",

        "У ніч перед твоїм приїздом його знайшли мертвим у кабінеті власного будинку. " +
            "Слідів злому немає, зі столу зникли частина документів, а поруч залишився напис: \"It was not a cure.\"",

        "Місту зручніше вважати це особистою трагедією, ніж починати гучне розслідування. " +
            "Тобі потрібно з’ясувати, що сталося насправді, перш ніж справу закриють. " +
            "Для цього доведеться поговорити з тими, хто знав Вальтера найближче, зіставити їхні слова й вирішити, кому можна довіряти."
    };

    private int currentPage = 0;

    private final GlyphLayout layout;
    private final TypewriterText typewriterText;

    public StoryPopup(Stage stage, GameContext game) {
        super(stage);
        layout = new GlyphLayout();

        storyTexture = new Texture(Assets.PROLOGUE);
        storyImage = new Image(storyTexture);

        storyLabel = new Label("", UiStyles.label(skin, UiStyles.parchmentText()));
        storyLabel.setWrap(true);
        storyLabel.setAlignment(Align.center);
        typewriterText = new TypewriterText(storyLabel);

        continueButton = game.buttonFactory.createButton(
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
