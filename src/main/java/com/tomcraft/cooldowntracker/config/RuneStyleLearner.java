package com.tomcraft.cooldowntracker.config;

import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads a rune's icon glyph and theme colour out of the game's own styled
 * text. Rune lines all share one shape - "<icon> <Name> <Tier>" followed by
 * end-of-line, "|" (chat) or "(" (lore) - e.g. "[icon] Lightning Crash I | ..."
 * in chat, or "[icon] Fortify I (1)" in an item's lore. The name, icon and
 * tier segments carry the server's colour styling, which plain getString()
 * throws away. Colour priority: name colour, then icon colour, then tier
 * colour - skipping white/greys, since item tooltips show the name in white
 * with the theme colour on the icon and tier instead.
 */
public class RuneStyleLearner {

    private static final Pattern LINE = Pattern.compile(
            "^\\s*(?:([^\\sA-Za-z0-9]+)\\s+)?([A-Za-z][A-Za-z' \\-]*?)\\s+(I|II|III|IV|V|VI|VII|VIII|IX|X)(?=$|[\\s|(:])");

    /** Chat: rune lines always contain a pipe, so skip everything else without touching regex. */
    public static void learnFromChat(Text text) {
        if (text == null) return;
        String plain = text.getString();
        if (plain.indexOf('|') < 0) return;
        learn(text, plain, true);
    }

    /** Item names and lore lines. */
    public static void learnFromItemText(Text text) {
        if (text == null) return;
        learn(text, text.getString(), false);
    }

    private static void learn(Text text, String plain, boolean chat) {
        if (plain.isEmpty() || plain.length() > 300) return;
        Matcher m = LINE.matcher(plain);
        if (!m.find()) return;

        // Style of every character, with parent styles already merged in.
        Style[] styleAt = new Style[plain.length()];
        int[] pos = {0};
        text.visit((style, s) -> {
            for (int i = 0; i < s.length() && pos[0] + i < styleAt.length; i++) {
                styleAt[pos[0] + i] = style;
            }
            pos[0] += s.length();
            return Optional.empty();
        }, Style.EMPTY);

        int glyphStart = m.group(1) != null ? m.start(1) : -1;
        Integer nameC = colorAt(styleAt, m.start(2));
        Integer glyphC = glyphStart >= 0 ? colorAt(styleAt, glyphStart) : null;
        Integer tierC = colorAt(styleAt, m.start(3));
        Integer theme = firstThemed(nameC, glyphC, tierC);

        String glyph = null;
        String font = null;
        int glyphColor = 0;
        if (glyphStart >= 0) {
            glyph = m.group(1);
            Style gs = styleAt[glyphStart];
            if (gs != null && gs.getFont() != null) font = gs.getFont().toString();
            Integer gc = glyphC != null ? glyphC : theme;
            glyphColor = gc != null ? (0xFF000000 | gc) : 0xFFFFFFFF;
        }

        int color = theme != null ? (0xFF000000 | theme) : 0;
        if (color == 0 && glyph == null) return;
        RuneStyleCache.learn(TextMatch.normalize(m.group(2)), color, glyph, font, glyphColor, chat);
    }

    private static Integer colorAt(Style[] styleAt, int idx) {
        if (idx < 0 || idx >= styleAt.length) return null;
        Style s = styleAt[idx];
        if (s == null) return null;
        TextColor c = s.getColor();
        return c == null ? null : c.getRgb();
    }

    private static boolean isNeutral(int rgb) {
        return rgb == 0xFFFFFF || rgb == 0xAAAAAA || rgb == 0x555555 || rgb == 0x000000;
    }

    private static Integer firstThemed(Integer... colors) {
        for (Integer c : colors) {
            if (c != null && !isNeutral(c)) return c;
        }
        return null;
    }
}
