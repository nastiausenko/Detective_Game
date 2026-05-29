package com.gdx.game.domain.world;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.ObjectSet;
import com.gdx.game.ai.NpcDialogueService;

public class CrimeSceneService {
    private static final String PREFS_NAME = "crime_scene_state";
    private static final String UNLOCKED_KEY = "unlocked";
    private static final String PENDING_KEY = "pending";

    private final LoreDatabase loreDb;
    private final NpcDialogueService npcDialogueService;
    private final Preferences prefs;
    private final ObjectMap<String, Array<LoreDatabase.CrimeSceneHint>> hintsByLocation = new ObjectMap<>();
    private final ObjectSet<String> unlockedHintIds = new ObjectSet<>();
    private final ObjectSet<String> pendingHintIds = new ObjectSet<>();

    public CrimeSceneService(LoreDatabase loreDb, NpcDialogueService npcDialogueService) {
        this.loreDb = loreDb;
        this.npcDialogueService = npcDialogueService;
        this.prefs = Gdx.app.getPreferences(PREFS_NAME);

        indexHints();
        loadState();
        syncUnlockedHints();
    }

    public Array<LoreDatabase.CrimeSceneHint> getUnlockedHintsForLocation(String locationId) {
        Array<LoreDatabase.CrimeSceneHint> result = new Array<>();
        Array<LoreDatabase.CrimeSceneHint> source = hintsByLocation.get(locationId);
        if (source == null) {
            return result;
        }

        for (LoreDatabase.CrimeSceneHint hint : source) {
            if (hint != null && unlockedHintIds.contains(hint.id)) {
                result.add(hint);
            }
        }
        return result;
    }

    public Array<LoreDatabase.CrimeSceneHint> getHintsForLocation(String locationId) {
        Array<LoreDatabase.CrimeSceneHint> source = hintsByLocation.get(locationId);
        if (source == null) {
            return new Array<>();
        }
        return new Array<>(source);
    }

    public boolean hasPendingHints(String locationId) {
        Array<LoreDatabase.CrimeSceneHint> source = hintsByLocation.get(locationId);
        if (source == null) {
            return false;
        }

        for (LoreDatabase.CrimeSceneHint hint : source) {
            if (hint != null && pendingHintIds.contains(hint.id)) {
                return true;
            }
        }
        return false;
    }

    public int syncUnlockedHints() {
        Array<LoreDatabase.CrimeSceneHint> allHints = getAllHints();
        if (allHints.size == 0) {
            return 0;
        }

        int newlyUnlocked = 0;
        for (LoreDatabase.CrimeSceneHint hint : allHints) {
            if (hint == null || hint.id == null || hint.id.isEmpty()) {
                continue;
            }
            if (unlockedHintIds.contains(hint.id)) {
                continue;
            }
            if (!isUnlockSatisfied(hint)) {
                continue;
            }

            unlockedHintIds.add(hint.id);
            pendingHintIds.add(hint.id);
            newlyUnlocked++;
        }

        if (newlyUnlocked > 0) {
            saveState();
        }

        return newlyUnlocked;
    }

    public void clearPendingForLocation(String locationId) {
        Array<LoreDatabase.CrimeSceneHint> source = hintsByLocation.get(locationId);
        if (source == null || source.size == 0) {
            return;
        }

        boolean changed = false;
        for (LoreDatabase.CrimeSceneHint hint : source) {
            if (hint != null && pendingHintIds.remove(hint.id)) {
                changed = true;
            }
        }

        if (changed) {
            saveState();
        }
    }

    public void reset() {
        unlockedHintIds.clear();
        pendingHintIds.clear();
        prefs.clear();
        prefs.flush();
    }

    private void indexHints() {
        Array<LoreDatabase.CrimeSceneHint> allHints = getAllHints();
        for (LoreDatabase.CrimeSceneHint hint : allHints) {
            if (hint == null || hint.id == null || hint.id.isEmpty()) {
                continue;
            }
            if (hint.locationId == null || hint.locationId.isEmpty()) {
                continue;
            }

            Array<LoreDatabase.CrimeSceneHint> locationHints = hintsByLocation.get(hint.locationId);
            if (locationHints == null) {
                locationHints = new Array<>();
                hintsByLocation.put(hint.locationId, locationHints);
            }
            locationHints.add(hint);
        }
    }

    private Array<LoreDatabase.CrimeSceneHint> getAllHints() {
        if (loreDb == null || loreDb.murder == null || loreDb.murder.crimeSceneHints == null) {
            return new Array<>();
        }
        return loreDb.murder.crimeSceneHints;
    }

    private boolean isUnlockSatisfied(LoreDatabase.CrimeSceneHint hint) {
        boolean anySatisfied = hint.unlockFactsAny == null || hint.unlockFactsAny.isEmpty();
        if (hint.unlockFactsAny != null && !hint.unlockFactsAny.isEmpty()) {
            anySatisfied = false;
            for (String factKey : hint.unlockFactsAny) {
                if (isNpcFactRevealed(factKey)) {
                    anySatisfied = true;
                    break;
                }
            }
        }

        boolean allSatisfied = true;
        if (hint.unlockFactsAll != null && !hint.unlockFactsAll.isEmpty()) {
            for (String factKey : hint.unlockFactsAll) {
                if (!isNpcFactRevealed(factKey)) {
                    allSatisfied = false;
                    break;
                }
            }
        }

        return anySatisfied && allSatisfied;
    }

    private boolean isNpcFactRevealed(String factKey) {
        if (factKey == null || factKey.isEmpty()) {
            return false;
        }

        String[] parts = factKey.split(":");
        if (parts.length != 2) {
            return false;
        }

        String npcId = parts[0].trim();
        if (npcId.isEmpty()) {
            return false;
        }

        int factIndex;
        try {
            factIndex = Integer.parseInt(parts[1].trim());
        } catch (NumberFormatException ex) {
            return false;
        }

        return npcDialogueService != null && npcDialogueService.isFactRevealed(npcId, factIndex);
    }

    private void loadState() {
        readSet(prefs.getString(UNLOCKED_KEY, ""), unlockedHintIds);
        readSet(prefs.getString(PENDING_KEY, ""), pendingHintIds);
    }

    private void saveState() {
        prefs.putString(UNLOCKED_KEY, joinSet(unlockedHintIds));
        prefs.putString(PENDING_KEY, joinSet(pendingHintIds));
        prefs.flush();
    }

    private void readSet(String raw, ObjectSet<String> target) {
        target.clear();
        if (raw == null || raw.isEmpty()) {
            return;
        }

        String[] parts = raw.split(",");
        for (String part : parts) {
            String trimmed = part != null ? part.trim() : "";
            if (!trimmed.isEmpty()) {
                target.add(trimmed);
            }
        }
    }

    private String joinSet(ObjectSet<String> source) {
        StringBuilder sb = new StringBuilder();
        for (String value : source) {
            if (value == null || value.isEmpty()) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append(",");
            }
            sb.append(value);
        }
        return sb.toString();
    }
}
