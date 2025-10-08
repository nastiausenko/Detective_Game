package com.gdx.game.utils;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

public class FadeTransition {
    private final ShapeRenderer shapeRenderer = new ShapeRenderer();
    private final OrthographicCamera camera = new OrthographicCamera();

    private float alpha = 0f;
    private float duration = 1f;
    private float time = 0f;

    private boolean fadingOut = false;
    private boolean fadingIn = false;

    private Runnable onFinish;

    public void startFadeOut(float duration, Runnable onFinish) {
        this.duration = duration;
        this.time = 0f;
        this.alpha = 0f;
        this.fadingOut = true;
        this.fadingIn = false;
        this.onFinish = onFinish;
    }

    public void startFadeIn(float duration) {
        this.duration = duration;
        this.time = 0f;
        this.alpha = 1f;
        this.fadingIn = true;
        this.fadingOut = false;
        this.onFinish = null;
    }

    public void update(float delta) {
        if (fadingOut) {
            time += delta;
            alpha = Math.min(1f, time / duration);
            if (alpha >= 1f) {
                fadingOut = false;
                if (onFinish != null) {
                    onFinish.run();
                }
            }
        } else if (fadingIn) {
            time += delta;
            alpha = Math.max(0f, 1f - (time / duration));
            if (alpha <= 0f) {
                fadingIn = false;
            }
        }
    }

    public void render() {
        if (alpha <= 0f) return;

        camera.setToOrtho(false, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.update();
        shapeRenderer.setProjectionMatrix(camera.combined);

        Gdx.gl.glEnable(GL20.GL_BLEND);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0, 0, 0, alpha);
        shapeRenderer.rect(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        shapeRenderer.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    public boolean isTransitioning() {
        return fadingOut || fadingIn;
    }

    public void dispose() {
        shapeRenderer.dispose();
    }
}
