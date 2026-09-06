package com.tomcraft.cooldowntracker.hud;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

/**
 * Draws the bundled rounded-rect texture (assets/cooldowntracker/textures/
 * gui/panel.png - a 64x64 white shape with a genuinely anti-aliased rounded
 * corner, corner size 20px) as a 9-slice: 4 corners drawn 1:1, edges and
 * center stretched to fit any box size. This is a real curved shape, not
 * an approximation from flat rects - Minecraft's fill() can only draw
 * straight-edged rectangles, so a true rounded look needs an actual
 * texture. The white source is tinted at draw time via the shader color,
 * so one texture serves any background color/opacity the user configures.
 */
public class RoundedPanel {

    private static final Identifier TEXTURE = new Identifier("cooldowntracker", "textures/gui/panel.png");
    private static final int TEX_SIZE = 64;
    private static final int CORNER = 8;
    private static final int EDGE = TEX_SIZE - CORNER * 2;

    public static void draw(MatrixStack matrices, int x, int y, int width, int height, int argbColor) {
        float a = ((argbColor >> 24) & 0xFF) / 255f;
        float r = ((argbColor >> 16) & 0xFF) / 255f;
        float g = ((argbColor >> 8) & 0xFF) / 255f;
        float b = (argbColor & 0xFF) / 255f;

        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, TEXTURE);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(r, g, b, a);

        int cornerW = Math.min(CORNER, width / 2);
        int cornerH = Math.min(CORNER, height / 2);
        int midW = Math.max(0, width - cornerW * 2);
        int midH = Math.max(0, height - cornerH * 2);

        // Corners - 1:1, no stretch.
        drawRegion(matrices, x, y, cornerW, cornerH, 0, 0, cornerW, cornerH);
        drawRegion(matrices, x + width - cornerW, y, cornerW, cornerH, TEX_SIZE - cornerW, 0, cornerW, cornerH);
        drawRegion(matrices, x, y + height - cornerH, cornerW, cornerH, 0, TEX_SIZE - cornerH, cornerW, cornerH);
        drawRegion(matrices, x + width - cornerW, y + height - cornerH, cornerW, cornerH, TEX_SIZE - cornerW, TEX_SIZE - cornerH, cornerW, cornerH);

        // Edges - stretched along one axis.
        if (midW > 0) {
            drawRegion(matrices, x + cornerW, y, midW, cornerH, CORNER, 0, EDGE, cornerH);
            drawRegion(matrices, x + cornerW, y + height - cornerH, midW, cornerH, CORNER, TEX_SIZE - cornerH, EDGE, cornerH);
        }
        if (midH > 0) {
            drawRegion(matrices, x, y + cornerH, cornerW, midH, 0, CORNER, cornerW, EDGE);
            drawRegion(matrices, x + width - cornerW, y + cornerH, cornerW, midH, TEX_SIZE - cornerW, CORNER, cornerW, EDGE);
        }

        // Center - stretched both axes.
        if (midW > 0 && midH > 0) {
            drawRegion(matrices, x + cornerW, y + cornerH, midW, midH, CORNER, CORNER, EDGE, EDGE);
        }

        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.disableBlend();
    }

    private static void drawRegion(MatrixStack matrices, int x, int y, int width, int height,
                                    int u, int v, int regionWidth, int regionHeight) {
        DrawableHelper.drawTexture(matrices, x, y, width, height, u, v, regionWidth, regionHeight, TEX_SIZE, TEX_SIZE);
    }

    /**
     * Parses a 6-digit hex string (with or without leading #) plus a
     * 0-100 opacity percentage into a single ARGB int, defaulting to a
     * sane dark color if the hex is invalid.
     */
    public static int toArgb(String hex, int opacityPercent) {
        String cleaned = hex == null ? "1E1E1E" : (hex.startsWith("#") ? hex.substring(1) : hex);
        int rgb;
        try {
            rgb = Integer.parseInt(cleaned, 16) & 0xFFFFFF;
        } catch (NumberFormatException e) {
            rgb = 0x1E1E1E;
        }
        int clampedOpacity = Math.max(0, Math.min(100, opacityPercent));
        int alpha = (int) (clampedOpacity / 100f * 255f);
        return (alpha << 24) | rgb;
    }
}
