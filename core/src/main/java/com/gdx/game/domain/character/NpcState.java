package com.gdx.game.domain.character;

public class NpcState {
    public String id;
    public float trust;
    public float fear;
    public int questionsAsked;
    public float lastQuestionTime;
    public boolean[] hiddenRevealed;

    public NpcState(int hiddenCount) {
        this.trust = 0.5f;
        this.fear = 0.2f;
        this.questionsAsked = 0;
        this.hiddenRevealed = new boolean[hiddenCount];
    }
}
