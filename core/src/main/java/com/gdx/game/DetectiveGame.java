package com.gdx.game;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.gdx.game.screens.MenuScreen;
import com.gdx.game.ui.GdxResourceProvider;
import com.gdx.game.ui.ResourceProvider;
import com.gdx.game.ui.UIButtonFactory;
import com.gdx.game.utils.FadeTransition;

public class DetectiveGame extends Game {
    public SpriteBatch batch;
    private UIButtonFactory buttonFactory;

    @Override
    public void create() {
        batch = new SpriteBatch();
        FadeTransition transition = new FadeTransition();
        ResourceProvider resourceProvider = new GdxResourceProvider();
        buttonFactory = new UIButtonFactory(resourceProvider);
        this.setScreen(new MenuScreen(this, transition));
    }

    @Override
    public void render() {
        super.render();
    }

    @Override
    public void dispose() {
        batch.dispose();
    }

    public UIButtonFactory getButtonFactory() {
        return buttonFactory;
    }
}
