package com.gdx.game;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.gdx.game.screens.MenuScreen;
import com.gdx.game.ui.GdxResourceProvider;
import com.gdx.game.ui.UIButtonFactory;
import com.gdx.game.utils.FadeTransition;
import com.gdx.game.utils.UIOverlayManager;

public class DetectiveGame extends Game {
    public SpriteBatch batch;
    public UIOverlayManager overlay;

    private UIButtonFactory buttonFactory;
    private FadeTransition transition;

    @Override
    public void create() {
        batch = new SpriteBatch();
        transition = new FadeTransition();
        buttonFactory = new UIButtonFactory(new GdxResourceProvider());

        overlay = new UIOverlayManager(this);

        setScreen(new MenuScreen(this, transition));
    }

    @Override
    public void render() {
        super.render();

        transition.update(Gdx.graphics.getDeltaTime());
        transition.render();
    }

    @Override
    public void dispose() {
        if (overlay != null) overlay.dispose();
        if (batch != null) batch.dispose();
    }

    public UIButtonFactory getButtonFactory() {
        return buttonFactory;
    }

    public FadeTransition getTransition() {
        return transition;
    }
}
