package com.gdx.game.infrastructure;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;

public final class UiLayout {

    private UiLayout() {
    }

    public static UiLayoutProfile current() {
        return current(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
    }

    public static UiLayoutProfile current(float width, float height) {
        boolean portrait = height >= width;
        boolean touchDevice = isTouchDevice();
        UiLayoutProfile.DeviceClass deviceClass = classifyDevice(width, height, touchDevice);

        if (deviceClass == UiLayoutProfile.DeviceClass.DESKTOP) {
            return new UiLayoutProfile(
                deviceClass, false, portrait,
                1.0f, 1.0f,
                0.10f, 0.03f, 0.30f,
                0.55f, 0.40f,
                0.12f,
                0.90f, 0.90f, 0.12f,
                0.85f, 0.14f, 0.02f,
                0.45f, 0.70f, 0.10f,
                0.85f, 0.80f, 0.70f,
                0.20f
            );
        }

        if (deviceClass == UiLayoutProfile.DeviceClass.TABLET) {
            return new UiLayoutProfile(
                deviceClass, true, portrait,
                1.15f, 1.15f,
                0.12f, 0.026f, 0.28f,
                portrait ? 0.72f : 0.62f,
                portrait ? 0.32f : 0.36f,
                0.13f,
                0.94f, 0.92f, 0.13f,
                portrait ? 0.92f : 0.88f,
                0.16f, 0.025f,
                portrait ? 0.56f : 0.50f,
                portrait ? 0.62f : 0.72f,
                portrait ? 0.08f : 0.07f,
                0.90f, 0.84f, 0.82f,
                0.22f
            );
        }

        return new UiLayoutProfile(
            deviceClass, true, portrait,
            1.35f, 1.30f,
            portrait ? 0.13f : 0.15f,
            0.02f,
            portrait ? 0.20f : 0.24f,
            portrait ? 0.82f : 0.72f,
            portrait ? 0.22f : 0.28f,
            0.15f,
            0.98f, 0.95f, 0.15f,
            portrait ? 0.94f : 0.92f,
            portrait ? 0.18f : 0.20f,
            0.03f,
            portrait ? 0.72f : 0.60f,
            portrait ? 0.46f : 0.58f,
            portrait ? 0.22f : 0.10f,
            0.94f, 0.88f, 0.90f,
            0.24f
        );
    }

    private static UiLayoutProfile.DeviceClass classifyDevice(float width, float height, boolean touchDevice) {
        if (!touchDevice) {
            return UiLayoutProfile.DeviceClass.DESKTOP;
        }

        float density = Math.max(1f, Gdx.graphics.getDensity());
        float shortSideDp = Math.min(width, height) / density;
        if (shortSideDp >= 700f) {
            return UiLayoutProfile.DeviceClass.TABLET;
        }
        return UiLayoutProfile.DeviceClass.PHONE;
    }

    private static boolean isTouchDevice() {
        Application.ApplicationType type = Gdx.app.getType();
        return type == Application.ApplicationType.Android
            || type == Application.ApplicationType.iOS;
    }
}
