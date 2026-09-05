package com.ogatamizuki.radialteleport;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;

public record TeleportResultPayload(boolean success, String messageKey, List<String> messageArgs) implements CustomPacketPayload {

    public static final Type<TeleportResultPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(RadialTeleportCommon.MODID, "teleport_result"));

    public static final StreamCodec<ByteBuf, TeleportResultPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, TeleportResultPayload::success,
            ByteBufCodecs.STRING_UTF8, TeleportResultPayload::messageKey,
            ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), TeleportResultPayload::messageArgs,
            TeleportResultPayload::new
    );

    public static TeleportResultPayload message(boolean success, String messageKey, Object... args) {
        List<String> stringArgs = new ArrayList<>(args.length);
        for (Object arg : args) {
            stringArgs.add(String.valueOf(arg));
        }
        return new TeleportResultPayload(success, messageKey, stringArgs);
    }

    public Component toComponent() {
        if (messageArgs.isEmpty()) {
            return Component.translatable(messageKey);
        }
        return Component.translatable(messageKey, messageArgs.toArray());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
