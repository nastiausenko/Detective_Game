package com.gdx.game.ui.style;

public class UiLayoutProfile {
    public enum DeviceClass {
        DESKTOP,
        TABLET,
        PHONE
    }

    private final DeviceClass deviceClass;
    private final boolean touchDevice;
    private final boolean portrait;
    private final float spacingMultiplier;
    private final float fontScaleMultiplier;
    private final float menuButtonHeightRatio;
    private final float menuSpacingRatio;
    private final float menuStartYRatio;
    private final float titleWidthRatio;
    private final float titleTopInsetRatio;
    private final float overlayButtonHeightRatio;
    private final float popupMaxWidthRatio;
    private final float popupMaxHeightRatio;
    private final float popupButtonHeightRatio;
    private final float questionAreaWidthRatio;
    private final float questionAreaHeightRatio;
    private final float questionAreaBottomMarginRatio;
    private final float characterWidthRatio;
    private final float characterHeightRatio;
    private final float characterBottomRatio;
    private final float bubbleWidthPortraitRatio;
    private final float bubbleWidthLandscapeRatio;
    private final float bubbleFontScale;
    private final float badgeSizeRatio;

    public UiLayoutProfile(
        DeviceClass deviceClass,
        boolean touchDevice,
        boolean portrait,
        float spacingMultiplier,
        float fontScaleMultiplier,
        float menuButtonHeightRatio,
        float menuSpacingRatio,
        float menuStartYRatio,
        float titleWidthRatio,
        float titleTopInsetRatio,
        float overlayButtonHeightRatio,
        float popupMaxWidthRatio,
        float popupMaxHeightRatio,
        float popupButtonHeightRatio,
        float questionAreaWidthRatio,
        float questionAreaHeightRatio,
        float questionAreaBottomMarginRatio,
        float characterWidthRatio,
        float characterHeightRatio,
        float characterBottomRatio,
        float bubbleWidthPortraitRatio,
        float bubbleWidthLandscapeRatio,
        float bubbleFontScale,
        float badgeSizeRatio
    ) {
        this.deviceClass = deviceClass;
        this.touchDevice = touchDevice;
        this.portrait = portrait;
        this.spacingMultiplier = spacingMultiplier;
        this.fontScaleMultiplier = fontScaleMultiplier;
        this.menuButtonHeightRatio = menuButtonHeightRatio;
        this.menuSpacingRatio = menuSpacingRatio;
        this.menuStartYRatio = menuStartYRatio;
        this.titleWidthRatio = titleWidthRatio;
        this.titleTopInsetRatio = titleTopInsetRatio;
        this.overlayButtonHeightRatio = overlayButtonHeightRatio;
        this.popupMaxWidthRatio = popupMaxWidthRatio;
        this.popupMaxHeightRatio = popupMaxHeightRatio;
        this.popupButtonHeightRatio = popupButtonHeightRatio;
        this.questionAreaWidthRatio = questionAreaWidthRatio;
        this.questionAreaHeightRatio = questionAreaHeightRatio;
        this.questionAreaBottomMarginRatio = questionAreaBottomMarginRatio;
        this.characterWidthRatio = characterWidthRatio;
        this.characterHeightRatio = characterHeightRatio;
        this.characterBottomRatio = characterBottomRatio;
        this.bubbleWidthPortraitRatio = bubbleWidthPortraitRatio;
        this.bubbleWidthLandscapeRatio = bubbleWidthLandscapeRatio;
        this.bubbleFontScale = bubbleFontScale;
        this.badgeSizeRatio = badgeSizeRatio;
    }

    public DeviceClass getDeviceClass() {
        return deviceClass;
    }

    public boolean isTouchDevice() {
        return touchDevice;
    }

    public boolean isPortrait() {
        return portrait;
    }

    public float getSpacingMultiplier() {
        return spacingMultiplier;
    }

    public float getFontScaleMultiplier() {
        return fontScaleMultiplier;
    }

    public float getMenuButtonHeightRatio() {
        return menuButtonHeightRatio;
    }

    public float getMenuSpacingRatio() {
        return menuSpacingRatio;
    }

    public float getMenuStartYRatio() {
        return menuStartYRatio;
    }

    public float getTitleWidthRatio() {
        return titleWidthRatio;
    }

    public float getTitleTopInsetRatio() {
        return titleTopInsetRatio;
    }

    public float getOverlayButtonHeightRatio() {
        return overlayButtonHeightRatio;
    }

    public float getPopupMaxWidthRatio() {
        return popupMaxWidthRatio;
    }

    public float getPopupMaxHeightRatio() {
        return popupMaxHeightRatio;
    }

    public float getPopupButtonHeightRatio() {
        return popupButtonHeightRatio;
    }

    public float getQuestionAreaWidthRatio() {
        return questionAreaWidthRatio;
    }

    public float getQuestionAreaHeightRatio() {
        return questionAreaHeightRatio;
    }

    public float getQuestionAreaBottomMarginRatio() {
        return questionAreaBottomMarginRatio;
    }

    public float getCharacterWidthRatio() {
        return characterWidthRatio;
    }

    public float getCharacterHeightRatio() {
        return characterHeightRatio;
    }

    public float getCharacterBottomRatio() {
        return characterBottomRatio;
    }

    public float getBubbleWidthPortraitRatio() {
        return bubbleWidthPortraitRatio;
    }

    public float getBubbleWidthLandscapeRatio() {
        return bubbleWidthLandscapeRatio;
    }

    public float getBubbleFontScale() {
        return bubbleFontScale;
    }

    public float getBadgeSizeRatio() {
        return badgeSizeRatio;
    }

    public float scale(float value) {
        return value * spacingMultiplier;
    }
}
