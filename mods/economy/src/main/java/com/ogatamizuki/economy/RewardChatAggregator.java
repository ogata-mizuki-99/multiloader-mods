package com.ogatamizuki.economy;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import net.neoforged.neoforge.server.ServerLifecycleHooks;

/**
 * 報酬獲得チャットを一定時間まとめて表示し、連続採掘・討伐時のスパムを抑えます。
 * 報酬名は {@link EconomyMasterI18n} の翻訳キーで送り、クライアント言語で表示します。
 */
public final class RewardChatAggregator {
    private static final ConcurrentHashMap<String, Accumulator> ACCUMULATORS = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, ScheduledFuture<?>> SCHEDULED = new ConcurrentHashMap<>();
    private static final ScheduledExecutorService SCHEDULER = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "economy-reward-chat");
        t.setDaemon(true);
        return t;
    });

    private RewardChatAggregator() {}

    private static final class Accumulator {
        int totalAmount;
        int count;
        int latestBalance;
        String actionType;
        String fallbackDisplayName;
        UUID playerUuid;

        void add(int amount, int balance, String actionType, String fallbackName, UUID uuid) {
            totalAmount += amount;
            count++;
            latestBalance = balance;
            this.actionType = actionType;
            fallbackDisplayName = fallbackName;
            playerUuid = uuid;
        }
    }

    public static void notify(
            ServerPlayer player,
            String actionType,
            String fallbackDisplayName,
            int rewardAmount,
            int newBalance
    ) {
        int delaySeconds = EconomyFeatures.rewardChatAggregateSeconds();
        if (delaySeconds <= 0) {
            sendImmediate(player, actionType, fallbackDisplayName, rewardAmount, newBalance, 1);
            return;
        }

        String key = player.getUUID() + ":" + (actionType == null ? "" : actionType);
        ACCUMULATORS.compute(key, (k, acc) -> {
            if (acc == null) {
                acc = new Accumulator();
            }
            acc.add(rewardAmount, newBalance, actionType, fallbackDisplayName, player.getUUID());
            return acc;
        });

        long delayMs = delaySeconds * 1000L;
        SCHEDULED.compute(key, (k, existing) -> {
            if (existing != null) {
                existing.cancel(false);
            }
            return SCHEDULER.schedule(() -> flush(k), delayMs, TimeUnit.MILLISECONDS);
        });
    }

    /** ログアウト時に保留中の通知を即座に送信します。 */
    public static void flushPlayer(UUID playerUuid) {
        List<String> keys = new ArrayList<>();
        String prefix = playerUuid + ":";
        for (String key : ACCUMULATORS.keySet()) {
            if (key.startsWith(prefix)) {
                keys.add(key);
            }
        }
        for (String key : keys) {
            ScheduledFuture<?> future = SCHEDULED.remove(key);
            if (future != null) {
                future.cancel(false);
            }
            flush(key);
        }
    }

    private static void flush(String key) {
        SCHEDULED.remove(key);
        Accumulator acc = ACCUMULATORS.remove(key);
        if (acc == null) {
            return;
        }

        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return;
        }

        server.execute(() -> {
            ServerPlayer player = server.getPlayerList().getPlayer(acc.playerUuid);
            if (player == null) {
                return;
            }
            sendImmediate(
                    player,
                    acc.actionType,
                    acc.fallbackDisplayName,
                    acc.totalAmount,
                    acc.latestBalance,
                    acc.count
            );
        });
    }

    private static void sendImmediate(
            ServerPlayer player,
            String actionType,
            String fallbackDisplayName,
            int totalAmount,
            int newBalance,
            int count
    ) {
        if (count <= 1) {
            player.sendSystemMessage(Component.literal(
                    "economy.chat.reward.gain|economy.reward." + actionType + "|" + totalAmount + "|" + newBalance
            ));
        } else {
            player.sendSystemMessage(Component.literal(
                    "economy.chat.reward.gain_multiple|economy.reward." + actionType + "|" + totalAmount + "|" + count + "|" + newBalance
            ));
        }
    }
}
