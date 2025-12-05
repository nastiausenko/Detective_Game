package com.gdx.game.data;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;

public class DialogueHistory {

    private static final String PREFS_NAME   = "ai_detective_history";
    private static final String KEY_PREFIX   = "chat_history_";
    public  static final String SEP          = "|||";

    private static String keyForNpc(String npcId) {
        return KEY_PREFIX + npcId;
    }

    private static String sanitize(String s) {
        if (s == null) return "";
        return s.replace("\n", " ").replace(SEP, " ");
    }

    public static void append(String npcId, String question, String answer) {
        Preferences prefs = Gdx.app.getPreferences(PREFS_NAME);
        String key = keyForNpc(npcId);

        String existing = prefs.getString(key, "");
        String line = sanitize(question) + SEP + sanitize(answer);

        String updated = existing.isEmpty()
                ? line
                : existing + "\n" + line;

        prefs.putString(key, updated);
        prefs.flush();
    }

    public static String loadRaw(String npcId) {
        Preferences prefs = Gdx.app.getPreferences(PREFS_NAME);
        return prefs.getString(keyForNpc(npcId), "");
    }
}
