package com.tomcraft.cooldowntracker.hud;

import net.minecraft.text.LiteralText;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.util.Identifier;

/**
 * The mod bundles Inter (SIL OFL, assets/cooldowntracker/font/) as a real
 * TTF font via Minecraft's resource-pack font system, instead of relying
 * on the game's built-in blocky bitmap font for the HUD text. Inter is
 * specifically designed for screen/UI legibility at small sizes, which is
 * what this HUD needs. Any text drawn with these fonts MUST be measured
 * with the matching styled Text object too (not a plain String) - the two
 * fonts don't share character widths, so measuring with the wrong one
 * causes overflow/misalignment.
 */
public class CooldownFont {

    public static final Identifier REGULAR = new Identifier("cooldowntracker", "inter");
    public static final Identifier BOLD = new Identifier("cooldowntracker", "inter_bold");

    public static MutableText styled(String text, Identifier font) {
        return new LiteralText(text).setStyle(Style.EMPTY.withFont(font));
    }
}
