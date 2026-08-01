package com.ogatamizuki.economy;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** クライアント → サーバー: マスタ設定の保存・同梱版への復帰。 */
public record EconomyMasterConfigPayload(
        String action,
        double deathPenaltyRate,
        double shortSellLimitRate,
        int etfIntervalMinutes,
        int loanMaxAmount,
        double loanAssetMultiplier
) implements CustomPacketPayload {

    public static final Type<EconomyMasterConfigPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(EconomyMod.MODID, "master_config"));

    public static final StreamCodec<ByteBuf, EconomyMasterConfigPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, EconomyMasterConfigPayload::action,
            ByteBufCodecs.DOUBLE, EconomyMasterConfigPayload::deathPenaltyRate,
            ByteBufCodecs.DOUBLE, EconomyMasterConfigPayload::shortSellLimitRate,
            ByteBufCodecs.VAR_INT, EconomyMasterConfigPayload::etfIntervalMinutes,
            ByteBufCodecs.VAR_INT, EconomyMasterConfigPayload::loanMaxAmount,
            ByteBufCodecs.DOUBLE, EconomyMasterConfigPayload::loanAssetMultiplier,
            EconomyMasterConfigPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
