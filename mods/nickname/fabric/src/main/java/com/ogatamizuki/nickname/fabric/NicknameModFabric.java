package com.ogatamizuki.nickname.fabric;

import com.ogatamizuki.nickname.*;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class NicknameModFabric implements ModInitializer {
    public static final String MODID = "nickname";
    public static final Logger LOGGER = LogManager.getLogger(NicknameModFabric.class);
    private static MinecraftServer currentServer;

    public static MinecraftServer getServer() {
        return currentServer;
    }

    @Override
    public void onInitialize() {
        LOGGER.info("Nickname Mod (Fabric) Initializing...");
        NicknamePlatform.refreshDisplayNames = player -> {};

        // Register Payloads
        PayloadTypeRegistry.clientboundPlay().register(NicknameSyncPayload.TYPE, NicknameSyncPayload.STREAM_CODEC);
        PayloadTypeRegistry.clientboundPlay().register(NicknameClearAllPayload.TYPE, NicknameClearAllPayload.STREAM_CODEC);

        // Server Lifecycle Events
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            currentServer = server;
            NicknameStorage.load(server);
            LOGGER.info("Loaded nicknames data: {} entries", NicknameStorage.getNicknames().size());
        });

        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            NicknameStorage.save(server);
            LOGGER.info("Saved nicknames data.");
            currentServer = null;
        });

        // Player Connect / Sync
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayer player = handler.getPlayer();
            // ログイン時に全オンラインプレイヤーのニックネームを送信
            for (ServerPlayer online : server.getPlayerList().getPlayers()) {
                String nick = NicknameStorage.getNickname(online.getUUID());
                if (nick != null && !nick.isEmpty()) {
                    ServerPlayNetworking.send(player, new NicknameSyncPayload(online.getUUID(), nick));
                }
            }
        });

        // Command Registration
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            NicknameCommands.register(
                    dispatcher,
                    (uuid, newName) -> {
                        if (currentServer != null) {
                            NicknameSyncPayload payload = new NicknameSyncPayload(uuid, newName);
                            for (ServerPlayer player : PlayerLookup.all(currentServer)) {
                                ServerPlayNetworking.send(player, payload);
                            }
                        }
                    },
                    () -> {
                        if (currentServer != null) {
                            NicknameClearAllPayload payload = new NicknameClearAllPayload();
                            for (ServerPlayer player : PlayerLookup.all(currentServer)) {
                                ServerPlayNetworking.send(player, payload);
                            }
                        }
                    }
            );
        });
    }
}
