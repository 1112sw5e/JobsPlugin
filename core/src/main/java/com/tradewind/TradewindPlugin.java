package com.tradewind;

import com.tradewind.bootstrap.PluginContext;
import com.tradewind.bootstrap.PluginStartupException;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Tradewind — профессии, статистика и скупщик ресурсов для Paper.
 *
 * <p>Deliberately thin: all wiring lives in {@link PluginContext} so that startup failures
 * can be reported in one place and commands always go through a fully built context.
 */
public final class TradewindPlugin extends JavaPlugin {

    private PluginContext context;

    @Override
    public void onEnable() {
        try {
            this.context = PluginContext.start(this);
        } catch (PluginStartupException failure) {
            getLogger().severe(failure.getMessage());
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        if (context != null) {
            context.close();
            context = null;
        }
    }

    public PluginContext context() {
        return context;
    }
}

