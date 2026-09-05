package com.ogatamizuki.radialteleport.fabric;

import com.mojang.blaze3d.platform.InputConstants;
import com.ogatamizuki.radialteleport.*;
import com.ogatamizuki.radialteleport.client.*;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public class RadialTeleportModFabricClient implements ClientModInitializer {
    public static final KeyMapping.Category CATEGORY = new KeyMapping.Category(
            Identifier.fromNamespaceAndPath(RadialTeleportCommon.MODID, "main")
    );
    public static KeyMapping WAYPOINT_MODIFIER;

    @Override
    public void onInitializeClient() {
        RadialTeleportModFabric.LOGGER.info("Radial Teleport Mod (Fabric Client) Initializing...");

        RadialTeleportCommon.sendToServer = ClientPlayNetworking::send;

        // Register KeyBinding
        WAYPOINT_MODIFIER = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.radial_teleport.waypoint_modifier",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_LEFT_SHIFT,
                CATEGORY
        ));

        // Register HUD Overlay
        HudElementRegistry.attachElementAfter(
                VanillaHudElements.CROSSHAIR,
                Identifier.fromNamespaceAndPath(RadialTeleportCommon.MODID, "radial_menu"),
                RadialTeleportOverlay::render
        );

        // Network S2C Handlers
        ClientPlayNetworking.registerGlobalReceiver(TeleportDestinationsPayload.TYPE, (payload, context) -> {
            context.client().execute(() -> RadialTeleportSession.updateDestinations(payload));
        });

        ClientPlayNetworking.registerGlobalReceiver(TeleportResultPayload.TYPE, (payload, context) -> {
            context.client().execute(() -> {
                if (context.client().player != null) {
                    context.client().player.sendSystemMessage(payload.toComponent());
                }
                if (payload.success()) {
                    RadialTeleportSession.end(context.client());
                }
            });
        });

        ClientPlayNetworking.registerGlobalReceiver(WaypointListPayload.TYPE, (payload, context) -> {
            context.client().execute(() -> WaypointEditScreen.applyList(payload.waypoints()));
        });

        ClientPlayNetworking.registerGlobalReceiver(RadialTeleportClientFlagsPayload.TYPE, (payload, context) -> {
            context.client().execute(() -> RadialTeleportClientFlags.apply(payload));
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            RadialTeleportClientFlags.clear();
        });

        // Client Tick
        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            LocalPlayer player = mc.player;
            if (player == null || mc.gui.screen() != null) {
                if (RadialTeleportSession.isActive()) {
                    RadialTeleportSession.end(mc);
                }
                RadialTeleportClientHooks.setWasUsingCompass(false);
                return;
            }

            boolean usingCompass = RadialTeleportClientHooks.isActivelyUsingCompass(player)
                    || RadialTeleportClientHooks.isSpectatorRadialUse(mc, player);

            boolean wasUsingCompass = RadialTeleportClientHooks.wasUsingCompass();

            if (usingCompass && !wasUsingCompass) {
                RadialTeleportSession.begin(mc);
            } else if (!usingCompass && wasUsingCompass) {
                RadialTeleportSession.end(mc);
            } else if (usingCompass) {
                RadialTeleportSession.tick(player);
                if (RadialTeleportSession.shouldRefreshLocal(mc)) {
                    RadialTeleportSession.refreshDisplayNames(mc);
                    RadialTeleportSession.requestDestinationsFromServer(mc);
                }
            }

            RadialTeleportClientHooks.setWasUsingCompass(usingCompass);
        });
    }

    public static boolean isWaypointModifierDown() {
        return WAYPOINT_MODIFIER != null && WAYPOINT_MODIFIER.isDown();
    }
}
