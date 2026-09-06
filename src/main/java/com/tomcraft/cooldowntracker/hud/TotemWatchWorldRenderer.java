package com.tomcraft.cooldowntracker.hud;

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
 * cooldown (per OtherPlayerTotemTracker), billboarded to always face the
 * camera - same technique many "waypoint" style mods use for in-world
 * markers. This is genuinely new territory for this mod: full 3D
 * world-space rendering with camera-relative positioning, as opposed to
 * the flat 2D HUD overlays everything else here uses.
 */
public class TotemWatchWorldRenderer {

    private static final float TEXT_SCALE = 0.025f;
    private static final double HEIGHT_OFFSET = 1.1; // above the player's head - clears most cosmetic backpacks/wings

    public static void register() {
        WorldRenderEvents.AFTER_ENTITIES.register(TotemWatchWorldRenderer::render);
    }

    private static void render(WorldRenderContext context) {
        Map<String, Long> active = OtherPlayerTotemTracker.getActiveEndTimes();
        if (active.isEmpty()) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) return;

        Vec3d cameraPos = context.camera().getPos();
        float tickDelta = context.tickDelta();
        long now = System.currentTimeMillis();

        for (PlayerEntity player : client.world.getPlayers()) {
            Long endTime = active.get(player.getGameProfile().getName());
            if (endTime == null) continue;
            long remaining = endTime - now;
            if (remaining <= 0) continue;

            String label = "Totem " + CooldownBoxes.formatTime(remaining);
            renderLabel(context, cameraPos, tickDelta, player, label);
        }
    }

    private static void renderLabel(WorldRenderContext context, Vec3d cameraPos, float tickDelta,
                                     PlayerEntity player, String label) {
        double x = MathHelper.lerp(tickDelta, player.prevX, player.getX());
        double y = MathHelper.lerp(tickDelta, player.prevY, player.getY());
        double z = MathHelper.lerp(tickDelta, player.prevZ, player.getZ());
        double height = player.getHeight();

        MatrixStack matrices = context.matrixStack();
        MinecraftClient client = MinecraftClient.getInstance();

        matrices.push();
        matrices.translate(x - cameraPos.x, y - cameraPos.y + height + HEIGHT_OFFSET, z - cameraPos.z);
        matrices.multiply(context.camera().getRotation());
        matrices.scale(-TEXT_SCALE, -TEXT_SCALE, TEXT_SCALE);

        VertexConsumerProvider.Immediate consumers = client.getBufferBuilders().getEntityVertexConsumers();
        var styled = CooldownFont.styled(label, CooldownFont.BOLD);
        int width = client.textRenderer.getWidth(styled);

        client.textRenderer.draw(styled, -width / 2f, 0, 0xFFFFFFFF,
                false, matrices.peek().getPositionMatrix(), consumers,
                true, 0xA0000000, 0xF000F0);

        consumers.draw();
        matrices.pop();
    }
}
