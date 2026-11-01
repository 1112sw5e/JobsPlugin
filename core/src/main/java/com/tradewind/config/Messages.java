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
    private boolean fallbackNotified;

    public Messages(JavaPlugin plugin) {
        this.logger = plugin.getLogger();
        this.file = new YamlFile(plugin, "messages.yml", VERSION);
    }

    public void reload() {
        fallbackNotified = false;
        file.reload();
        String rawPrefix = file.yaml().getString("prefix", "");
        if (rawPrefix == null) {
            rawPrefix = file.bundledDefaults().getString("prefix", "");
        }
        prefix = rawPrefix == null || rawPrefix.isBlank() ? Component.empty() : Text.render(rawPrefix);
    }

    public int version() {
        return file.version();
    }

    /** Raw configured string, used when one message is a placeholder of another. */
    public String raw(String key) {
        String value = lookup(key);
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
        String value = lookup(key);
        if (value == null) {
            reportMissing(key);
            value = "<red>[Tradewind: нет сообщения " + key + "]</red>";
        }
        return Text.render(value, placeholders);
    }

    public List<Component> componentList(String key) {
        List<String> lines = lookupList(key);
        List<Component> rendered = new ArrayList<>(lines.size());
        for (String line : lines) {
            rendered.add(Text.render(line));
        }
        return rendered;
    }

    public List<Component> componentList(String key, Map<String, String> placeholders) {
        List<String> lines = lookupList(key);
        List<Component> rendered = new ArrayList<>(lines.size());
        for (String line : lines) {
            rendered.add(Text.render(line, placeholders));
        }
        return rendered;
    }

    private String lookup(String key) {
        String value = file.yaml().getString(key);
        if (value != null) {
            return value;
        }
        String fallback = file.bundledDefaults().getString(key);
        if (fallback != null) {
            notifyFallback();
        }
        return fallback;
    }

    private List<String> lookupList(String key) {
        if (file.yaml().contains(key)) {
            return file.yaml().getStringList(key);
        }
        List<String> fallback = file.bundledDefaults().getStringList(key);
        if (!fallback.isEmpty()) {
            notifyFallback();
        }
        return fallback;
    }

    /** One line per reload: the file is left untouched, missing keys come from the jar. */
    private void notifyFallback() {
        if (!fallbackNotified) {
            fallbackNotified = true;
            logger.info("messages.yml: часть ключей отсутствует в вашем файле, "
                    + "используются значения из jar (файл не изменяется).");
        }
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

