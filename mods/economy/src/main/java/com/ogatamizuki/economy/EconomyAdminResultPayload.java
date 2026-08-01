package com.ogatamizuki.economy;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** サーバー → クライアント: 管理ブロック操作結果。 */
public record EconomyAdminResultPayload(boolean success, String message) implements CustomPacketPayload {

    public static final Type<EconomyAdminResultPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(EconomyMod.MODID, "admin_result"));

    public static final StreamCodec<ByteBuf, EconomyAdminResultPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, EconomyAdminResultPayload::success,
            ByteBufCodecs.STRING_UTF8, EconomyAdminResultPayload::message,
            EconomyAdminResultPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
