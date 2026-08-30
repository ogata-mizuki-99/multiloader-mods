package com.ogatamizuki.sleep.fabric;

import com.ogatamizuki.sleep.SleepClientFlagsPayload;
import com.ogatamizuki.sleep.SleepCommon;
import com.ogatamizuki.sleep.SleepCommonConfigPushPayload;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.EntitySleepEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.gamerules.GameRules;

public class SleepModFabric implements ModInitializer {
    private static final int VANILLA_SLEEPING_PERCENTAGE = 100;

    @Override
    public void onInitialize() {
        SleepCommon.LOGGER.info("Good Sleep Mod (Fabric) Initializing...");

        // パケット登録
        PayloadTypeRegistry.clientboundPlay().register(SleepClientFlagsPayload.TYPE, SleepClientFlagsPayload.STREAM_CODEC);
        PayloadTypeRegistry.serverboundPlay().register(SleepCommonConfigPushPayload.TYPE, SleepCommonConfigPushPayload.STREAM_CODEC);

        // サーバー側パケット受信ハンドラ
        ServerPlayNetworking.registerGlobalReceiver(SleepCommonConfigPushPayload.TYPE, (payload, context) -> {
            context.server().execute(() -> {
                ServerPlayer player = context.player();
                if (!player.createCommandSourceStack().permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)) {
                    player.sendSystemMessage(Component.translatable("good_sleep.configuration.push_denied").withStyle(ChatFormatting.RED));
                    return;
                }

                SleepCommon.allowDaySleep = payload.allowDaySleep();
                SleepCommon.healWhileSleeping = payload.healWhileSleeping();
                SleepCommon.healIntervalTicks = Math.max(0, Math.min(200, payload.healIntervalTicks()));
                SleepCommon.onePlayerSkip = payload.onePlayerSkip();

                applySleepingPercentage(context.server());
                syncClientFlagsToAllPlayers(context.server());

                player.sendSystemMessage(Component.translatable("good_sleep.configuration.push_ok").withStyle(ChatFormatting.GREEN));
            });
        });

        // 昼間睡眠: 昼間のベッド睡眠制限を解除 (BedRule が寝られない環境判定のとき null を返して睡眠許可)
        EntitySleepEvents.ALLOW_SLEEPING.register((player, sleepingPos) -> {
            if (!SleepCommon.allowDaySleep) {
                return null;
            }
            return null;
        });

        // プレイヤー参加時の同期
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayNetworking.send(handler.player, SleepClientFlagsPayload.fromConfig());
        });

        // サーバー起動時
        ServerLifecycleEvents.SERVER_STARTED.register(SleepModFabric::applySleepingPercentage);

        // プレイヤーTick（睡眠時回復）
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (!SleepCommon.healWhileSleeping) {
                return;
            }
            int interval = SleepCommon.healIntervalTicks;
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                if (!player.isSleeping()) {
                    continue;
                }
                if (interval == 0) {
                    if (player.getHealth() < player.getMaxHealth()) {
                        player.setHealth(player.getMaxHealth());
                    }
                    continue;
                }
                if (player.isSleepingLongEnough() && player.tickCount % interval == 0) {
                    if (player.getHealth() < player.getMaxHealth()) {
                        player.heal(1.0F);
                    }
                }
            }
        });
    }

    public static void syncClientFlagsToAllPlayers(MinecraftServer server) {
        SleepClientFlagsPayload payload = SleepClientFlagsPayload.fromConfig();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            ServerPlayNetworking.send(player, payload);
        }
    }

    private static void applySleepingPercentage(MinecraftServer server) {
        int percentage = SleepCommon.onePlayerSkip ? 0 : VANILLA_SLEEPING_PERCENTAGE;
        server.getGameRules().set(GameRules.PLAYERS_SLEEPING_PERCENTAGE, percentage, server);
        SleepCommon.LOGGER.info("playersSleepingPercentage set to {}", percentage);
    }
}
