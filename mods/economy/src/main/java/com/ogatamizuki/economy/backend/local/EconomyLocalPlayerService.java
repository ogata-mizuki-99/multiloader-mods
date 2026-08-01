package com.ogatamizuki.economy.backend.local;

import java.util.UUID;

import com.google.gson.JsonObject;
import com.ogatamizuki.economy.EconomyMasterI18n;
import com.ogatamizuki.economy.EconomyMod;
import com.ogatamizuki.economy.RewardChatAggregator;
import com.ogatamizuki.economy.backend.EconomyBalanceSync;
import com.ogatamizuki.economy.data.EconomyWorldSavedData;
import com.ogatamizuki.economy.master.EconomyMasterData;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/** プレイヤー残高・銀行・報酬・死亡のローカル処理。 */
public final class EconomyLocalPlayerService {
    private EconomyLocalPlayerService() {
    }

    public static void join(ServerPlayer player) {
        UUID uuid = player.getUUID();
        EconomyWorldSavedData data = EconomyWorldSavedData.get(player.level().getServer());
        EconomyWorldSavedData.PlayerRecord record = data.getOrCreate(uuid, player.getName().getString());
        EconomyBalanceSync.applyBalanceAndSync(player, record.balance(), record.bankBalance(), record.debt());
        EconomyMod.setEconomyReady(uuid, true);
    }

    public static void leave(UUID playerUuid) {
        EconomyMod.setEconomyReady(playerUuid, false);
    }

    public static boolean deposit(UUID playerUuid, int amount) {
        if (amount <= 0) {
            return false;
        }
        EconomyWorldSavedData data = requireData(playerUuid);
        if (data == null) {
            return false;
        }
        EconomyWorldSavedData.PlayerRecord current = data.getOrCreate(playerUuid, "");
        if (current.balance() < amount) {
            return false;
        }
        EconomyWorldSavedData.PlayerRecord updated = new EconomyWorldSavedData.PlayerRecord(
                current.username(),
                current.balance() - amount,
                current.bankBalance() + amount,
                current.debt(),
                current.totalEarnings(),
                current.totalLost(),
                current.etfBuyAmount(),
                current.etfShortAmount(),
                current.etfProfitAmount(),
                current.totalTradeCount()
        );
        data.putPlayer(playerUuid, updated);
        applyState(playerUuid, updated);
        return true;
    }

    public static boolean withdraw(UUID playerUuid, int amount) {
        if (amount <= 0) {
            return false;
        }
        EconomyWorldSavedData data = requireData(playerUuid);
        if (data == null) {
            return false;
        }
        EconomyWorldSavedData.PlayerRecord current = data.getOrCreate(playerUuid, "");
        if (current.bankBalance() < amount) {
            return false;
        }
        EconomyWorldSavedData.PlayerRecord updated = new EconomyWorldSavedData.PlayerRecord(
                current.username(),
                current.balance() + amount,
                current.bankBalance() - amount,
                current.debt(),
                current.totalEarnings(),
                current.totalLost(),
                current.etfBuyAmount(),
                current.etfShortAmount(),
                current.etfProfitAmount(),
                current.totalTradeCount()
        );
        data.putPlayer(playerUuid, updated);
        applyState(playerUuid, updated);
        return true;
    }

