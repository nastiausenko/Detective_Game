package com.gdx.game.service.world;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.ObjectMap;
import com.gdx.game.domain.character.CharacterData;
import com.gdx.game.domain.world.BuildingData;

public class WorldLookupService {
    private final ObjectMap<String, CharacterData> charactersById = new ObjectMap<>();
    private final ObjectMap<String, BuildingData> buildingsById = new ObjectMap<>();

    public WorldLookupService() {
        loadCharacters("characters.json");
        loadBuildings("buildings.json");
    }

    public CharacterData getCharacter(String npcId) {
        if (npcId == null || npcId.isEmpty()) return null;
        return charactersById.get(npcId);
    }

    public BuildingData getBuilding(String buildingId) {
        if (buildingId == null || buildingId.isEmpty()) return null;
        return buildingsById.get(buildingId);
    }

    public String getCharacterFullBodyPath(String npcId) {
        CharacterData character = getCharacter(npcId);
        return character != null ? character.fullBody : null;
    }

    public String getInteriorBackgroundPath(String buildingId) {
        BuildingData building = getBuilding(buildingId);
        return building != null ? building.interiorBackground : null;
    }

    public boolean hasInteriorBackground(String buildingId) {
        String backgroundPath = getInteriorBackgroundPath(buildingId);
        return backgroundPath != null && !backgroundPath.isEmpty();
    }

    private void loadCharacters(String path) {
        Json json = new Json();
        CharacterData[] characters = json.fromJson(CharacterData[].class, Gdx.files.internal(path));
        if (characters == null) return;

        for (CharacterData character : characters) {
            if (character != null && character.id != null && !character.id.isEmpty()) {
                charactersById.put(character.id, character);
            }
        }
    }

    private void loadBuildings(String path) {
        Json json = new Json();
        BuildingData[] buildings = json.fromJson(BuildingData[].class, Gdx.files.internal(path));
        if (buildings == null) return;

        for (BuildingData building : buildings) {
            if (building != null && building.id != null && !building.id.isEmpty()) {
                buildingsById.put(building.id, building);
            }
        }
    }
}
