package com.tradewind.economy;

import java.util.Locale;
import org.bukkit.OfflinePlayer;

/** No-op economy used when Vault or its economy provider is missing. */
public final class UnavailableEconomy implements EconomyService {

    public static final UnavailableEconomy INSTANCE = new UnavailableEconomy();

    private UnavailableEconomy() {
    }

    @Override
    public boolean available() {
        return false;
    }

    @Override
    public String backend() {
        return "недоступна";
    }

    @Override
    public double balance(OfflinePlayer player) {
        return 0.0D;
    }

    @Override
    public boolean withdraw(OfflinePlayer player, double amount) {
        return false;
    }

    @Override
    public boolean deposit(OfflinePlayer player, double amount) {
        return false;
    }

    @Override
    public String format(double amount) {
        return String.format(Locale.ROOT, "%.2f", amount);
    }
}

