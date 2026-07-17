package com.gdx.game.service.world;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.Json;
import com.gdx.game.domain.character.CharacterData;
import com.gdx.game.infrastructure.GameContext;
import com.gdx.game.game.GameFlow;
import com.gdx.game.ui.component.CharacterIcon;

import java.util.ArrayList;
import java.util.List;

public class CharacterLoader {
    public static List<CharacterIcon> loadMarkers(GameContext game, GameFlow flow, String jsonPath) {
        Json json = new Json();
        CharacterData[] characters = json.fromJson(CharacterData[].class, Gdx.files.internal(jsonPath));

        List<CharacterIcon> markers = new ArrayList<>();
        for (CharacterData c : characters) {
            markers.add(new CharacterIcon(game, flow, c.id, c.icon, c.buildingId, true));
        }
        return markers;
    }
}
