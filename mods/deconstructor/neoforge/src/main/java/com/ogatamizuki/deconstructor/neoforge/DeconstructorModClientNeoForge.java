package com.ogatamizuki.deconstructor.neoforge;

import com.ogatamizuki.deconstructor.DeconstructorCommon;
import com.ogatamizuki.deconstructor.neoforge.client.DeconstructorConfigScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

@Mod(value = DeconstructorCommon.MODID, dist = Dist.CLIENT)
public class DeconstructorModClientNeoForge {
    public static final String MODID = DeconstructorCommon.MODID;

    public DeconstructorModClientNeoForge(ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class,
                (modContainer, screen) -> new DeconstructorConfigScreen(screen, modContainer));
    }
}
