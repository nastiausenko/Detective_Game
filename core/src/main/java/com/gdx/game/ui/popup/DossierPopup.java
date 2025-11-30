package com.gdx.game.ui.popup;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Array;
import com.gdx.game.DetectiveGame;
import com.gdx.game.data.DossierData;
import com.gdx.game.data.DossierDatabase;
import com.gdx.game.utils.Assets;
import com.gdx.game.utils.FontScaler;
import com.gdx.game.utils.ScreenUtilsHelper;

public class DossierPopup extends AbstractPopup {

    private final Texture[] pages;
    private final Image pageImage;

    private final Image btnNext;
    private final Image btnPrev;
    private final Image closeBtn;

    private final Skin skin;

    private final Label nameLabel;
    private final Label roleLabel;

    private final Label rightLabel;

    private DossierDatabase database;
    private Array<String> characterKeys;

    private int currentPage = 0;

    public DossierPopup(Stage stage, DetectiveGame game) {
        super(stage);
        skin = new Skin(Gdx.files.internal("ui/uiskin.json"));

        pages = new Texture[]{
            new Texture(Assets.DOSSIER_1),
            new Texture(Assets.DOSSIER_2),
            new Texture(Assets.DOSSIER_3)
        };

        pageImage = new Image(pages[0]);

        pageImage.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) { event.stop(); }
        });

        btnPrev = game.getButtonFactory().createButton(Assets.ARROW_LEFT, 64, 64, this::prevPage);
        btnNext = game.getButtonFactory().createButton(Assets.ARROW_RIGHT, 64, 64, this::nextPage);
        closeBtn = game.getButtonFactory().createButton(Assets.CLOSE_BUTTON, 64, 64, this::remove);

        Label.LabelStyle nameStyle = new Label.LabelStyle();
        nameStyle.font = skin.getFont("default-font");
        nameStyle.fontColor = new Color(0.1f, 0.08f, 0.06f, 1f);

        Label.LabelStyle roleStyle = new Label.LabelStyle();
        roleStyle.font = skin.getFont("default-font");
        roleStyle.fontColor = new Color(0.1f, 0.08f, 0.06f, 1f);

        Label.LabelStyle infoStyle = new Label.LabelStyle();
        infoStyle.font = new BitmapFont(Gdx.files.internal("fonts/8bold.fnt"));
        infoStyle.fontColor = new Color(0.1f, 0.08f, 0.06f, 1f);
        infoStyle.font.getData().setScale(0.6f);

        nameLabel = new Label("", nameStyle);
        roleLabel = new Label("", roleStyle);
        rightLabel = new Label("", infoStyle);

        nameLabel.setWrap(true);
        roleLabel.setWrap(true);
        rightLabel.setWrap(true);

        resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
    }

    public void loadDatabase(DossierDatabase db) {
        this.database = db;
        this.characterKeys = new Array<>(db.characters.keys().toArray());
        currentPage = 0;
        updateContent();
    }

    private void nextPage() {
        if (currentPage < characterKeys.size - 1) {
            currentPage++;
            updateContent();
        }
    }

    private void prevPage() {
        if (currentPage > 0) {
            currentPage--;
            updateContent();
        }
    }

    private void updateContent() {
        if (database == null) return;

        String key = characterKeys.get(currentPage);
        DossierData data = database.characters.get(key);

        pageImage.setDrawable(new TextureRegionDrawable(new TextureRegion(pages[currentPage % pages.length])));

        nameLabel.setText(data.name);
        roleLabel.setText(data.role);

        StringBuilder sb = new StringBuilder();

        sb.append("Age: ").append(data.age).append("\n");
        sb.append("Character: ").append(data.personality).append("\n");
        sb.append("Lie risk: ").append(data.lieRisk).append("/5\n\n");

        sb.append("Facts:\n");
        for (String f : data.publicFacts) sb.append(" * ").append(f).append("\n");
        for (String f : data.hiddenFacts) sb.append(" - ").append(f).append("\n");

        rightLabel.setText(sb.toString());
    }

    @Override
    public void show() {
        super.show();
        stage.addActor(pageImage);
        stage.addActor(nameLabel);
        stage.addActor(roleLabel);
        stage.addActor(rightLabel);
        stage.addActor(btnPrev);
        stage.addActor(btnNext);
        stage.addActor(closeBtn);

        resize(stage.getViewport().getWorldWidth(), stage.getViewport().getWorldHeight());
        updateContent();
    }

    public void resize(float screenWidth, float screenHeight) {
        background.setSize(screenWidth, screenHeight);
        resizeCentered(pageImage, pages[0], screenWidth, screenHeight);

        float w = pageImage.getWidth();
        float h = pageImage.getHeight();

        FontScaler.applyScale(skin.getFont("default-font"));

        nameLabel.setWidth(w * 0.37f);
        roleLabel.setWidth(w * 0.37f);

        nameLabel.setPosition(
            pageImage.getX() + w * 0.1f,
            pageImage.getY() + h * 0.2f
        );

        roleLabel.setPosition(
            pageImage.getX() + w * 0.1f,
            pageImage.getY() + h * 0.14f
        );

        rightLabel.setWidth(w * 0.36f);
        rightLabel.setPosition(
            pageImage.getX() + w * 0.55f,
            pageImage.getY() + h * 0.5f
        );

        float btnSize = screenHeight * 0.12f;

        ScreenUtilsHelper.scaleAndPositionButton(btnPrev, btnSize,
            pageImage.getX() - btnPrev.getWidth() * 0.4f,
            pageImage.getY() + h / 2 - btnPrev.getHeight() / 2
        );

        ScreenUtilsHelper.scaleAndPositionButton(btnNext, btnSize,
            pageImage.getX() + w - btnNext.getWidth() * 0.6f,
            pageImage.getY() + h / 2 - btnNext.getHeight() / 2
        );

        ScreenUtilsHelper.scaleAndPositionButton(closeBtn, btnSize,
            10, screenHeight - closeBtn.getHeight() - 10
        );
    }

    @Override
    public void remove() {
        super.remove();
        pageImage.remove();
        nameLabel.remove();
        roleLabel.remove();
        rightLabel.remove();
        btnPrev.remove();
        btnNext.remove();
        closeBtn.remove();
    }

    public void dispose() {
        for (Texture t : pages) t.dispose();
    }
}
