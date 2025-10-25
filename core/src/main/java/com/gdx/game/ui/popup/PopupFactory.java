package com.gdx.game.ui.popup;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.gdx.game.DetectiveGame;
import com.gdx.game.utils.Assets;
import com.gdx.game.utils.FadeTransition;
import com.gdx.game.utils.NotePages;

public class PopupFactory {
    private final Stage stage;
    private final DetectiveGame game;
    private final FadeTransition transition;
    private final Skin skin;


    public PopupFactory(Stage stage, DetectiveGame game, FadeTransition transition) {
        this.stage = stage;
        this.game = game;
        this.transition = transition;
        this.skin = new Skin(Gdx.files.internal("ui/uiskin.json"));
    }

    public NotePopup createNotePopup() {
        return new NotePopup(stage, skin, game);
    }

    public SettingsPopup createSettingsPopup() {
        return new SettingsPopup(stage, game, transition);
    }

    public StoryPopup createStoryPopup() {
        return new StoryPopup(stage, game);
    }
}
