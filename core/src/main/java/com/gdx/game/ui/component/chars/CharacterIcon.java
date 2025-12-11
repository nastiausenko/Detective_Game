package com.gdx.game.ui.component.chars;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Cursor;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.gdx.game.DetectiveGame;
import com.gdx.game.domain.world.BuildingData;
import com.gdx.game.ui.overlay.FadeTransition;
import com.gdx.game.ui.screens.CharacterInteriorScreen;

public class CharacterIcon extends Image {
    private final String id;
    private String buildingId;
    private BuildingData linkedBuilding;
    private final float baseSize = 40;

    public CharacterIcon(DetectiveGame game, String id, String iconPath, String fullBodyPath, String buildingId) {
        super(new Texture(iconPath));
        this.id = id;
        this.buildingId = buildingId;

        setSize(baseSize, baseSize * 1.3f);

        getColor().a = 0.7f;

        addListener(new InputListener() {
            @Override
            public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                getColor().a = 1f;
                Gdx.graphics.setSystemCursor(Cursor.SystemCursor.Hand);
            }

            @Override
            public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
                getColor().a = 0.7f;
                Gdx.graphics.setSystemCursor(Cursor.SystemCursor.Arrow);
            }

            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                if (linkedBuilding != null) {
                    FadeTransition transition = game.getTransition();
                    if (!transition.isTransitioning()) {
                        transition.startFadeOut(0.7f, () -> {
                            CharacterInteriorScreen interiorScreen = new CharacterInteriorScreen(
                                game,
                                linkedBuilding.interiorBackground,
                                id,
                                fullBodyPath
                            );
                            game.setScreen(interiorScreen);
                            transition.startFadeIn(0.7f);
                        });
                    }
                }
                return true;
            }
        });
    }

    public String getBuildingId() {
        return buildingId;
    }

    public void setBuilding(BuildingData building) {
        this.linkedBuilding = building;
        this.buildingId = (building != null) ? building.id : null;
    }

    public void updatePositionFromBuilding(float mapWidth, float mapHeight, float scale) {
        if (linkedBuilding != null) {
            float iconWidth = baseSize * scale;
            float iconHeight = iconWidth * 1.3f;
            setSize(iconWidth, iconHeight);

            float bx = linkedBuilding.x * mapWidth;
            float by = linkedBuilding.y * mapHeight;
            float bw = linkedBuilding.width * mapWidth;
            float bh = linkedBuilding.height * mapHeight;

            float iconX = bx + bw / 2f - getWidth() / 2f;
            float iconY = by + bh - getHeight() * 0.5f;

            setPosition(iconX, iconY);
        }
    }

    public String getId() {
        return id;
    }
}
