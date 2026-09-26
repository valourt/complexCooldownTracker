package com.tomcraft.cooldowntracker.hud;

import com.tomcraft.cooldowntracker.listener.GrilledPlayerTracker;
import com.tomcraft.cooldowntracker.listener.OtherPlayerTotemTracker;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.Map;

/**
 * Draws a small floating label above any player currently on totem
 * cooldown (per OtherPlayerTotemTracker) or grilled (per
 * GrilledPlayerTracker), billboarded to always face the camera - same
 * technique many "waypoint" style mods use for in-world markers. This is
 * genuinely new territory for this mod: full 3D world-space rendering
 * with camera-relative positioning, as opposed to the flat 2D HUD
 * overlays everything else here uses.
 */
public class TotemWatchWorldRenderer {

    private static final float TEXT_SCALE = 0.025f;
    private static final double HEIGHT_OFFSET = 1.1; // above the player's head - clears most cosmetic backpacks/wings
    private static final double GRILLED_HEIGHT_OFFSET = HEIGHT_OFFSET + 0.3; // stacked above the totem label so both are readable if a player has both

    // Minecraft's classic "aqua" formatting color - a light blue/cyan close
    // to the one used in Barbeque's own chat message.
    private static final int GRILLED_COLOR = 0xFF55FFFF;

    public static void register() {
        WorldRenderEvents.AFTER_ENTITIES.register(TotemWatchWorldRenderer::render);
    }

    private static void render(WorldRenderContext context) {
        Map<String, Long> totemActive = OtherPlayerTotemTracker.getActiveEndTimes();
        Map<String, Long> grilledActive = GrilledPlayerTracker.getActiveEndTimes();
        if (totemActive.isEmpty() && grilledActive.isEmpty()) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) return;

        Vec3d cameraPos = context.camera().getPos();
        float tickDelta = context.tickDelta();
        long now = System.currentTimeMillis();

        for (PlayerEntity player : client.world.getPlayers()) {
            String name = player.getGameProfile().getName();

            Long totemEndTime = totemActive.get(name);
            if (totemEndTime != null && totemEndTime - now > 0) {
                String label = "Totem " + CooldownBoxes.formatTime(totemEndTime - now);
                renderLabel(context, cameraPos, tickDelta, player, label, 0xFFFFFFFF, HEIGHT_OFFSET);
            }

            Long grilledEndTime = grilledActive.get(name);
            if (grilledEndTime != null && grilledEndTime - now > 0) {
                String label = "Grilled! " + CooldownBoxes.formatTime(grilledEndTime - now);
                renderLabel(context, cameraPos, tickDelta, player, label, GRILLED_COLOR, GRILLED_HEIGHT_OFFSET);
            }
        }
    }

    private static void renderLabel(WorldRenderContext context, Vec3d cameraPos, float tickDelta,
                                     PlayerEntity player, String label, int color, double heightOffset) {
        double x = MathHelper.lerp(tickDelta, player.prevX, player.getX());
        double y = MathHelper.lerp(tickDelta, player.prevY, player.getY());
        double z = MathHelper.lerp(tickDelta, player.prevZ, player.getZ());
        double height = player.getHeight();

        MatrixStack matrices = context.matrixStack();
        MinecraftClient client = MinecraftClient.getInstance();

        matrices.push();
        matrices.translate(x - cameraPos.x, y - cameraPos.y + height + heightOffset, z - cameraPos.z);
        matrices.multiply(context.camera().getRotation());
        matrices.scale(-TEXT_SCALE, -TEXT_SCALE, TEXT_SCALE);

        VertexConsumerProvider.Immediate consumers = client.getBufferBuilders().getEntityVertexConsumers();
        var styled = CooldownFont.styled(label, CooldownFont.BOLD);
        int width = client.textRenderer.getWidth(styled);

        client.textRenderer.draw(styled, -width / 2f, 0, color,
                false, matrices.peek().getPositionMatrix(), consumers,
                true, 0xA0000000, 0xF000F0);

        consumers.draw();
        matrices.pop();
    }
}

