package com.gdx.game.data;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;

public class GameData {
    private static final String[] PREFS_NAMES = {
        "notes_data",
        "game_data",
        "game_timer",
        "ai_detective_history"
    };

    public static void clearAll() {
        for (String name : PREFS_NAMES) {
            Preferences prefs = Gdx.app.getPreferences(name);
            prefs.clear();
            prefs.flush();
        }
    }
}
