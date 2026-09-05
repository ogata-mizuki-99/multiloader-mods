package com.ogatamizuki.radialteleport;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * クライアント → サーバー: Mods Config 画面の COMMON 設定を Dedicated / 統合サーバーの toml へ反映。
 */
public record RadialTeleportCommonConfigPushPayload(
        boolean enableCraftingRecipe,
        boolean enableWaypoints,
        int maxWaypointsPerPlayer,
        int teleportCooldownTicks
) implements CustomPacketPayload {

    public static final Type<RadialTeleportCommonConfigPushPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(RadialTeleportCommon.MODID, "common_config_push"));

    public static final StreamCodec<ByteBuf, RadialTeleportCommonConfigPushPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, RadialTeleportCommonConfigPushPayload::enableCraftingRecipe,
            ByteBufCodecs.BOOL, RadialTeleportCommonConfigPushPayload::enableWaypoints,
            ByteBufCodecs.VAR_INT, RadialTeleportCommonConfigPushPayload::maxWaypointsPerPlayer,
            ByteBufCodecs.VAR_INT, RadialTeleportCommonConfigPushPayload::teleportCooldownTicks,
            RadialTeleportCommonConfigPushPayload::new);

    public static RadialTeleportCommonConfigPushPayload fromLocalConfig() {
        return new RadialTeleportCommonConfigPushPayload(
                Config.isEnableCraftingRecipe(),
                Config.isEnableWaypoints(),
                Config.getMaxWaypointsPerPlayer(),
                Config.getTeleportCooldownTicks()
        );
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
