package com.tradewind.economy;

import org.bukkit.OfflinePlayer;

/**
 * Currency access. The interface intentionally mentions no Vault type, so it can be loaded
 * and used when Vault is absent.
 */
public interface EconomyService {

    boolean available();

    /** Backend name for diagnostics, e.g. "EssentialsX Economy". */
    String backend();

    double balance(OfflinePlayer player);

    boolean withdraw(OfflinePlayer player, double amount);

    boolean deposit(OfflinePlayer player, double amount);

    String format(double amount);
}

