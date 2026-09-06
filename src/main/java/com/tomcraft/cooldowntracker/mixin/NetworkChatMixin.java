package com.tomcraft.cooldowntracker.mixin;

import com.tomcraft.cooldowntracker.listener.ChatCooldownListener;
import com.tomcraft.cooldowntracker.listener.MoodSwingsTracker;
import com.tomcraft.cooldowntracker.listener.OtherPlayerTotemTracker;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.network.packet.s2c.play.OverlayMessageS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * In 1.18.2, GameMessageS2CPacket covers ALL server-sent text (system
 * notices AND regular player chat, distinguished internally by a
 * MessageType field) before it ever reaches ChatHud. This is the confirmed-
 * working interception point for this client environment (ChatHud-level
 * mixins and Accessors were silently no-ops here; this network-level packet
 * handler is not).
 *
 * Also hooks onEntityStatus here for the same reason - it's a proven-
 * working injection point in this environment, and totem-pop detection for
 * OTHER players needs it (unlike your own totem, which GameStatePoller
 * already covers via health/absorption polling and doesn't need this).
 *
 * Also hooks onOverlayMessage - the action bar text above the hotbar is a
 * genuinely different packet from regular chat (OverlayMessageS2CPacket,
 * not GameMessageS2CPacket), needed for effects some items announce there
 * instead of in chat (e.g. Mood Swings).
 */
@Mixin(ClientPlayNetworkHandler.class)
public class NetworkChatMixin {

    private static final byte TOTEM_STATUS = 35;

    @Inject(method = "onGameMessage", at = @At("HEAD"))
    private void cooldowntracker$onGameMessage(GameMessageS2CPacket packet, CallbackInfo ci) {
        ChatCooldownListener.onChatLine(packet.getMessage().getString());
    }

    @Inject(method = "onOverlayMessage", at = @At("HEAD"))
    private void cooldowntracker$onOverlayMessage(OverlayMessageS2CPacket packet, CallbackInfo ci) {
        MoodSwingsTracker.onActionBarLine(packet.getMessage().getString());
    }

    @Inject(method = "onEntityStatus", at = @At("HEAD"))
    private void cooldowntracker$onEntityStatus(EntityStatusS2CPacket packet, CallbackInfo ci) {
        if (packet.getStatus() != TOTEM_STATUS) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || client.player == null) return;

        Entity entity = packet.getEntity(client.world);
        if (entity == client.player || !(entity instanceof PlayerEntity)) return; // your own totem is handled elsewhere

        String name = ((PlayerEntity) entity).getGameProfile().getName();
        OtherPlayerTotemTracker.onOtherPlayerTotemPop(name);
    }
}

