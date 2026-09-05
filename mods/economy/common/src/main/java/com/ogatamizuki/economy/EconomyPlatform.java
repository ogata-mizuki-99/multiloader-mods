package com.ogatamizuki.economy;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

public final class EconomyPlatform {
    public static BiConsumer<ServerPlayer, CustomPacketPayload> sendToPlayer = (player, payload) -> {};
    public static Consumer<CustomPacketPayload> sendToAllPlayers = payload -> {};
    /** Fabric: ClientPlayNetworking::send / NeoForge: ClientPacketListener#send */
    public static Consumer<CustomPacketPayload> sendToServer = payload -> {};
    public static Supplier<MinecraftServer> getServerSupplier = () -> null;
    public static Supplier<Boolean> isClientSupplier = () -> false;
    public static Predicate<String> isModLoadedCheck = modId -> false;
    public static Runnable persistRuntimeConfig = () -> {};
    public static Runnable registerEntityRenderers = () -> {};
    /** NeoForge: {@code Entity#getPersistentData()}. Fabric: empty (NPC は entityTags / CustomName で識別). */
    public static Function<Entity, net.minecraft.nbt.CompoundTag> getEntityPersistentData = entity -> new net.minecraft.nbt.CompoundTag();

    private EconomyPlatform() {}

    public static void send(ServerPlayer player, CustomPacketPayload payload) {
        sendToPlayer.accept(player, payload);
    }

    public static void sendToAll(CustomPacketPayload payload) {
        sendToAllPlayers.accept(payload);
    }

    public static void sendToServer(CustomPacketPayload payload) {
        sendToServer.accept(payload);
    }

    public static MinecraftServer getServer() {
        return getServerSupplier.get();
    }

    public static boolean isClient() {
        return Boolean.TRUE.equals(isClientSupplier.get());
    }

    public static boolean isModLoaded(String modId) {
        return isModLoadedCheck.test(modId);
    }

    public static void runOnServerThread(Runnable action) {
        MinecraftServer server = getServer();
        if (server != null) {
            server.execute(action);
        } else {
            EconomyCommon.LOGGER.warn("MinecraftServer not available; running task on current thread.");
            action.run();
        }
    }
}
