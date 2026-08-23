package com.ogatamizuki.instantstructure;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * クライアント → サーバー: 設定画面の COMMON 設定を Dedicated / 統合サーバーの toml へ反映。
 */
public record InstantStructureCommonConfigPushPayload(
        boolean enableCraftingRecipe,
        boolean enableMaterialConsumption,
        boolean dropClearedBlocks
) implements CustomPacketPayload {

    public static final Type<InstantStructureCommonConfigPushPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(InstantStructureMod.MODID, "common_config_push"));

    public static final StreamCodec<ByteBuf, InstantStructureCommonConfigPushPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, InstantStructureCommonConfigPushPayload::enableCraftingRecipe,
            ByteBufCodecs.BOOL, InstantStructureCommonConfigPushPayload::enableMaterialConsumption,
            ByteBufCodecs.BOOL, InstantStructureCommonConfigPushPayload::dropClearedBlocks,
            InstantStructureCommonConfigPushPayload::new);

    public static InstantStructureCommonConfigPushPayload fromLocalConfig() {
        return new InstantStructureCommonConfigPushPayload(
                Config.ENABLE_CRAFTING_RECIPE.get(),
                Config.ENABLE_MATERIAL_CONSUMPTION.get(),
                Config.DROP_CLEARED_BLOCKS.get()
        );
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
