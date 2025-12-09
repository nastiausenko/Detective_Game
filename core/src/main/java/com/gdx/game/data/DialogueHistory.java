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

    public static String loadRecentForLlm(String npcId,
                                          int maxPairs,
                                          int maxChars) {

        String raw = loadRaw(npcId);
        if (raw == null || raw.isEmpty()) return "";

        String[] lines = raw.split("\n");
        if (lines.length == 0) return "";

        int start = Math.max(0, lines.length - maxPairs);
        StringBuilder sb = new StringBuilder();

        for (int i = start; i < lines.length; i++) {
            String line = lines[i];
            if (line == null || line.isEmpty()) continue;

            String[] parts = line.split(SEP, 2);
            String q = parts.length > 0 ? parts[0].trim() : "";
            String a = parts.length > 1 ? parts[1].trim() : "";

            if (!q.isEmpty()) {
                sb.append("Детектив: ").append(q).append("\n");
            }
            if (!a.isEmpty()) {
                sb.append("NPC: ").append(a).append("\n");
            }
            sb.append("\n");
        }

        String result = sb.toString().trim();
        if (result.length() <= maxChars) return result;

        return result.substring(result.length() - maxChars);
    }
}
