package com.ogatamizuki.instantstructure;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

import java.nio.file.Path;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

public final class InstantStructurePlatform {
    public static BiConsumer<ServerPlayer, CustomPacketPayload> sendToPlayer;
    public static java.util.function.Consumer<CustomPacketPayload> sendToServer;
    public static Supplier<Path> getConfigDir;
    public static java.util.function.Predicate<String> isModLoadedCheck = modId -> false;

    private InstantStructurePlatform() {}

    public static boolean isModLoaded(String modId) {
        return isModLoadedCheck.test(modId);
    }

    public static void send(ServerPlayer player, CustomPacketPayload payload) {
        if (sendToPlayer != null) {
            sendToPlayer.accept(player, payload);
        }
    }

    public static Path getConfigDir() {
        if (getConfigDir != null) {
            return getConfigDir.get();
        }
        return Path.of("config");
    }
}
