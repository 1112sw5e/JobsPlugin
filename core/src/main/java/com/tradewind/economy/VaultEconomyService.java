package com.tradewind.economy;

import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.RegisteredServiceProvider;

/**
 * Vault-backed economy.
 *
 * <p>This class is the only place that links against Vault classes, and it is loaded only after
 * {@code Dependency.VAULT} was found, so a server without Vault never sees
 * {@code NoClassDefFoundError}. {@link #create(Logger)} still guards with {@link LinkageError}.
 */
public final class VaultEconomyService implements EconomyService {

    private final Economy economy;

    private VaultEconomyService(Economy economy) {
        this.economy = economy;
    }

    public static EconomyService create(Logger logger) {
        try {
            RegisteredServiceProvider<Economy> registration =
                    Bukkit.getServicesManager().getRegistration(Economy.class);
            if (registration == null || registration.getProvider() == null) {
                logger.warning("Vault найден, но ни один плагин экономики не зарегистрировал провайдера. "
                        + "Установите, например, EssentialsX Economy.");
                return UnavailableEconomy.INSTANCE;
            }
            return new VaultEconomyService(registration.getProvider());
        } catch (LinkageError | RuntimeException failure) {
            logger.log(Level.SEVERE, "Не удалось подключиться к Vault; экономика отключена.", failure);
            return UnavailableEconomy.INSTANCE;
        }
    }

    @Override
    public boolean available() {
        return true;
    }

    @Override
    public String backend() {
        try {
            return economy.getName();
        } catch (RuntimeException failure) {
            return "Vault";
        }
    }

    @Override
    public double balance(OfflinePlayer player) {
        try {
            return economy.getBalance(player);
        } catch (RuntimeException failure) {
            return 0.0D;
        }
    }

    @Override
    public boolean withdraw(OfflinePlayer player, double amount) {
        if (amount <= 0.0D) {
            return false;
        }
        try {
            if (!economy.hasAccount(player) || economy.getBalance(player) < amount) {
                return false;
            }
            EconomyResponse response = economy.withdrawPlayer(player, amount);
            return response.transactionSuccess();
        } catch (RuntimeException failure) {
            return false;
        }
    }

    @Override
    public boolean deposit(OfflinePlayer player, double amount) {
        if (amount <= 0.0D) {
            return false;
        }
        try {
            EconomyResponse response = economy.depositPlayer(player, amount);
            return response.transactionSuccess();
        } catch (RuntimeException failure) {
            return false;
        }
    }

    @Override
    public String format(double amount) {
        try {
            return economy.format(amount);
        } catch (RuntimeException failure) {
            return String.format(Locale.ROOT, "%.2f", amount);
        }
    }
}

