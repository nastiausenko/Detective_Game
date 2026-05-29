package com.gdx.game.model;

public class InvestigationState {
    public long startTimeMillis;
    public boolean accusationUnlocked;
    public boolean accusationPromptShown;
    public boolean accusationDone;

    public int revealedHiddenFacts;

    public String accusedNpcId;
    public boolean accusationCorrect;
}
