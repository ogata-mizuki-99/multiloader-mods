package com.ogatamizuki.sleep.neoforge;

import com.ogatamizuki.sleep.SleepClientFlags;
import com.ogatamizuki.sleep.SleepClientFlagsPayload;
import com.ogatamizuki.sleep.SleepCommon;
import com.ogatamizuki.sleep.SleepHealthOverlay;
import com.ogatamizuki.sleep.neoforge.client.SleepConfigScreen;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;
import net.neoforged.neoforge.common.NeoForge;

@Mod(value = SleepCommon.MODID, dist = Dist.CLIENT)
public class SleepModNeoForgeClient {
    public SleepModNeoForgeClient(ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, SleepConfigScreen::new);
        container.getEventBus().addListener(SleepModNeoForgeClient::onRegisterGuiLayers);
        container.getEventBus().addListener(SleepModNeoForgeClient::onRegisterClientPayloads);
        NeoForge.EVENT_BUS.addListener(SleepModNeoForgeClient::onClientLoggingOut);
    }

    private static void onClientLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        SleepClientFlags.clear();
    }

    private static void onRegisterClientPayloads(RegisterClientPayloadHandlersEvent event) {
        event.register(SleepClientFlagsPayload.TYPE, (payload, context) -> {
            Minecraft.getInstance().execute(() -> SleepClientFlags.apply(payload));
        });
    }

    private static void onRegisterGuiLayers(RegisterGuiLayersEvent event) {
        event.wrapLayer(VanillaGuiLayers.PLAYER_HEALTH, original -> (guiGraphics, deltaTracker) ->
                SleepHealthOverlay.render(guiGraphics, deltaTracker, original::render));
    }
}
