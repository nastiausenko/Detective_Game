package com.gdx.game.ui.chars;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.Json;
import com.gdx.game.DetectiveGame;
import com.gdx.game.data.CharacterData;

import java.util.ArrayList;
import java.util.List;

public class CharacterLoader {
    public static List<CharacterIcon> loadMarkers(DetectiveGame game, String jsonPath) {
        Json json = new Json();
        CharacterData[] characters = json.fromJson(CharacterData[].class, Gdx.files.internal(jsonPath));

        List<CharacterIcon> markers = new ArrayList<>();
        for (CharacterData c : characters) {
            markers.add(new CharacterIcon(game, c.id, c.icon, c.fullBody, c.buildingId));
        }
        return markers;
    }
}
