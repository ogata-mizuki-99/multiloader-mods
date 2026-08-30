package com.ogatamizuki.elytraslot;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import java.util.UUID;

public record FireworkSlotSyncPayload(
        UUID playerUuid,
        ItemStack fireworkItem
) implements CustomPacketPayload {

    public static final Type<FireworkSlotSyncPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(ElytraSlotCommon.MODID, "firework_slot_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, FireworkSlotSyncPayload> STREAM_CODEC = StreamCodec.composite(
            UUIDStreamCodec(), FireworkSlotSyncPayload::playerUuid,
            ItemStack.OPTIONAL_STREAM_CODEC, FireworkSlotSyncPayload::fireworkItem,
            FireworkSlotSyncPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static StreamCodec<ByteBuf, UUID> UUIDStreamCodec() {
        return new StreamCodec<>() {
            @Override
            public UUID decode(ByteBuf buffer) {
                long most = buffer.readLong();
                long least = buffer.readLong();
                return new UUID(most, least);
            }

            @Override
            public void encode(ByteBuf buffer, UUID value) {
                buffer.writeLong(value.getMostSignificantBits());
                buffer.writeLong(value.getLeastSignificantBits());
            }
        };
    }
}
