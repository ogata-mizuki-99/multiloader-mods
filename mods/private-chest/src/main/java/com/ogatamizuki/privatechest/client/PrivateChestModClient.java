package com.ogatamizuki.privatechest.client;

import com.ogatamizuki.privatechest.PrivateChestMod;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

@Mod(value = PrivateChestMod.MODID, dist = Dist.CLIENT)
public class PrivateChestModClient {
    public PrivateChestModClient(ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, PrivateChestConfigScreen::new);
    }
}
