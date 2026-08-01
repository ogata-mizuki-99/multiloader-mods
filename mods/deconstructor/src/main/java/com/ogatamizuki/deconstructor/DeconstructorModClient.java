package com.ogatamizuki.deconstructor;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import com.ogatamizuki.deconstructor.client.DeconstructorConfigScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

@Mod(value = DeconstructorMod.MODID, dist = Dist.CLIENT)
public class DeconstructorModClient {
    public DeconstructorModClient(ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, DeconstructorConfigScreen::new);
    }
}
