package com.gdx.game.service.save;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;

public class GameData {
    private static final String[] PREFS_NAMES = {
        "notes_data",
        "game_data",
        "game_timer",
        "dialogue_history",
        "npc_state",
        "npc_locations",
        "crime_scene_state"
    };

    public static void clearAll() {
        for (String name : PREFS_NAMES) {
            Preferences prefs = Gdx.app.getPreferences(name);
            prefs.clear();
            prefs.flush();
        }
    }
}
