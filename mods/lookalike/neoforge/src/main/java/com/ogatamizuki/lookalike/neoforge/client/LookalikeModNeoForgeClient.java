package com.ogatamizuki.lookalike.neoforge.client;

import com.ogatamizuki.lookalike.LookalikeAttachments.ScanEntry;
import com.ogatamizuki.lookalike.LookalikeClientFlags;
import com.ogatamizuki.lookalike.LookalikeClientFlagsPayload;
import com.ogatamizuki.lookalike.LookalikeCommon;
import com.ogatamizuki.lookalike.NetworkPayloads;
import com.ogatamizuki.lookalike.client.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.lwjgl.glfw.GLFW;

import java.util.List;

@Mod(value = LookalikeCommon.MODID, dist = Dist.CLIENT)
public class LookalikeModNeoForgeClient {
    public static List<ScanEntry> getScanHistory() {
        return LookalikeClientState.getScanHistory();
    }

    public static boolean isDisguised(java.util.UUID uuid) {
        return LookalikeClientState.isDisguised(uuid);
    }

    private static boolean wasUsingMirror = false;

    public LookalikeModNeoForgeClient(ModContainer container) {
        LookalikeCommon.sendToServer = payload -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.getConnection() != null) {
                mc.getConnection().send(payload);
            }
        };
        IEventBus modEventBus = container.getEventBus();
        modEventBus.addListener(this::registerClientPayloads);
        modEventBus.addListener(LookalikeModNeoForgeClient::onRegisterGuiLayers);
        NeoForge.EVENT_BUS.addListener(LookalikeModNeoForgeClient::onClientTick);
        NeoForge.EVENT_BUS.addListener(LookalikeModNeoForgeClient::onMouseInput);
        NeoForge.EVENT_BUS.addListener(LookalikeModNeoForgeClient::onMouseScroll);
        NeoForge.EVENT_BUS.addListener(LookalikeModNeoForgeClient::onClientLoggingOut);
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post event) -> ShadowAppearanceEffects.tick());
        NeoForge.EVENT_BUS.register(this);
        container.registerExtensionPoint(IConfigScreenFactory.class, LookalikeConfigScreen::new);
    }

    private static void onClientLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        LookalikeClientState.clear();
    }

    private static void onRegisterGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(
                VanillaGuiLayers.CROSSHAIR,
                Identifier.fromNamespaceAndPath(LookalikeCommon.MODID, "disguise_radial_menu"),
                LookalikeRadialOverlay::render
        );
    }

    private void registerClientPayloads(RegisterClientPayloadHandlersEvent event) {
        event.register(NetworkPayloads.ScanHistorySyncPayload.TYPE, (payload, context) -> {
            Minecraft mc = Minecraft.getInstance();
            mc.execute(() -> {
                LookalikeClientState.applyScanHistorySync(payload);
            });
        });

        event.register(NetworkPayloads.DisguiseListSyncPayload.TYPE, (payload, context) -> {
            Minecraft mc = Minecraft.getInstance();
            mc.execute(() -> {
                LookalikeClientState.applyDisguiseListSync(payload);
                LookalikeClientSkins.applyDisguiseListSync(payload.disguisedPlayers());
            });
        });

        event.register(NetworkPayloads.ShadowAppearanceSyncPayload.TYPE, (payload, context) -> {
            Minecraft mc = Minecraft.getInstance();
            mc.execute(() -> LookalikeClientShadows.apply(payload));
        });

        event.register(LookalikeClientFlagsPayload.TYPE, (payload, context) -> {
            Minecraft mc = Minecraft.getInstance();
            mc.execute(() -> LookalikeClientState.applyClientFlags(payload));
        });
    }

    private static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) {
            if (LookalikeRadialSession.isActive()) {
                LookalikeRadialSession.cancel(mc);
            }
            wasUsingMirror = false;
            return;
        }

        if (mc.screen instanceof ScanHistoryEditScreen) {
            wasUsingMirror = player.isUsingItem()
                    && player.getUseItem().is(LookalikeCommon.DISGUISE_MIRROR.get());
            return;
        }

        if (mc.screen != null) {
            if (LookalikeRadialSession.isActive()) {
                LookalikeRadialSession.cancel(mc);
            }
            wasUsingMirror = false;
            return;
        }

        boolean usingMirror = player.isUsingItem()
                && player.getUseItem().is(LookalikeCommon.DISGUISE_MIRROR.get());

        if (usingMirror && !wasUsingMirror) {
            LookalikeRadialSession.begin(mc);
        } else if (!usingMirror && wasUsingMirror) {
            LookalikeRadialSession.closeWithoutSelecting(mc);
        } else if (usingMirror) {
            LookalikeRadialSession.tick(mc);
        }

        wasUsingMirror = usingMirror;
    }

    public static void onScanScreenOpened() {
        Minecraft mc = Minecraft.getInstance();
        if (LookalikeRadialSession.isActive()) {
            LookalikeRadialSession.closeWithoutSelecting(mc);
        }
        LocalPlayer player = mc.player;
        if (player != null) {
            wasUsingMirror = player.isUsingItem()
                    && player.getUseItem().is(LookalikeCommon.DISGUISE_MIRROR.get());
        }
    }

    public static void onScanScreenClosed() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player != null) {
            wasUsingMirror = player.isUsingItem()
                    && player.getUseItem().is(LookalikeCommon.DISGUISE_MIRROR.get());
        }
    }

    private static void onMouseInput(InputEvent.MouseButton.Pre event) {
        if (event.getAction() != GLFW.GLFW_PRESS || event.getButton() != GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            return;
        }
        if (!LookalikeRadialSession.isActive()) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null) {
            return;
        }

        event.setCanceled(true);

        LookalikeRadialSession.refreshHoverForInput(mc);

        if (LookalikeRadialSession.isMouseOverCenter(mc)) {
            ScanHistoryEditScreen.open();
            return;
        }

        if (LookalikeRadialSession.getHoveredIndex() < 0) {
            return;
        }

        LookalikeRadialSession.confirmSelection(mc);
    }

    private static void onMouseScroll(InputEvent.MouseScrollingEvent event) {
        if (!LookalikeRadialSession.isActive()) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null) {
            return;
        }

        event.setCanceled(true);
    }

    @net.neoforged.bus.api.SubscribeEvent
    public void onRenderNameTag(net.neoforged.neoforge.client.event.RenderNameTagEvent.CanRender event) {
        if (event.getEntity() instanceof net.minecraft.world.entity.player.Player) {
            if (LookalikeClientFlags.hideAllNametags()) {
                Minecraft mc = Minecraft.getInstance();
                if (mc.getConnection() != null && !mc.isSingleplayer()) {
                    event.setCanRender(net.minecraft.util.TriState.FALSE);
                }
            }
        }
    }
}
