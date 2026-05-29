package com.gdx.game.model;

import java.util.List;

public class DossierData {
    public static class HiddenFactData {
        public String text;
        public List<String> requiredEvidenceAny;
        public List<List<String>> requiredEvidenceAllGroups;
    }

    public String name;
    public int age;
    public String role;
    public String personality;
    public Integer lieRisk;

    public List<String> publicFacts;
    public List<HiddenFactData> hiddenFacts;

    public HiddenFactData getHiddenFact(int index) {
        if (hiddenFacts == null || index < 0 || index >= hiddenFacts.size()) {
            return null;
        }
        return hiddenFacts.get(index);
    }

    public String getHiddenFactText(int index) {
        HiddenFactData fact = getHiddenFact(index);
        return fact != null && fact.text != null ? fact.text : "";
    }
}
