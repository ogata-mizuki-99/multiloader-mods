package com.ogatamizuki.sleep.fabric;

import com.ogatamizuki.sleep.SleepClientFlags;
import com.ogatamizuki.sleep.SleepClientFlagsPayload;
import com.ogatamizuki.sleep.SleepCommon;
import com.ogatamizuki.sleep.SleepHealthOverlay;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;

public class SleepModFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        SleepCommon.LOGGER.info("Good Sleep Mod (Fabric Client) Initializing...");

        // パケット受信
        ClientPlayNetworking.registerGlobalReceiver(SleepClientFlagsPayload.TYPE, (payload, context) -> {
            context.client().execute(() -> SleepClientFlags.apply(payload));
        });

        // ログアウト時
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            SleepClientFlags.clear();
        });

        // HUD オーバーレイ（ヘルスバー描画の置換/ラップ）
        HudElementRegistry.replaceElement(VanillaHudElements.HEALTH_BAR, original ->
                (guiGraphics, deltaTracker) -> SleepHealthOverlay.render(guiGraphics, deltaTracker, original::extractRenderState));
    }
}
