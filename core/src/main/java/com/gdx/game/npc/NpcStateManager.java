package com.gdx.game.npc;

import com.badlogic.gdx.utils.ObjectMap;

public class NpcStateManager {

    private final ObjectMap<String, NpcState> states = new ObjectMap<>();

    public NpcState getOrCreate(String npcId, int hiddenCount) {
        NpcState state = states.get(npcId);
        if (state == null || state.hiddenRevealed == null
            || state.hiddenRevealed.length != hiddenCount) {
            state = new NpcState(hiddenCount);
            state.id = npcId;
            states.put(npcId, state);
        }
        return state;
    }

    public void revealHidden(String npcId, int index, int hiddenCount) {
        NpcState state = getOrCreate(npcId, hiddenCount);
        if (index >= 0 && index < state.hiddenRevealed.length) {
            state.hiddenRevealed[index] = true;
        }
    }

    public NpcState get(String npcId) {
        return states.get(npcId);
    }
}
