package com.ogatamizuki.privatechest.neoforge.client;

import com.ogatamizuki.privatechest.client.LockerAwareSkullRenderer;
import com.ogatamizuki.privatechest.client.LockerBlockEntityRenderer;

import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

public final class PrivateChestClient {
    private PrivateChestClient() {
    }

    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(BlockEntityType.SKULL, LockerAwareSkullRenderer::new);
        event.registerBlockEntityRenderer(com.ogatamizuki.privatechest.PrivateChestCommon.LOCKER_BLOCK_ENTITY_TYPE.get(), LockerBlockEntityRenderer::new);
    }
}
