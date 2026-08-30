package com.ogatamizuki.elytraslot.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import com.ogatamizuki.elytraslot.ElytraSlotCommon;

public record ActionPayload(
        int actionId
) implements CustomPacketPayload {

    public static final int ACTION_SWAP_ELYTRA = 1;
    public static final int ACTION_FIREWORK_BOOST = 2;

    public static final Type<ActionPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(ElytraSlotCommon.MODID, "action_payload"));

    public static final StreamCodec<ByteBuf, ActionPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public ActionPayload decode(ByteBuf buffer) {
            return new ActionPayload(buffer.readInt());
        }

        @Override
        public void encode(ByteBuf buffer, ActionPayload value) {
            buffer.writeInt(value.actionId());
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
