package com.tradewind.model;

import java.util.Locale;
import java.util.Optional;

/** The five professions of the plugin. */
public enum Profession {

    MINE("mine", "tw_mine"),
    FARM("farm", "tw_farm"),
    WOOD("wood", "tw_wood"),
    FISH("fish", "tw_fish"),
    SNOW("snow", "tw_snow");

    private final String id;
    private final String defaultRegion;

    Profession(String id, String defaultRegion) {
        this.id = id;
        this.defaultRegion = defaultRegion;
    }

    public String id() {
        return id;
    }

    public String defaultRegion() {
        return defaultRegion;
    }

    public String configKey(String key) {
        return "professions." + id + "." + key;
    }

    public static Optional<Profession> byId(String id) {
        if (id == null) {
            return Optional.empty();
        }
        String normalized = id.toLowerCase(Locale.ROOT);
        for (Profession profession : values()) {
            if (profession.id.equals(normalized)) {
                return Optional.of(profession);
            }
        }
        return Optional.empty();
    }
}

