package com.ogatamizuki.privatechest.client;

import net.minecraft.world.level.block.entity.BlockEntityTypes;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

public final class PrivateChestClient {
    private PrivateChestClient() {
    }

    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(BlockEntityTypes.SKULL, LockerAwareSkullRenderer::new);
        event.registerBlockEntityRenderer(com.ogatamizuki.privatechest.PrivateChestMod.LOCKER_BLOCK_ENTITY_TYPE.get(),
                LockerBlockEntityRenderer::new);
    }
}
