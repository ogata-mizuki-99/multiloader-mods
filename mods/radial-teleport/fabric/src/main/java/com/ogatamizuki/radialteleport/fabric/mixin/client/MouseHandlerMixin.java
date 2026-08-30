package com.ogatamizuki.radialteleport.fabric.mixin.client;

import com.ogatamizuki.radialteleport.RadialTeleportClientFlags;
import com.ogatamizuki.radialteleport.TeleportDestination;
import com.ogatamizuki.radialteleport.TeleportRequestPayload;
import com.ogatamizuki.radialteleport.client.*;
import com.ogatamizuki.radialteleport.fabric.RadialTeleportModFabricClient;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(MouseHandler.class)
public abstract class MouseHandlerMixin {

    @Shadow
    @Final
    private Minecraft minecraft;

    @Inject(method = "onButton", at = @At("HEAD"), cancellable = true)
    private void radialTeleport$onMouseButton(long window, MouseButtonInfo buttonInfo, int action, CallbackInfo ci) {
        if (this.minecraft.player == null || this.minecraft.screen != null) {
            return;
        }

        if (action != GLFW.GLFW_PRESS) {
            return;
        }

        int button = buttonInfo.button();

        if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            if (!RadialTeleportSession.isActive() && RadialTeleportModFabricClient.isWaypointModifierDown()) {
                LocalPlayer player = this.minecraft.player;
                if (player != null && RadialTeleportClientHooks.isHoldingCompass(player)) {
                    ci.cancel();
                    if (!RadialTeleportClientFlags.enableWaypoints()) {
                        player.sendSystemMessage(
                                Component.translatable("radial_teleport.message.waypoints_disabled")
                                        .withStyle(ChatFormatting.RED));
                        return;
                    }
                    WaypointSaveScreen.open();
                    return;
                }
            }
        }

        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && RadialTeleportSession.isActive()) {
            if (RadialTeleportOverlay.isMouseOverCenter()) {
                ci.cancel();
                if (!RadialTeleportClientFlags.enableWaypoints()) {
                    if (this.minecraft.player != null) {
                        this.minecraft.player.sendSystemMessage(
                                Component.translatable("radial_teleport.message.waypoints_disabled")
                                        .withStyle(ChatFormatting.RED));
                    }
                    return;
                }
                WaypointEditScreen.open();
                return;
            }

            int hoveredIndex = RadialTeleportSession.getHoveredIndex();
            if (hoveredIndex >= 0) {
                List<TeleportDestination> destinations = RadialTeleportSession.getDestinations();
                if (hoveredIndex < destinations.size()) {
                    TeleportDestination destination = destinations.get(hoveredIndex);
                    ci.cancel();
                    ClientPlayNetworking.send(new TeleportRequestPayload(destination.id()));
                }
            }
        }
    }

    @Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
    private void radialTeleport$onMouseScroll(long window, double xoffset, double yoffset, CallbackInfo ci) {
        if (RadialTeleportSession.isActive() && this.minecraft.screen == null) {
            ci.cancel();
        }
    }
}
