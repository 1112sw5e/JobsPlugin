package com.tradewind.bootstrap;

/**
 * External plugins Tradewind relies on. These are declared as {@code softdepend} in plugin.yml
 * on purpose: the plugin checks them itself and reports a readable message instead of letting
 * the server skip loading Tradewind (or throwing {@code NoClassDefFoundError}).
 */
public enum Dependency {

    VAULT("Vault", true, "экономика, баланс, сдача предметов и зарплата"),
    PLACEHOLDER_API("PlaceholderAPI", true, "плейсхолдеры в текстах и scoreboard"),
    WORLD_GUARD("WorldGuard", true, "регионы профессий"),
    TAB("TAB", false, "scoreboard-профили игроков");

    private final String pluginName;
    private final boolean required;
    private final String purpose;

    Dependency(String pluginName, boolean required, String purpose) {
        this.pluginName = pluginName;
        this.required = required;
        this.purpose = purpose;
    }

    public String pluginName() {
        return pluginName;
    }

    public boolean required() {
        return required;
    }

    public String purpose() {
        return purpose;
    }
}

