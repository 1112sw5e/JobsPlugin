package com.tradewind.text;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

/**
 * Renders configured text. MiniMessage tags and legacy {@code &} codes may be mixed
 * in one string; placeholders use the {@code {name}} form and are replaced before parsing.
 */
public final class Text {

    private static final Pattern LEGACY_CODE = Pattern.compile("&#([0-9A-Fa-f]{6})|&([0-9A-Fa-fK-Or])");

    private static final Map<Character, String> LEGACY_TAGS = Map.ofEntries(
            Map.entry('0', "<black>"),
            Map.entry('1', "<dark_blue>"),
            Map.entry('2', "<dark_green>"),
            Map.entry('3', "<dark_aqua>"),
            Map.entry('4', "<dark_red>"),
            Map.entry('5', "<dark_purple>"),
            Map.entry('6', "<gold>"),
            Map.entry('7', "<gray>"),
            Map.entry('8', "<dark_gray>"),
            Map.entry('9', "<blue>"),
            Map.entry('a', "<green>"),
            Map.entry('b', "<aqua>"),
            Map.entry('c', "<red>"),
            Map.entry('d', "<light_purple>"),
            Map.entry('e', "<yellow>"),
            Map.entry('f', "<white>"),
            Map.entry('k', "<obfuscated>"),
            Map.entry('l', "<bold>"),
            Map.entry('m', "<strikethrough>"),
            Map.entry('n', "<underlined>"),
            Map.entry('o', "<italic>"),
            Map.entry('r', "<reset>")
    );

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private Text() {
    }

    public static Component render(String raw) {
        return render(raw, Map.of());
    }

    public static Component render(String raw, Map<String, String> placeholders) {
        if (raw == null || raw.isEmpty()) {
            return Component.empty();
        }
        String filled = applyPlaceholders(raw, placeholders);
        try {
            return MINI_MESSAGE.deserialize(toMiniMessage(filled));
        } catch (RuntimeException brokenTag) {
            // A malformed tag in an admin-written config must not break the command.
            return Component.text(filled);
        }
    }

    public static String plain(Component component) {
        return PlainTextComponentSerializer.plainText().serialize(component);
    }

    /**
     * Rewrites legacy colour codes into their MiniMessage equivalents.
     * Works on the raw text, so a literal {@code &} that is not a colour code stays intact.
     */
    static String toMiniMessage(String input) {
        Matcher matcher = LEGACY_CODE.matcher(input);
        StringBuilder output = new StringBuilder(input.length() + 16);
        int lastEnd = 0;
        while (matcher.find()) {
            output.append(input, lastEnd, matcher.start());
            String hex = matcher.group(1);
            if (hex != null) {
                output.append("<color:#").append(hex).append('>');
            } else {
                String tag = LEGACY_TAGS.get(Character.toLowerCase(matcher.group(2).charAt(0)));
                output.append(tag == null ? matcher.group() : tag);
            }
            lastEnd = matcher.end();
        }
        output.append(input, lastEnd, input.length());
        return output.toString();
    }

    private static String applyPlaceholders(String raw, Map<String, String> placeholders) {
        String result = raw;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            String value = entry.getValue() == null ? "" : entry.getValue();
            result = result.replace("{" + entry.getKey() + "}", value);
        }
        return result;
    }
}

