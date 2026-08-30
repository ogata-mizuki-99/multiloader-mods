package com.ogatamizuki.lookalike;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.function.BiConsumer;
import java.util.function.Supplier;

public final class LookalikeCommon {
    public static final String MODID = "lookalike";
    public static final Logger LOGGER = LogManager.getLogger(LookalikeCommon.class);

    public static Supplier<DisguiseMirrorItem> DISGUISE_MIRROR;

    public static BiConsumer<ServerPlayer, CustomPacketPayload> sendToPlayer = (player, payload) -> {};
    public static Consumer2<ServerPlayer, CustomPacketPayload> sendToTrackingAndSelf = (player, payload) -> {};
    public static java.util.function.Consumer<CustomPacketPayload> sendToServer = payload -> {};
    public static java.util.function.Consumer<ServerPlayer> refreshPlayerNames = player -> {};

    @FunctionalInterface
    public interface Consumer2<T, U> {
        void accept(T t, U u);
    }

    private LookalikeCommon() {}

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MODID, path);
    }
}
