package com.gdx.game.features.worldmap.model;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.Json;
import com.gdx.game.model.CharacterData;
import com.gdx.game.app.model.GameContext;
import com.gdx.game.app.navigation.GameFlow;
import com.gdx.game.features.worldmap.ui.CharacterIcon;

import java.util.ArrayList;
import java.util.List;

public class CharacterLoader {
    public static List<CharacterIcon> loadMarkers(GameContext game, GameFlow flow, String jsonPath) {
        Json json = new Json();
        CharacterData[] characters = json.fromJson(CharacterData[].class, Gdx.files.internal(jsonPath));

        List<CharacterIcon> markers = new ArrayList<>();
        for (CharacterData c : characters) {
            markers.add(new CharacterIcon(game, flow, c.id, c.icon, c.fullBody, c.buildingId));
        }
        return markers;
    }
}
