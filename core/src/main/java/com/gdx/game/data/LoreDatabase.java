// LoreDatabase.java
package com.gdx.game.data;

import com.badlogic.gdx.utils.Array;

public class LoreDatabase {
    public int version;

    public Setting setting;
    public Murder murder;
    public KillerView killerView;
    public Array<Relationship> relationships;
    public static class Setting {
        public String townName;
        public String tone;
        public String summary;
        public Array<String> publicHistory;
        public Array<String> hiddenHistory;
    }

    public static class Murder {
        public String victimId;
        public String killerId;
        public String timeApprox;
        public String place;
        public String weapon;
        public Array<String> crimeScenePublic;
        public Array<String> crimeSceneHidden;
        public String officialStory;
        public String realStory;
    }

    public static class KillerView {
        public String killerId;
        public String perceivedMotiveWorld;
        public Array<String> rationalMotive;
        public Array<String> trueMotive;
    }

    public static class Relationship {
        public String from;
        public String to;
        public String type;
        public String publicView;
        public String hiddenView;
    }
}
