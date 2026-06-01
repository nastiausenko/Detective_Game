package com.gdx.game.widgets.popup;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.gdx.game.app.model.GameContext;
import com.gdx.game.app.navigation.GameFlow;

public class PopupFactory {
    private final Stage stage;
    private final GameContext game;
    private final GameFlow flow;


    public PopupFactory(Stage stage, GameContext game, GameFlow flow) {
        this.stage = stage;
        this.game = game;
        this.flow = flow;
        new Skin(Gdx.files.internal("ui/uiskin.json"));
    }

    public NotePopup createNotePopup() {
        return new NotePopup(stage, game);
    }

    public SettingsPopup createSettingsPopup() {
        return new SettingsPopup(stage, game, flow);
    }

    public StoryPopup createStoryPopup() {
        return new StoryPopup(stage, game);
    }

    public DossierPopup createDossierPopup() {
        return new DossierPopup(stage, game);
    }

    public AccusationPopup createAccusationPopup() {
        return new AccusationPopup(stage, game, flow);
    }

    public ChatHistoryPopup createChatHistoryPopup(String npcId) {
        return new ChatHistoryPopup(stage, game, npcId);
    }

    public EpiloguePopup createEpiloguePopup() {
        return new EpiloguePopup(stage, game, flow);
    }

    public TimeOverPopup createTimeOverPopup() {
        return new TimeOverPopup(stage, game, flow);
    }

    public TheEndPopup createTheEndPopup() {
        return new TheEndPopup(stage, game, flow);
    }
}
