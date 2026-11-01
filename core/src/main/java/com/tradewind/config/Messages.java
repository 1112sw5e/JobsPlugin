package com.tradewind.config;

import com.tradewind.text.Text;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

/** {@code messages.yml} access with a prefix and {@code {placeholder}} substitution. */
public final class Messages {

    public static final int VERSION = 1;

    private final Logger logger;
    private final YamlFile file;
    private final Set<String> reportedMissing = ConcurrentHashMap.newKeySet();
    private Component prefix = Component.empty();

    public Messages(JavaPlugin plugin) {
        this.logger = plugin.getLogger();
        this.file = new YamlFile(plugin, "messages.yml", VERSION);
    }

    public void reload() {
        file.reload();
        String rawPrefix = file.yaml().getString("prefix", "");
        prefix = rawPrefix == null || rawPrefix.isBlank() ? Component.empty() : Text.render(rawPrefix);
    }

    public int version() {
        return file.version();
    }

    /** Raw configured string, used when one message is a placeholder of another. */
    public String raw(String key) {
        String value = file.yaml().getString(key);
        if (value == null) {
            reportMissing(key);
            return "";
        }
        return value;
    }

    public Component component(String key) {
        return component(key, Map.of());
    }

    public Component component(String key, Map<String, String> placeholders) {
        String value = file.yaml().getString(key);
        if (value == null) {
            reportMissing(key);
            value = "<red>[Tradewind: нет сообщения " + key + "]</red>";
        }
        return Text.render(value, placeholders);
    }

    public List<Component> componentList(String key) {
        List<String> lines = file.yaml().getStringList(key);
        List<Component> rendered = new ArrayList<>(lines.size());
        for (String line : lines) {
            rendered.add(Text.render(line));
        }
        return rendered;
    }

    public List<Component> componentList(String key, Map<String, String> placeholders) {
        List<String> lines = file.yaml().getStringList(key);
        List<Component> rendered = new ArrayList<>(lines.size());
        for (String line : lines) {
            rendered.add(Text.render(line, placeholders));
        }
        return rendered;
    }

    public void send(CommandSender target, String key) {
        target.sendMessage(prefix.append(component(key)));
    }

    public void send(CommandSender target, String key, String... placeholderPairs) {
        target.sendMessage(prefix.append(component(key, pairs(placeholderPairs))));
    }

    /** Sends an already rendered component with the configured prefix. */
    public void sendPrefixed(CommandSender target, Component component) {
        target.sendMessage(prefix.append(component));
    }

    public static Map<String, String> pairs(String... placeholderPairs) {
        if (placeholderPairs.length % 2 != 0) {
            throw new IllegalArgumentException("Ожидается чётное число аргументов: ключ, значение");
        }
        Map<String, String> map = new LinkedHashMap<>();
        for (int i = 0; i < placeholderPairs.length; i += 2) {
            map.put(placeholderPairs[i], placeholderPairs[i + 1]);
        }
        return map;
    }

    private void reportMissing(String key) {
        if (reportedMissing.add(key)) {
            logger.warning("messages.yml: отсутствует ключ '" + key + "'.");
        }
    }
}

