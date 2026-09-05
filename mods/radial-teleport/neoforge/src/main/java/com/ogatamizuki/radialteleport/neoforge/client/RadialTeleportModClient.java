package com.ogatamizuki.radialteleport.neoforge.client;

import com.ogatamizuki.radialteleport.RadialTeleportClientFlags;
import com.ogatamizuki.radialteleport.RadialTeleportClientFlagsPayload;
import com.ogatamizuki.radialteleport.RadialTeleportCommon;
import com.ogatamizuki.radialteleport.TeleportDestination;
import com.ogatamizuki.radialteleport.TeleportDestinationsPayload;
import com.ogatamizuki.radialteleport.TeleportRequestPayload;
import com.ogatamizuki.radialteleport.TeleportResultPayload;
import com.ogatamizuki.radialteleport.WaypointListPayload;
import com.ogatamizuki.radialteleport.client.*;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

import java.util.List;

@Mod(value = RadialTeleportCommon.MODID, dist = Dist.CLIENT)
public class RadialTeleportModClient {

    public RadialTeleportModClient(ModContainer container) {
        RadialTeleportCommon.sendToServer = payload -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.getConnection() != null) {
                mc.getConnection().send(payload);
            }
        };

        container.registerExtensionPoint(IConfigScreenFactory.class, RadialTeleportConfigScreen::new);

        IEventBus modEventBus = container.getEventBus();
        modEventBus.addListener(RadialTeleportModClient::onRegisterGuiLayers);
        modEventBus.addListener(RadialTeleportModClient::onRegisterClientPayloads);
        modEventBus.addListener(RadialTeleportKeyBindings::register);

        NeoForge.EVENT_BUS.addListener(RadialTeleportModClient::onClientTick);
        NeoForge.EVENT_BUS.addListener(RadialTeleportModClient::onMouseInput);
        NeoForge.EVENT_BUS.addListener(RadialTeleportModClient::onMouseScroll);
        NeoForge.EVENT_BUS.addListener(RadialTeleportModClient::onClientLoggingOut);
    }

    private static void onClientLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        RadialTeleportClientFlags.clear();
    }

    private static void onRegisterGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(
                VanillaGuiLayers.CROSSHAIR,
                Identifier.fromNamespaceAndPath(RadialTeleportCommon.MODID, "radial_menu"),
                RadialTeleportOverlay::render
        );
    }

    private static void onRegisterClientPayloads(RegisterClientPayloadHandlersEvent event) {
        event.register(TeleportDestinationsPayload.TYPE, (payload, context) -> {
            Minecraft mc = Minecraft.getInstance();
            mc.execute(() -> RadialTeleportSession.updateDestinations(payload));
        });

        event.register(TeleportResultPayload.TYPE, (payload, context) -> {
            Minecraft mc = Minecraft.getInstance();
            mc.execute(() -> {
                if (mc.player != null) {
                    mc.player.sendSystemMessage(payload.toComponent());
                }
                if (payload.success()) {
                    RadialTeleportSession.end(mc);
                }
            });
        });

        event.register(WaypointListPayload.TYPE, (payload, context) -> {
            Minecraft mc = Minecraft.getInstance();
            mc.execute(() -> WaypointEditScreen.applyList(payload.waypoints()));
        });

        event.register(RadialTeleportClientFlagsPayload.TYPE, (payload, context) -> {
            Minecraft mc = Minecraft.getInstance();
            mc.execute(() -> RadialTeleportClientFlags.apply(payload));
        });
    }

    private static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
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
    }

    private static void onMouseInput(InputEvent.MouseButton.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.gui.screen() != null) {
            return;
        }

        if (event.getAction() != GLFW.GLFW_PRESS) {
            return;
        }

        if (event.getButton() == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            handleWaypointSaveClick(mc, event);
            return;
        }

        if (event.getButton() != GLFW.GLFW_MOUSE_BUTTON_LEFT || !RadialTeleportSession.isActive()) {
            return;
        }

        if (RadialTeleportOverlay.isMouseOverCenter()) {
            event.setCanceled(true);
            if (!RadialTeleportClientFlags.enableWaypoints()) {
                if (mc.player != null) {
                    mc.player.sendSystemMessage(
                            Component.translatable("radial_teleport.message.waypoints_disabled")
                                    .withStyle(ChatFormatting.RED));
                }
                return;
            }
            WaypointEditScreen.open();
            return;
        }

        int hoveredIndex = RadialTeleportSession.getHoveredIndex();
        if (hoveredIndex < 0) {
            return;
        }

        List<TeleportDestination> destinations = RadialTeleportSession.getDestinations();
        if (hoveredIndex >= destinations.size()) {
            return;
        }

        TeleportDestination destination = destinations.get(hoveredIndex);
        event.setCanceled(true);

        if (mc.getConnection() != null) {
            mc.getConnection().send(new TeleportRequestPayload(destination.id()));
        }
    }

    private static void handleWaypointSaveClick(Minecraft mc, InputEvent.MouseButton.Pre event) {
        if (RadialTeleportSession.isActive() || !RadialTeleportKeyBindings.isWaypointModifierDown()) {
            return;
        }

        LocalPlayer player = mc.player;
        if (player == null || !RadialTeleportClientHooks.isHoldingCompass(player)) {
            return;
        }

        event.setCanceled(true);
        if (!RadialTeleportClientFlags.enableWaypoints()) {
            player.sendSystemMessage(
                    Component.translatable("radial_teleport.message.waypoints_disabled")
                            .withStyle(ChatFormatting.RED));
            return;
        }
        WaypointSaveScreen.open();
    }

    private static void onMouseScroll(InputEvent.MouseScrollingEvent event) {
        if (!RadialTeleportSession.isActive()) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.gui.screen() != null) {
            return;
        }

        event.setCanceled(true);
    }
}
