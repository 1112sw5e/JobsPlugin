package com.tradewind.text;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;
import org.junit.jupiter.api.Test;

class TextTest {

    @Test
    void rendersLegacyColourCodes() {
        assertEquals("Привет", Text.plain(Text.render("&aПривет")));
    }

    @Test
    void rendersMiniMessageTags() {
        assertEquals("Привет", Text.plain(Text.render("<red>Привет</red>")));
    }

    @Test
    void mixesMiniMessageAndLegacyInOneLine() {
        assertEquals("a b", Text.plain(Text.render("<bold>a</bold> &7b")));
    }

    @Test
    void rendersLegacyHexAndPlaceholders() {
        assertEquals("Шахта", Text.plain(Text.render("&#ffaa00{name}", Map.of("name", "Шахта"))));
    }

    @Test
    void keepsUnknownPlaceholdersLiteral() {
        assertEquals("{unknown}", Text.plain(Text.render("{unknown}")));
    }

    @Test
    void emptyInputRendersEmptyComponent() {
        assertEquals("", Text.plain(Text.render("")));
        assertEquals("", Text.plain(Text.render(null)));
    }
}

