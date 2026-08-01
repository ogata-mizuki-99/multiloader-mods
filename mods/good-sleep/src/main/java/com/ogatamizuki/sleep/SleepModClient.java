package com.ogatamizuki.sleep;

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
import com.ogatamizuki.sleep.client.SleepConfigScreen;

@Mod(value = "good_sleep", dist = Dist.CLIENT)
public class SleepModClient {
    public SleepModClient(ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, SleepConfigScreen::new);
        container.getEventBus().addListener(SleepModClient::onRegisterGuiLayers);
        container.getEventBus().addListener(SleepModClient::onRegisterClientPayloads);
        NeoForge.EVENT_BUS.addListener(SleepModClient::onClientLoggingOut);
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
        event.wrapLayer(VanillaGuiLayers.PLAYER_HEALTH, SleepHealthOverlay::wrapPlayerHealth);
    }
}
