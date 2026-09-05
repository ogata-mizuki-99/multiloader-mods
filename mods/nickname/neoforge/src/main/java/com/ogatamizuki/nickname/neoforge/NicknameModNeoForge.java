package com.ogatamizuki.nickname.neoforge;

import com.ogatamizuki.nickname.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(NicknameModNeoForge.MODID)
public class NicknameModNeoForge {
    public static final String MODID = "nickname";
    public static final Logger LOGGER = LogManager.getLogger(NicknameModNeoForge.class);

    public NicknameModNeoForge(IEventBus modEventBus, ModContainer modContainer) {
        LOGGER.info("Nickname Mod (NeoForge) Initializing...");
        NicknamePlatform.refreshDisplayNames = player -> {
            player.refreshDisplayName();
            player.refreshTabListName();
        };

        modEventBus.addListener(this::registerPayloads);

        NeoForge.EVENT_BUS.register(this);
    }

    private void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");

        // サーバー → クライアントへの同期パケット
        registrar.playToClient(
                NicknameSyncPayload.TYPE,
                NicknameSyncPayload.STREAM_CODEC,
                (payload, context) -> {
                    context.enqueueWork(() -> {
                        NicknameStorage.setNickname(payload.playerUuid(), payload.nickname());

                        // 表示名をキャッシュから再構成
                        net.minecraft.world.entity.player.Player targetPlayer = context.player().level()
                                .getPlayerByUUID(payload.playerUuid());
                        if (targetPlayer != null) {
                            targetPlayer.refreshDisplayName();
                        }
                    });
                });

        registrar.playToClient(
                NicknameClearAllPayload.TYPE,
                NicknameClearAllPayload.STREAM_CODEC,
                (payload, context) -> {
                    context.enqueueWork(() -> {
                        NicknameStorage.clear();
                        for (net.minecraft.world.entity.player.Player targetPlayer : context.player().level().players()) {
                            targetPlayer.refreshDisplayName();
                        }
                    });
                });
    }

    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) {
        NicknameStorage.load(event.getServer());
        LOGGER.info("Loaded nicknames data: {} entries", NicknameStorage.getNicknames().size());
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        NicknameStorage.save(event.getServer());
        LOGGER.info("Saved nicknames data.");
    }

    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        MinecraftServer server = player.level().getServer();
        if (server == null) {
            return;
        }

        // ログイン直後に Tab 上の他プレイヤー表示名（radial-teleport 等）を安定させる
        for (ServerPlayer online : server.getPlayerList().getPlayers()) {
            String nick = NicknameStorage.getNickname(online.getUUID());
            if (nick != null && !nick.isEmpty()) {
                PacketDistributor.sendToPlayer(player, new NicknameSyncPayload(online.getUUID(), nick));
            }
        }
    }

    @SubscribeEvent
    public void onStartTracking(PlayerEvent.StartTracking event) {
        // 他のプレイヤーが描画範囲に入った（トラッキング開始した）際、そのプレイヤーのニックネームを同期
        if (event.getTarget() instanceof ServerPlayer targetPlayer && event.getEntity() instanceof ServerPlayer tracker) {
            String nick = NicknameStorage.getNickname(targetPlayer.getUUID());
            if (nick != null && !nick.isEmpty()) {
                PacketDistributor.sendToPlayer(tracker, new NicknameSyncPayload(targetPlayer.getUUID(), nick));
            }
        }
    }

    @SubscribeEvent
    public void onPlayerNameFormat(PlayerEvent.NameFormat event) {
        String nick = NicknameStorage.getNickname(event.getEntity().getUUID());
        if (nick != null && !nick.isEmpty()) {
            event.setDisplayname(Component.literal(nick));
        }
    }

    @SubscribeEvent
    public void onTabListNameFormat(PlayerEvent.TabListNameFormat event) {
        String nick = NicknameStorage.getNickname(event.getEntity().getUUID());
        if (nick != null && !nick.isEmpty()) {
            event.setDisplayName(Component.literal(nick));
        }
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        NicknameCommands.register(
                event.getDispatcher(),
                (uuid, newName) -> PacketDistributor.sendToAllPlayers(new NicknameSyncPayload(uuid, newName)),
                () -> PacketDistributor.sendToAllPlayers(new NicknameClearAllPayload())
        );
    }
}
