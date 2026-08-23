package com.ogatamizuki.lookalike;

import com.ogatamizuki.lookalike.mixin.AvatarAccessor;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

final class ModelCustomizationHelper {
    /** All PlayerModelPart flags enabled (hat, jacket, sleeves, pants, cape). */
    static final byte ALL_PARTS = (byte) 0x7F;

    private ModelCustomizationHelper() {
    }

    static byte get(ServerPlayer player) {
        return player.getEntityData().get(AvatarAccessor.lookalike$modelCustomizationId());
    }

    static void set(ServerPlayer player, byte customization) {
        player.getEntityData().set(AvatarAccessor.lookalike$modelCustomizationId(), customization);
    }

    static ClientboundSetEntityDataPacket createSyncPacket(ServerPlayer player) {
        return new ClientboundSetEntityDataPacket(
                player.getId(),
                List.of(SynchedEntityData.DataValue.create(
                        AvatarAccessor.lookalike$modelCustomizationId(),
                        get(player)
                ))
        );
    }
}
