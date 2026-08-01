package com.ogatamizuki.guide;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class GuideLibDataComponents {
    public static final DeferredRegister.DataComponents REGISTRAR =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, GuideLibMod.MODID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<GuideAccess>> GUIDE_ACCESS =
            REGISTRAR.registerComponentType(
                    "guide_access",
                    builder -> builder
                            .persistent(GuideAccess.CODEC)
                            .networkSynchronized(ByteBufCodecs.fromCodec(GuideAccess.CODEC))
            );

    private GuideLibDataComponents() {}
}
