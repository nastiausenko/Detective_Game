package com.gdx.game.ui.component.popup;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.gdx.game.DetectiveGame;

public class PopupFactory {
    private final Stage stage;
    private final DetectiveGame game;


    public PopupFactory(Stage stage, DetectiveGame game) {
        this.stage = stage;
        this.game = game;
        new Skin(Gdx.files.internal("ui/uiskin.json"));
    }

    public NotePopup createNotePopup() {
        return new NotePopup(stage, game);
    }

    public SettingsPopup createSettingsPopup() {
        return new SettingsPopup(stage, game);
    }

    public StoryPopup createStoryPopup() {
        return new StoryPopup(stage, game);
    }

    public DossierPopup createDossierPopup() {
        return new DossierPopup(stage, game);
    }

    public AccusationPopup createAccusationPopup() {
        return new AccusationPopup(stage, game);
    }

    public ChatHistoryPopup createChatHistoryPopup(String npcId) {
        return new ChatHistoryPopup(stage, game, npcId);
    }

    public EpiloguePopup createEpiloguePopup() {
        return new EpiloguePopup(stage, game);
    }

    public TimeOverPopup createTimeOverPopup() {
        return new TimeOverPopup(stage, game);
    }

    public TheEndPopup createTheEndPopup() {
        return new TheEndPopup(stage, game);
    }
}
