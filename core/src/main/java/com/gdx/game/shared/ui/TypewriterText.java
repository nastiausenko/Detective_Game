package com.gdx.game.shared.ui;

import com.badlogic.gdx.scenes.scene2d.ui.Label;

public class TypewriterText {
    private static final float CHAR_DELAY = 0.04f;

    private final Label label;
    private String fullText = "";
    private float timer;
    private int charIndex;
    private boolean finished = true;

    public TypewriterText(Label label) {
        this.label = label;
    }

    public void start(String text) {
        fullText = text != null ? text : "";
        timer = 0f;
        charIndex = 0;
        finished = fullText.isEmpty();
        label.setText("");
    }

    public void update(float delta) {
        if (finished) return;

        timer += delta;
        while (timer >= CHAR_DELAY && charIndex < fullText.length()) {
            timer -= CHAR_DELAY;
            charIndex++;
            label.setText(fullText.substring(0, charIndex));
        }

        if (charIndex >= fullText.length()) {
            finished = true;
        }
    }

    public void finish() {
        charIndex = fullText.length();
        finished = true;
        label.setText(fullText);
    }

    public boolean isFinished() {
        return finished;
    }
}
