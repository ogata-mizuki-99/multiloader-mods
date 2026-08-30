package com.ogatamizuki.privatechest.neoforge.client;

import com.ogatamizuki.privatechest.LockerScreen;
import com.ogatamizuki.privatechest.neoforge.PrivateChestModNeoForge;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

@Mod(value = PrivateChestModNeoForge.MODID, dist = Dist.CLIENT)
public class PrivateChestModNeoForgeClient {
    public PrivateChestModNeoForgeClient(ModContainer container) {
        IEventBus modEventBus = container.getEventBus();
        modEventBus.addListener(this::registerScreens);
        modEventBus.addListener(PrivateChestClient::registerRenderers);
        container.registerExtensionPoint(IConfigScreenFactory.class, PrivateChestConfigScreen::new);
    }

    private void registerScreens(RegisterMenuScreensEvent event) {
        event.register(PrivateChestModNeoForge.LOCKER_MENU_TYPE.get(), LockerScreen::new);
    }
}
