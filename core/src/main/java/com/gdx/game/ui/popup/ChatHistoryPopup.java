package com.gdx.game.ui.popup;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.NinePatch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable;
import com.badlogic.gdx.utils.Align;
import com.gdx.game.DetectiveGame;
import com.gdx.game.data.DialogueHistory;
import com.gdx.game.utils.Assets;
import com.gdx.game.utils.FontScaler;
import com.gdx.game.utils.NoMinNinePatchDrawable;
import com.gdx.game.utils.ScreenUtilsHelper;

import java.util.ArrayList;
import java.util.List;

public class ChatHistoryPopup extends AbstractPopup {
    private final String npcId;

    private final Texture chatHistoryTexture;
    private final Image chatHistoryImage;

    private final Image closeBtn;

    private final Table messagesTable;
    private final ScrollPane scrollPane;

    private final NinePatchDrawable npcBubbleDrawable;
    private final NinePatchDrawable playerBubbleDrawable;

    private final Label.LabelStyle bubbleLabelStyle;
    private final GlyphLayout bubbleLabelLayout = new GlyphLayout();

    private float maxBubbleWidth;

    protected ChatHistoryPopup(Stage stage, Skin skin, DetectiveGame game, String npcId) {
        super(stage);
        this.npcId = npcId;

        chatHistoryTexture = new Texture(Assets.CHAT_HISTORY_POPUP);
        chatHistoryImage = new Image(chatHistoryTexture);
        chatHistoryImage.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                event.stop();
            }
        });

        closeBtn = game.getButtonFactory().createButton(Assets.CLOSE_BUTTON, 64, 64, ChatHistoryPopup.this::remove);

        bubbleLabelStyle = new Label.LabelStyle();
        bubbleLabelStyle.font = skin.getFont("default-font");
        bubbleLabelStyle.fontColor = Color.BLACK;
        bubbleLabelStyle.font.getData().setScale(0.5f);

        Texture bubbleTex = new Texture(Assets.QUESTION_AREA);
        int right = 40;
        int left = 40;
        int top = 0;
        int bottom = 0;

        NinePatch npcPatch = new NinePatch(bubbleTex, left, right, top, bottom);
        NinePatch playerPatch = new NinePatch(bubbleTex, left, right, top, bottom);

        npcBubbleDrawable = new NoMinNinePatchDrawable(npcPatch);
        playerBubbleDrawable = new NoMinNinePatchDrawable(playerPatch);

        messagesTable = new Table();
        messagesTable.top().pad(10f);

        ScrollPane.ScrollPaneStyle spStyle = new ScrollPane.ScrollPaneStyle(
            skin.get(ScrollPane.ScrollPaneStyle.class)
        );

        spStyle.background = null;
        spStyle.vScroll = skin.newDrawable("white", new Color(0.3f, 0.2f, 0.1f, 0.4f));
        spStyle.vScrollKnob = skin.newDrawable("white", new Color(0.8f, 0.7f, 0.5f, 0.9f));

        scrollPane = new ScrollPane(messagesTable, spStyle);
        scrollPane.setScrollingDisabled(true, false);
        scrollPane.setFadeScrollBars(false);
        scrollPane.setOverscroll(false, true);
        scrollPane.setClamp(true);

        scrollPane.addListener(new InputListener() {
            @Override
            public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                stage.setScrollFocus(scrollPane);
            }

            @Override
            public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
                if (stage.getScrollFocus() == scrollPane) {
                    stage.setScrollFocus(null);
                }
            }
        });

        resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
    }

    public void resize(int width, int height) {
        background.setSize(width, height);
        resizeCentered(chatHistoryImage, chatHistoryTexture, width, height);

        float popupX = chatHistoryImage.getX();
        float popupY = chatHistoryImage.getY();
        float popupWidth = chatHistoryImage.getWidth();
        float popupHeight = chatHistoryImage.getHeight();

        float btnSize = height * 0.12f;
        ScreenUtilsHelper.scaleButton(closeBtn, btnSize, stage);
        closeBtn.setPosition(10, height - closeBtn.getHeight() - 10);

        FontScaler.applyScale(bubbleLabelStyle.font);

        float padX = popupWidth * 0.05f;
        float padY = popupHeight * 0.05f;

        float scrollX = popupX + padX;
        float scrollY = popupY + padY;
        float scrollW = popupWidth - padX * 2f;
        float scrollH = popupHeight - padY * 2f;

        scrollPane.setBounds(scrollX, scrollY, scrollW, scrollH);

        maxBubbleWidth = scrollW * 0.75f - 20f;

        rebuildHistory();
    }

    @Override
    public void show() {
        super.show();
        stage.addActor(chatHistoryImage);
        stage.addActor(scrollPane);
        stage.addActor(closeBtn);
        rebuildHistory();
    }

    @Override
    public void remove() {
        super.remove();
        chatHistoryImage.remove();
        scrollPane.remove();
        closeBtn.remove();
    }

    public void dispose() {
        chatHistoryTexture.dispose();
    }

    private List<ChatEntry> loadHistory() {
        String raw = DialogueHistory.loadRaw(npcId);
        List<ChatEntry> result = new ArrayList<>();

        if (raw.isEmpty()) return result;

        String[] lines = raw.split("\n");
        for (String line : lines) {
            if (line.trim().isEmpty()) continue;
            String[] parts = line.split("\\Q" + DialogueHistory.SEP + "\\E");
            String q = parts.length > 0 ? parts[0] : "";
            String a = parts.length > 1 ? parts[1] : "";
            result.add(new ChatEntry(q, a));
        }
        return result;
    }

    private void rebuildHistory() {
        messagesTable.clearChildren();

        List<ChatEntry> history = loadHistory();
        if (history.isEmpty()) {
            Label empty = new Label("Поки що немає історії.", bubbleLabelStyle);
            messagesTable.add(empty).pad(10f);
            return;
        }

        for (ChatEntry entry : history) {
            if (!entry.question.isEmpty()) {
                addPlayerMessage(entry.question);
            }
            if (!entry.answer.isEmpty()) {
                addNpcMessage(entry.answer);
            }
        }

        stage.act();
        scrollPane.layout();
        scrollPane.setScrollY(scrollPane.getMaxY());
    }

    private void addNpcMessage(String text) {
        Table bubble = createBubble(text, npcBubbleDrawable);

        float bw = Math.min(bubble.getPrefWidth(), maxBubbleWidth);

        Table row = new Table();
        row.defaults().pad(3f);

        row.add(bubble)
            .width(bw)
            .left();
        row.add().expandX().fillX();

        messagesTable.add(row)
            .expandX()
            .fillX()
            .padTop(4f);
        messagesTable.row();
    }

    private void addPlayerMessage(String text) {
        Table bubble = createBubble(text, playerBubbleDrawable);

        float bw = Math.min(bubble.getPrefWidth(), maxBubbleWidth);

        Table row = new Table();
        row.defaults().pad(3f);

        row.add().expandX().fillX();
        row.add(bubble)
            .width(bw)
            .right();

        messagesTable.add(row)
            .expandX()
            .fillX()
            .padTop(4f);
        messagesTable.row();
    }

    private Table createBubble(String text, NinePatchDrawable background) {
        Label label = new Label(text, bubbleLabelStyle);
        label.setAlignment(Align.left);
        label.setWrap(true);

        float padX = 30f;
        float padY = 16f;

        float innerMax = Math.max(30f, maxBubbleWidth - padX * 2f);

        bubbleLabelLayout.setText(bubbleLabelStyle.font, text);
        float singleLineWidth = bubbleLabelLayout.width;

        float innerWidth = Math.min(singleLineWidth, innerMax);
        if (innerWidth < 1f) innerWidth = 1f;

        label.setWidth(innerWidth);
        label.invalidateHierarchy();
        label.layout();

        Table bubble = new Table();
        bubble.setBackground(background);

        bubble.add(label)
            .width(innerWidth)
            .pad(padY, padX, padY, padX)
            .left()
            .top();

        bubble.pack();

        return bubble;
    }

    private static class ChatEntry {
        final String question;
        final String answer;

        ChatEntry(String question, String answer) {
            this.question = question;
            this.answer = answer;
        }
    }
}
