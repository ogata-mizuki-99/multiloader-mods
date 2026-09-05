package com.ogatamizuki.elytraslot.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import com.ogatamizuki.elytraslot.ElytraSlotCommon;

public record SlotPosSyncPayload(
        int elytraX,
        int elytraY,
        int fireworkX,
        int fireworkY
) implements CustomPacketPayload {

    public static final Type<SlotPosSyncPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(ElytraSlotCommon.MODID, "slot_pos_sync"));

    public static final StreamCodec<ByteBuf, SlotPosSyncPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public SlotPosSyncPayload decode(ByteBuf buffer) {
            return new SlotPosSyncPayload(
                    buffer.readInt(),
                    buffer.readInt(),
                    buffer.readInt(),
                    buffer.readInt()
            );
        }

        @Override
        public void encode(ByteBuf buffer, SlotPosSyncPayload value) {
            buffer.writeInt(value.elytraX());
            buffer.writeInt(value.elytraY());
            buffer.writeInt(value.fireworkX());
            buffer.writeInt(value.fireworkY());
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
