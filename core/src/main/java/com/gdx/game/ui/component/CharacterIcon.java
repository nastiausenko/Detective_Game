package com.gdx.game.ui.component;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Cursor;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.gdx.game.domain.world.BuildingData;
import com.gdx.game.infrastructure.GameContext;
import com.gdx.game.game.GameFlow;

public class CharacterIcon extends Image {
    private static final float MIN_MOVEMENT_DURATION = 0.75f;
    private static final float MAX_MOVEMENT_DURATION = 1.8f;
    private static final float MOVEMENT_SPEED = 520f;

    private final String id;
    private final GameFlow flow;
    private final boolean hasNpc;
    private final boolean opensInterior;
    private String buildingId;
    private BuildingData linkedBuilding;
    private String fallbackInteriorBackground;
    private final float baseSize = 50;

    private boolean positionInitialized = false;
    private boolean moving = false;
    private float moveElapsed = 0f;
    private float moveDuration = MIN_MOVEMENT_DURATION;
    private float startX;
    private float startY;
    private float targetX;
    private float targetY;

    public CharacterIcon(GameContext game, GameFlow flow, String id, String iconPath, String buildingId, boolean hasNpc) {
        this(game, flow, id, iconPath, buildingId, hasNpc, true);
    }

    public CharacterIcon(
        GameContext game,
        GameFlow flow,
        String id,
        String iconPath,
        String buildingId,
        boolean hasNpc,
        boolean opensInterior
    ) {
        super(new Texture(iconPath));
        this.id = id;
        this.flow = flow;
        this.hasNpc = hasNpc;
        this.buildingId = buildingId;
        this.opensInterior = opensInterior;

        setSize(baseSize , baseSize);

        getColor().a = 0.7f;

        addListener(new InputListener() {
            @Override
            public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                getColor().a = 1f;
                Gdx.graphics.setSystemCursor(
                    CharacterIcon.this.opensInterior ? Cursor.SystemCursor.Hand : Cursor.SystemCursor.Arrow
                );
            }

            @Override
            public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
                getColor().a = 0.7f;
                Gdx.graphics.setSystemCursor(Cursor.SystemCursor.Arrow);
            }

            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                if (!CharacterIcon.this.opensInterior) {
                    return true;
                }

                if (linkedBuilding != null) {
                    if (resolveInteriorBackground() == null) {
                        Gdx.app.error("CharacterIcon", "Missing interior background for npc=" + id
                            + ", building=" + CharacterIcon.this.buildingId);
                        return true;
                    }

                    String interiorNpcId = CharacterIcon.this.hasNpc ? id : null;
                    CharacterIcon.this.flow.enterInterior(interiorNpcId, CharacterIcon.this.buildingId);
                }
                return true;
            }
        });
    }

    public String getBuildingId() {
        return buildingId;
    }

    public void setBuilding(BuildingData building) {
        setLinkedBuilding(building);
        moving = false;
    }

    public void moveToBuilding(BuildingData building, float mapWidth, float mapHeight, float scale) {
        setLinkedBuilding(building);
        if (linkedBuilding == null) return;

        updateIconSize(scale);
        targetX = calculateIconX(linkedBuilding, mapWidth);
        targetY = calculateIconY(linkedBuilding, mapHeight);

        if (!positionInitialized) {
            setPosition(targetX, targetY);
            positionInitialized = true;
            moving = false;
            return;
        }

        startX = getX();
        startY = getY();

        float dx = targetX - startX;
        float dy = targetY - startY;
        float distance = (float)Math.sqrt(dx * dx + dy * dy);

        if (distance < 1f) {
            setPosition(targetX, targetY);
            moving = false;
            return;
        }

        moveElapsed = 0f;
        moveDuration = MathUtils.clamp(
            distance / MOVEMENT_SPEED,
            MIN_MOVEMENT_DURATION,
            MAX_MOVEMENT_DURATION
        );
        moving = true;
    }

    @Override
    public void act(float delta) {
        super.act(delta);

        if (!moving) return;

        moveElapsed += delta;
        float progress = MathUtils.clamp(moveElapsed / moveDuration, 0f, 1f);
        float smoothProgress = Interpolation.smooth.apply(progress);

        setPosition(
            MathUtils.lerp(startX, targetX, smoothProgress),
            MathUtils.lerp(startY, targetY, smoothProgress)
        );

        if (progress >= 1f) {
            setPosition(targetX, targetY);
            moving = false;
        }
    }

    private void setLinkedBuilding(BuildingData building) {
        this.linkedBuilding = building;
        this.buildingId = (building != null) ? building.id : null;
        if (building != null && hasInteriorBackground(building.interiorBackground)) {
            fallbackInteriorBackground = building.interiorBackground;
        }
    }

    public void updatePositionFromBuilding(float mapWidth, float mapHeight, float scale) {
        if (linkedBuilding != null) {
            updateIconSize(scale);

            targetX = calculateIconX(linkedBuilding, mapWidth);
            targetY = calculateIconY(linkedBuilding, mapHeight);

            if (!positionInitialized || !moving) {
                setPosition(targetX, targetY);
                positionInitialized = true;
                moving = false;
            }
        }
    }

    public String getId() {
        return id;
    }

    private String resolveInteriorBackground() {
        if (linkedBuilding != null && hasInteriorBackground(linkedBuilding.interiorBackground)) {
            return linkedBuilding.interiorBackground;
        }
        return fallbackInteriorBackground;
    }

    private boolean hasInteriorBackground(String backgroundPath) {
        return backgroundPath != null && !backgroundPath.isEmpty();
    }

    private void updateIconSize(float scale) {
        float iconWidth = baseSize * scale;
        float iconHeight = iconWidth * 1.3f;
        setSize(iconWidth, iconHeight);
    }

    private float calculateIconX(BuildingData building, float mapWidth) {
        float bx = building.x * mapWidth;
        float bw = building.width * mapWidth;
        return bx + bw / 2f - getWidth() / 2f;
    }

    private float calculateIconY(BuildingData building, float mapHeight) {
        float by = building.y * mapHeight;
        float bh = building.height * mapHeight;
        return by + bh - getHeight() * 0.5f;
    }

}
