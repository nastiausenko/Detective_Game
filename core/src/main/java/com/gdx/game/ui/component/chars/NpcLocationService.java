package com.gdx.game.ui.component.chars;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.ObjectSet;
import com.gdx.game.domain.character.CharacterData;
import com.gdx.game.domain.world.BuildingData;

public class NpcLocationService {
    private static final String PREFS_NAME = "npc_locations";
    private static final String LAST_SLOT_KEY = "last_movement_slot";
    private static final String BUILDING_KEY_PREFIX = "npc.";
    private static final int MOVE_INTERVAL_GAME_MINUTES = 10;
    private static final int MOVE_CHANCE_PERCENT = 45;

    private final Preferences prefs;
    private final Array<String> npcIds = new Array<>();
    private final Array<String> buildingIds = new Array<>();
    private final ObjectMap<String, String> defaultLocations = new ObjectMap<>();
    private final ObjectMap<String, String> currentLocations = new ObjectMap<>();

    private boolean hasSavedMovementSlot = false;
    private int lastProcessedMovementSlot = -1;

    public NpcLocationService() {
        prefs = Gdx.app.getPreferences(PREFS_NAME);
        loadCharacterDefaults();
        loadBuildings();
        loadSavedState();
    }

    public String getCurrentBuildingId(String npcId) {
        String savedBuildingId = currentLocations.get(npcId);
        if (savedBuildingId != null) {
            return savedBuildingId;
        }
        return defaultLocations.get(npcId);
    }

    public void updateForGameMinutes(int elapsedGameMinutes) {
        if (elapsedGameMinutes < 0 || npcIds.size == 0 || buildingIds.size < 2) {
            return;
        }

        int targetSlot = elapsedGameMinutes / MOVE_INTERVAL_GAME_MINUTES;
        if (!hasSavedMovementSlot) {
            hasSavedMovementSlot = true;
            lastProcessedMovementSlot = targetSlot;
            saveState();
            return;
        }

        if (targetSlot <= lastProcessedMovementSlot) {
            return;
        }

        for (int slot = lastProcessedMovementSlot + 1; slot <= targetSlot; slot++) {
            processMovementSlot(slot);
        }

        lastProcessedMovementSlot = targetSlot;
        saveState();
    }

    public void reset() {
        currentLocations.clear();
        for (String npcId : npcIds) {
            currentLocations.put(npcId, defaultLocations.get(npcId));
        }
        hasSavedMovementSlot = false;
        lastProcessedMovementSlot = -1;
        prefs.clear();
        prefs.flush();
    }

    private void loadCharacterDefaults() {
        Json json = new Json();
        CharacterData[] characters = json.fromJson(CharacterData[].class, Gdx.files.internal("characters.json"));
        if (characters == null) {
            return;
        }

        for (CharacterData character : characters) {
            if (character == null || character.id == null || character.buildingId == null) {
                continue;
            }
            npcIds.add(character.id);
            defaultLocations.put(character.id, character.buildingId);
            currentLocations.put(character.id, character.buildingId);
        }
    }

    private void loadBuildings() {
        Json json = new Json();
        BuildingData[] buildings = json.fromJson(BuildingData[].class, Gdx.files.internal("buildings.json"));
        if (buildings == null) {
            return;
        }

        for (BuildingData building : buildings) {
            if (building == null || building.id == null) {
                continue;
            }
            if (building.interiorBackground == null || building.interiorBackground.isEmpty()) {
                continue;
            }
            buildingIds.add(building.id);
        }
    }

    private void loadSavedState() {
        hasSavedMovementSlot = prefs.contains(LAST_SLOT_KEY);
        if (hasSavedMovementSlot) {
            lastProcessedMovementSlot = prefs.getInteger(LAST_SLOT_KEY, 0);
        }

        for (String npcId : npcIds) {
            String savedBuildingId = prefs.getString(BUILDING_KEY_PREFIX + npcId, "");
            if (savedBuildingId.isEmpty() || !isKnownBuilding(savedBuildingId)) {
                continue;
            }
            currentLocations.put(npcId, savedBuildingId);
        }
    }

    private void processMovementSlot(int slot) {
        ObjectSet<String> occupiedBuildings = new ObjectSet<>();
        for (String npcId : npcIds) {
            String buildingId = getCurrentBuildingId(npcId);
            if (buildingId != null) {
                occupiedBuildings.add(buildingId);
            }
        }

        for (String npcId : npcIds) {
            if (!shouldMoveThisSlot(npcId, slot)) {
                continue;
            }

            String currentBuildingId = getCurrentBuildingId(npcId);
            if (currentBuildingId != null) {
                occupiedBuildings.remove(currentBuildingId);
            }

            Array<String> candidates = collectAvailableBuildings(currentBuildingId, occupiedBuildings);
            if (candidates.size == 0) {
                if (currentBuildingId != null) {
                    occupiedBuildings.add(currentBuildingId);
                }
                continue;
            }

            String nextBuildingId = candidates.get(selectCandidateIndex(npcId, slot, candidates.size));
            currentLocations.put(npcId, nextBuildingId);
            occupiedBuildings.add(nextBuildingId);
        }
    }

    private Array<String> collectAvailableBuildings(String currentBuildingId, ObjectSet<String> occupiedBuildings) {
        Array<String> candidates = new Array<>();
        for (String buildingId : buildingIds) {
            if (buildingId.equals(currentBuildingId)) {
                continue;
            }
            if (occupiedBuildings.contains(buildingId)) {
                continue;
            }
            candidates.add(buildingId);
        }
        return candidates;
    }

    private boolean shouldMoveThisSlot(String npcId, int slot) {
        return Math.floorMod(stableHash(npcId, slot, 17), 100) < MOVE_CHANCE_PERCENT;
    }

    private int selectCandidateIndex(String npcId, int slot, int candidatesCount) {
        return Math.floorMod(stableHash(npcId, slot, 53), candidatesCount);
    }

    private int stableHash(String npcId, int slot, int salt) {
        int hash = 17;
        hash = 31 * hash + npcId.hashCode();
        hash = 31 * hash + slot;
        hash = 31 * hash + salt;
        return hash;
    }

    private boolean isKnownBuilding(String buildingId) {
        for (String knownBuildingId : buildingIds) {
            if (knownBuildingId.equals(buildingId)) {
                return true;
            }
        }
        return false;
    }

    private void saveState() {
        prefs.clear();
        if (hasSavedMovementSlot) {
            prefs.putInteger(LAST_SLOT_KEY, lastProcessedMovementSlot);
        }
        for (String npcId : npcIds) {
            String buildingId = getCurrentBuildingId(npcId);
            if (buildingId != null) {
                prefs.putString(BUILDING_KEY_PREFIX + npcId, buildingId);
            }
        }
        prefs.flush();
    }

}
