package com.ogatamizuki.lookalike.client;

import com.ogatamizuki.lookalike.LookalikeAttachments.ScanEntry;
import com.ogatamizuki.lookalike.LookalikeClientFlags;
import com.ogatamizuki.lookalike.LookalikeClientFlagsPayload;
import com.ogatamizuki.lookalike.LookalikeMod;
import com.ogatamizuki.lookalike.NetworkPayloads;
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

import java.util.ArrayList;
import java.util.List;

@Mod(value = LookalikeMod.MODID, dist = Dist.CLIENT)
public class LookalikeModClient {
    private static final List<ScanEntry> scanHistory = new ArrayList<>();
    private static final java.util.Set<java.util.UUID> disguisedUuids = java.util.concurrent.ConcurrentHashMap.newKeySet();

    /** スキャン履歴の読み取り専用ビューを返す。 */
    static List<ScanEntry> getScanHistory() {
        return java.util.Collections.unmodifiableList(scanHistory);
    }

    /** 指定 UUID が現在変装中かどうかを返す。 */
    static boolean isDisguised(java.util.UUID uuid) {
        return disguisedUuids.contains(uuid);
    }

    private static boolean wasUsingMirror = false;

    public LookalikeModClient(ModContainer container) {
        IEventBus modEventBus = container.getEventBus();
        modEventBus.addListener(this::registerClientPayloads);
        modEventBus.addListener(LookalikeModClient::onRegisterGuiLayers);
        NeoForge.EVENT_BUS.addListener(LookalikeModClient::onClientTick);
        NeoForge.EVENT_BUS.addListener(LookalikeModClient::onMouseInput);
        NeoForge.EVENT_BUS.addListener(LookalikeModClient::onMouseScroll);
        NeoForge.EVENT_BUS.addListener(LookalikeModClient::onClientLoggingOut);
        NeoForge.EVENT_BUS.register(ShadowAppearanceEffects.class);
        NeoForge.EVENT_BUS.register(this);
        container.registerExtensionPoint(IConfigScreenFactory.class, LookalikeConfigScreen::new);
    }

    private static void onClientLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        LookalikeClientFlags.clear();
    }

    private static void onRegisterGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(
                VanillaGuiLayers.CROSSHAIR,
                Identifier.fromNamespaceAndPath(LookalikeMod.MODID, "disguise_radial_menu"),
                LookalikeRadialOverlay::render
        );
    }

    private void registerClientPayloads(RegisterClientPayloadHandlersEvent event) {
        event.register(NetworkPayloads.ScanHistorySyncPayload.TYPE, (payload, context) -> {
            Minecraft mc = Minecraft.getInstance();
            mc.execute(() -> {
                scanHistory.clear();
                scanHistory.addAll(payload.entries());
                if (mc.screen instanceof ScanHistoryEditScreen screen) {
                    screen.updateEntries(payload.entries());
                }
            });
        });

        event.register(NetworkPayloads.DisguiseListSyncPayload.TYPE, (payload, context) -> {
            Minecraft mc = Minecraft.getInstance();
            mc.execute(() -> {
                disguisedUuids.clear();
                for (NetworkPayloads.DisguiseSkinEntry entry : payload.disguisedPlayers()) {
                    disguisedUuids.add(java.util.UUID.fromString(entry.uuidStr()));
                }
                LookalikeClientSkins.applyDisguiseListSync(payload.disguisedPlayers());
            });
        });

        event.register(NetworkPayloads.ShadowAppearanceSyncPayload.TYPE, (payload, context) -> {
            Minecraft mc = Minecraft.getInstance();
            mc.execute(() -> LookalikeClientShadows.apply(payload));
        });

        event.register(LookalikeClientFlagsPayload.TYPE, (payload, context) -> {
            Minecraft mc = Minecraft.getInstance();
            mc.execute(() -> LookalikeClientFlags.apply(payload));
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
                    && player.getUseItem().is(LookalikeMod.DISGUISE_MIRROR.get());
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
                && player.getUseItem().is(LookalikeMod.DISGUISE_MIRROR.get());

        if (usingMirror && !wasUsingMirror) {
            LookalikeRadialSession.begin(mc);
        } else if (!usingMirror && wasUsingMirror) {
            LookalikeRadialSession.closeWithoutSelecting(mc);
        } else if (usingMirror) {
            LookalikeRadialSession.tick(mc);
        }

        wasUsingMirror = usingMirror;
    }

    static void onScanScreenOpened() {
        Minecraft mc = Minecraft.getInstance();
        if (LookalikeRadialSession.isActive()) {
            LookalikeRadialSession.closeWithoutSelecting(mc);
        }
        LocalPlayer player = mc.player;
        if (player != null) {
            wasUsingMirror = player.isUsingItem()
                    && player.getUseItem().is(LookalikeMod.DISGUISE_MIRROR.get());
        }
    }

    static void onScanScreenClosed() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player != null) {
            wasUsingMirror = player.isUsingItem()
                    && player.getUseItem().is(LookalikeMod.DISGUISE_MIRROR.get());
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
            // Dedicated ではサーバー同期値を優先（ローカル toml だけでは他プレイヤーに効かない）
            if (LookalikeClientFlags.hideAllNametags()) {
                Minecraft mc = Minecraft.getInstance();
                if (mc.getConnection() != null && !mc.isSingleplayer()) {
                    event.setCanRender(net.minecraft.util.TriState.FALSE);
                }
            }
        }
    }
}
