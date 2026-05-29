package com.gdx.game.domain.world;

import com.badlogic.gdx.utils.ObjectMap;

public final class LocationDescriptions {
    private static final ObjectMap<String, String> UKRAINIAN_NAMES = createUkrainianNames();

    private LocationDescriptions() {
    }

    public static String describe(String buildingId) {
        if (buildingId == null || buildingId.isEmpty()) {
            return "невідома локація";
        }

        String description = UKRAINIAN_NAMES.get(buildingId);
        return description != null ? description : buildingId;
    }

    private static ObjectMap<String, String> createUkrainianNames() {
        ObjectMap<String, String> names = new ObjectMap<>();
        names.put("cafe", "кав'ярня Blume");
        names.put("shop", "міський магазин");
        names.put("hospital", "лікарня Розенфельда");
        names.put("med_school", "медична школа");
        names.put("town_hall", "ратуша");
        names.put("doctor_house", "будинок Мари");
        names.put("professor_house", "будинок Вальтера");
        names.put("sister_house", "будинок Клари");
        names.put("cashier_house", "будинок Елени");
        names.put("officer_house", "будинок Ернста");
        names.put("student_house", "будинок Ліама");
        return names;
    }
}
