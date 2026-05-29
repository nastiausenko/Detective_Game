package com.gdx.game.widgets.popup;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Array;
import com.gdx.game.app.DetectiveGame;
import com.gdx.game.model.DossierData;
import com.gdx.game.model.DossierDatabase;
import com.gdx.game.model.NpcState;
import com.gdx.game.shared.config.Assets;
import com.gdx.game.shared.config.UiLayout;
import com.gdx.game.shared.config.UiLayoutProfile;
import com.gdx.game.shared.lib.ScreenUtilsHelper;

import java.util.Arrays;

public class DossierPopup extends AbstractPopup {

    private final Texture[] pages;
    private final Image pageImage;

    private final Image btnNext;
    private final Image btnPrev;
    private final Image closeBtn;

    private final DetectiveGame game;

    private final Label nameLabel;
    private final Label roleLabel;
    private final Label rightLabel;

    private final Table textTable;

    private DossierDatabase database;
    private Array<String> characterKeys;

    private final Label.LabelStyle styleLeft;
    private final Label.LabelStyle styleRight;

    private int currentPage = 0;

    public DossierPopup(Stage stage, DetectiveGame game) {
        super(stage);
        this.game = game;

        BitmapFont leftFont = new BitmapFont(Gdx.files.internal("fonts/8bold.fnt"));
        BitmapFont rightFont = new BitmapFont(Gdx.files.internal("fonts/8bold.fnt"));

        pages = new Texture[]{
            new Texture(Assets.DOSSIER_VICTIM),
            new Texture(Assets.DOSSIER_CASHIER),
            new Texture(Assets.DOSSIER_OFFICER),
            new Texture(Assets.DOSSIER_SISTER),
            new Texture(Assets.DOSSIER_STUDENT),
            new Texture(Assets.DOSSIER_DOCTOR),
        };

        pageImage = new Image(pages[0]);
        pageImage.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                event.stop();
            }
        });

        btnPrev = game.getButtonFactory().createButton(Assets.ARROW_LEFT, 64, 64, this::prevPage);
        btnNext = game.getButtonFactory().createButton(Assets.ARROW_RIGHT, 64, 64, this::nextPage);
        closeBtn = game.getButtonFactory().createButton(Assets.CLOSE_BUTTON, 64, 64, this::remove);

        styleLeft = new Label.LabelStyle();
        styleLeft.font = leftFont;
        styleLeft.fontColor = new Color(0.1f, 0.08f, 0.06f, 1f);

        styleRight = new Label.LabelStyle();
        styleRight.font = rightFont;
        styleRight.fontColor = new Color(0.1f, 0.08f, 0.06f, 1f);

        nameLabel = new Label("", styleLeft);
        roleLabel = new Label("", styleRight);
        rightLabel = new Label("", styleRight);

        nameLabel.setWrap(true);
        roleLabel.setWrap(true);
        rightLabel.setWrap(true);

        textTable = new Table();
        textTable.top().left();

        resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
    }

    public void loadDatabase(DossierDatabase db) {
        this.database = db;
        this.characterKeys = new Array<>(db.characters.orderedKeys());
        currentPage = 0;
        updateContent();
    }

    private void nextPage() {
        if (database == null) return;
        if (currentPage < characterKeys.size - 1) {
            currentPage++;
            updateContent();
        }
    }

    private void prevPage() {
        if (database == null) return;
        if (currentPage > 0) {
            currentPage--;
            updateContent();
        }
    }

    private void updateContent() {
        if (database == null || characterKeys == null || characterKeys.size == 0) return;

        String npcId = characterKeys.get(currentPage);
        DossierData data = database.characters.get(npcId);

        pageImage.setDrawable(new TextureRegionDrawable(
            new TextureRegion(pages[currentPage % pages.length]))
        );

        nameLabel.setText(data.name != null ? data.name : "");
        roleLabel.setText(data.role != null ? data.role : "");

        StringBuilder sb = new StringBuilder();

        sb.append("Вік: ").append(data.age).append("\n");
        sb.append("Характер: ").append(data.personality).append("\n");
        if (data.lieRisk != null) {
            sb.append("Брехливість: ").append(data.lieRisk).append("/5\n\n");
        } else {
            sb.append("\n");
        }

        sb.append("Факти:\n");

        if (data.publicFacts != null) {
            for (String f : data.publicFacts) {
                sb.append(" - ").append(f).append("\n");
            }
        }

        NpcState state = game.getNpcDialogueService().getStateForUi(npcId);

        if (data.hiddenFacts != null) {
            boolean[] revealed = (state != null) ? state.hiddenRevealed : null;

            if (revealed != null) {
                Gdx.app.log("DOSSIER_DEBUG", npcId + " hidden=" + Arrays.toString(revealed));
            }

            for (int i = 0; i < data.hiddenFacts.size(); i++) {
                String fact = data.getHiddenFactText(i);

                boolean isRevealed =
                    revealed != null &&
                        i < revealed.length &&
                        revealed[i];

                if (isRevealed) {
                    sb.append(" - ").append(fact).append("\n");
                } else {
                    sb.append(" - ???").append("\n");
                }
            }
        }

        rightLabel.setText(sb.toString());
    }

    @Override
    public void show() {
        super.show();
        addPopupActors(pageImage, textTable, btnPrev, btnNext, closeBtn);

        resize(stage.getViewport().getWorldWidth(), stage.getViewport().getWorldHeight());
        updateContent();
    }

    public void resize(float screenWidth, float screenHeight) {
        UiLayoutProfile profile = UiLayout.current(screenWidth, screenHeight);
        resizeCentered(pageImage, pages[0], screenWidth, screenHeight);

        float w = pageImage.getWidth();
        float h = pageImage.getHeight();

        styleLeft.font.getData().setScale(h/600f);
        styleRight.font.getData().setScale(h/800f);

        float marginX = w * 0.1f;
        float marginY = h * 0.08f;

        float contentW = w - marginX * 2f;
        float contentH = h - marginY * 2f;

        textTable.clear();
        textTable.setBounds(
            pageImage.getX() + marginX,
            pageImage.getY() + marginY,
            contentW,
            contentH
        );
        textTable.top().left();

        float leftColW  = contentW * 0.45f;
        float rightColW = contentW * 0.45f;

        Table leftCol = new Table();
        leftCol.top().left();
        leftCol.add(nameLabel)
            .left()
            .width(leftColW);
        leftCol.row();
        leftCol.add(roleLabel)
            .left()
            .width(leftColW);

        Table rightCol = new Table();
        rightCol.top().left();
        rightCol.add(rightLabel)
            .left()
            .top()
            .width(rightColW)
            .padTop(h*0.03f);

        textTable.add(leftCol)
            .left()
            .expandX()
            .fillY()
            .width(leftColW)
            .padTop(h*0.65f);
        textTable.add(rightCol)
            .top()
            .left()
            .width(rightColW)
            .padLeft(contentW * 0.04f)
            .expandY()
            .fillY();
        textTable.row();

        float targetHeight = screenHeight * profile.getPopupButtonHeightRatio();
        float margin = profile.scale(10f);

        ScreenUtilsHelper.scaleButton(btnPrev, targetHeight, stage);
        ScreenUtilsHelper.scaleButton(btnNext, targetHeight, stage);
        ScreenUtilsHelper.scaleButton(closeBtn, targetHeight, stage);

        btnPrev.setPosition(
            pageImage.getX() - btnPrev.getWidth() * 0.4f,
            pageImage.getY() + h / 2f - btnPrev.getHeight() / 2f
        );
        btnNext.setPosition(
            pageImage.getX() + w - btnNext.getWidth() * 0.6f,
            pageImage.getY() + h / 2f - btnNext.getHeight() / 2f
        );
        closeBtn.setPosition(margin, screenHeight - closeBtn.getHeight() - margin);
    }

    @Override
    public void remove() {
        super.remove();
        removePopupActors(pageImage, textTable, btnPrev, btnNext, closeBtn);
    }

    public void dispose() {
        for (Texture t : pages) {
            t.dispose();
        }
        skin.dispose();
    }
}
