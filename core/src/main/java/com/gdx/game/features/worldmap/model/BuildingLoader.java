package com.gdx.game.features.worldmap.model;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.Json;
import com.gdx.game.model.BuildingData;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BuildingLoader {
    public static List<BuildingData> loadBuildings(String path) {
        Json json = new Json();
        BuildingData[] buildings = json.fromJson(BuildingData[].class, Gdx.files.internal(path));
        return Arrays.asList(buildings);
    }

    public static Map<String, BuildingData> toMap(List<BuildingData> buildings) {
        Map<String, BuildingData> map = new HashMap<>();
        for (BuildingData b : buildings) map.put(b.id, b);
        return map;
    }
}
