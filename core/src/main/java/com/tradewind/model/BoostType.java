package com.tradewind.model;

/** Multipliers a player can buy. */
public enum BoostType {

    MONEY("money"),
    XP("xp");

    private final String id;

    BoostType(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public static BoostType byId(String id) {
        for (BoostType type : values()) {
            if (type.id.equalsIgnoreCase(id)) {
                return type;
            }
        }
        return null;
    }
}

