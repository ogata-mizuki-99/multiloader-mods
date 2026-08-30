package com.ogatamizuki.lookalike.fabric;

import com.ogatamizuki.lookalike.LookalikeClientFlags;
import com.ogatamizuki.lookalike.LookalikeClientFlagsPayload;
import com.ogatamizuki.lookalike.LookalikeCommon;
import com.ogatamizuki.lookalike.NetworkPayloads;
import com.ogatamizuki.lookalike.client.*;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Minecraft;

import java.util.UUID;

public class LookalikeModFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        LookalikeCommon.LOGGER.info("Lookalike Mod (Fabric Client) Initializing...");
        LookalikeCommon.sendToServer = ClientPlayNetworking::send;

        // HUD Elements
        HudElementRegistry.attachElementAfter(
                VanillaHudElements.CROSSHAIR,
                LookalikeCommon.id("disguise_radial_menu"),
                LookalikeRadialOverlay::render
        );

        // Network Handlers
        ClientPlayNetworking.registerGlobalReceiver(NetworkPayloads.ScanHistorySyncPayload.TYPE, (payload, context) -> {
            context.client().execute(() -> {
                LookalikeClientState.applyScanHistorySync(payload);
            });
        });

        ClientPlayNetworking.registerGlobalReceiver(NetworkPayloads.DisguiseListSyncPayload.TYPE, (payload, context) -> {
            context.client().execute(() -> {
                LookalikeClientState.applyDisguiseListSync(payload);
                LookalikeClientSkins.applyDisguiseListSync(payload.disguisedPlayers());
            });
        });

        ClientPlayNetworking.registerGlobalReceiver(NetworkPayloads.ShadowAppearanceSyncPayload.TYPE, (payload, context) -> {
            context.client().execute(() -> {
                LookalikeClientShadows.apply(payload);
            });
        });

        ClientPlayNetworking.registerGlobalReceiver(LookalikeClientFlagsPayload.TYPE, (payload, context) -> {
            context.client().execute(() -> {
                LookalikeClientState.applyClientFlags(payload);
            });
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            LookalikeClientState.clear();
        });

        // Client Tick (Shadow particles & Disguise Mirror radial session)
        net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.END_CLIENT_TICK.register(client -> {
            ShadowAppearanceEffects.tick();

            if (client.player == null) {
                if (LookalikeRadialSession.isActive()) {
                    LookalikeRadialSession.cancel(client);
                }
                wasUsingMirror = false;
                return;
            }

            if (client.screen instanceof ScanHistoryEditScreen) {
                wasUsingMirror = client.player.isUsingItem()
                        && LookalikeCommon.DISGUISE_MIRROR != null
                        && client.player.getUseItem().is(LookalikeCommon.DISGUISE_MIRROR.get());
                return;
            }

            if (client.screen != null) {
                if (LookalikeRadialSession.isActive()) {
                    LookalikeRadialSession.cancel(client);
                }
                wasUsingMirror = false;
                return;
            }

            boolean usingMirror = client.player.isUsingItem()
                    && LookalikeCommon.DISGUISE_MIRROR != null
                    && client.player.getUseItem().is(LookalikeCommon.DISGUISE_MIRROR.get());

            if (usingMirror && !wasUsingMirror) {
                LookalikeRadialSession.begin(client);
            } else if (!usingMirror && wasUsingMirror) {
                LookalikeRadialSession.closeWithoutSelecting(client);
            } else if (usingMirror) {
                LookalikeRadialSession.tick(client);
            }

            wasUsingMirror = usingMirror;
        });
    }

    private static boolean wasUsingMirror = false;
}
