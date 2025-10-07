package com.gdx.game.ui.chars;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.gdx.game.data.BuildingData;
import com.gdx.game.data.CharacterData;

import java.util.Map;

public class CharacterIcon extends Image {
    private final String id;
    private final String name;
    private String buildingId;
    private BuildingData linkedBuilding;
    private final float baseSize = 50;

    public CharacterIcon(String id, String name, String iconPath, String buildingId) {
        super(new Texture(iconPath));
        this.id = id;
        this.name = name;
        this.buildingId = buildingId;

        setSize(baseSize, baseSize * 1.4f);
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
            float iconHeight = iconWidth * 1.4f;
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