    public static void reward(Player player, String actionType, double ratio) {
        EconomyMasterData master = EconomyMasterData.get();
        var rewardOpt = master.actionReward(actionType);
        if (rewardOpt.isEmpty()) {
            EconomyMod.LOGGER.warn("Unknown or disabled action reward: {}", actionType);
            return;
        }
        EconomyMasterData.ActionRewardDef reward = rewardOpt.get();
        double multiplier = ratio > 0 ? ratio : 1.0;
        int finalAmount = multiplier > 0 ? Math.max(1, (int) Math.round(reward.rewardAmount() * multiplier)) : 0;
        if (finalAmount <= 0) {
            return;
        }

        UUID uuid = player.getUUID();
        EconomyWorldSavedData data = requireData(uuid);
        if (data == null) {
            return;
        }
        EconomyWorldSavedData.PlayerRecord current = data.getOrCreate(uuid, player.getName().getString());
        EconomyWorldSavedData.PlayerRecord updated = new EconomyWorldSavedData.PlayerRecord(
                current.username(),
                current.balance() + finalAmount,
                current.bankBalance(),
                current.debt(),
                current.totalEarnings() + finalAmount,
                current.totalLost(),
                current.etfBuyAmount(),
                current.etfShortAmount(),
                current.etfProfitAmount(),
                current.totalTradeCount()
        );
        data.putPlayer(uuid, updated);
        EconomyBalanceSync.applyBalanceAndSync(player, updated.balance(), updated.bankBalance(), updated.debt());

        if (player instanceof ServerPlayer serverPlayer) {
            RewardChatAggregator.notify(
                    serverPlayer,
                    reward.actionType(),
                    reward.displayName(),
                    finalAmount,
                    updated.balance()
            );
        } else {
            player.sendSystemMessage(Component.translatable(
                    "economy.chat.reward",
                    EconomyMasterI18n.rewardNameComponent(reward.actionType(), reward.displayName()),
                    String.valueOf(finalAmount),
                    String.valueOf(updated.balance())
            ));
        }
    }

    public static void death(Player player) {
        UUID uuid = player.getUUID();
        EconomyWorldSavedData data = requireData(uuid);
        if (data == null) {
            return;
        }
        EconomyWorldSavedData.PlayerRecord current = data.getOrCreate(uuid, player.getName().getString());
        double rate = com.ogatamizuki.economy.master.EconomyMasterData.get().deathPenaltyRate();
        int penaltyBase = current.balance() + (int) Math.floor(current.bankBalance() * 0.05);
        int lostAmount = Math.max(100, (int) Math.floor(penaltyBase * rate));
        int actualLost = Math.min(lostAmount, current.balance());
        if (actualLost <= 0) {
            return;
        }

        EconomyWorldSavedData.PlayerRecord updated = new EconomyWorldSavedData.PlayerRecord(
                current.username(),
                current.balance() - actualLost,
                current.bankBalance(),
                current.debt(),
                current.totalEarnings(),
                current.totalLost() + actualLost,
                current.etfBuyAmount(),
                current.etfShortAmount(),
                current.etfProfitAmount(),
                current.totalTradeCount()
        );
        data.putPlayer(uuid, updated);
        EconomyBalanceSync.applyBalanceAndSync(player, updated.balance(), updated.bankBalance(), updated.debt());
        player.sendSystemMessage(Component.literal(
                "§c[経済] §fデスペナルティにより §e¥" + actualLost + "§f を失いました！ (残高: §e¥" + updated.balance() + "§f)"));
    }

    public static JsonObject playerJson(UUID playerUuid) {
        EconomyWorldSavedData data = requireData(playerUuid);
        if (data == null) {
            return new JsonObject();
        }
        EconomyWorldSavedData.PlayerRecord record = data.getOrCreate(playerUuid, "");
        JsonObject json = new JsonObject();
        json.addProperty("balance", record.balance());
        json.addProperty("bankBalance", record.bankBalance());
        json.addProperty("debt", record.debt());
        return json;
    }

    private static EconomyWorldSavedData requireData(UUID playerUuid) {
        EconomyWorldSavedData data = worldData();
        if (data == null) {
            EconomyMod.LOGGER.warn("Economy server not available for player {}", playerUuid);
        }
        return data;
    }

    public static EconomyWorldSavedData worldData() {
        MinecraftServer server = server();
        if (server == null) {
            return null;
        }
        return EconomyWorldSavedData.get(server);
    }

    public static MinecraftServer server() {
        var s = EconomyMod.getServer();
        if (s == null) {
            s = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
        }
        return s;
    }

    private static void applyState(UUID playerUuid, EconomyWorldSavedData.PlayerRecord record) {
        MinecraftServer server = server();
        if (server == null) {
            return;
        }
        ServerPlayer player = server.getPlayerList().getPlayer(playerUuid);
        if (player != null) {
            EconomyBalanceSync.applyBalanceAndSync(player, record.balance(), record.bankBalance(), record.debt());
        }
    }
}
