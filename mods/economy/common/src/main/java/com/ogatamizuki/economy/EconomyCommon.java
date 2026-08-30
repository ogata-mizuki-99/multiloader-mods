package com.ogatamizuki.economy;

import com.mojang.logging.LogUtils;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class EconomyCommon {
    public static final String MODID = "economy";
    public static final Logger LOGGER = LogUtils.getLogger();

    /** 環境ダメージ追跡用のダミーUUID (環境からの落下・炎上ダメージ等のロスト用) */
    public static final UUID ENVIRONMENT_UUID = new UUID(0, 0);

    private static int currentBalance;
    private static int currentBankBalance;
    private static int currentDebt;
    private static MinecraftServer minecraftServer;
    private static final Set<UUID> economyReadyUuids = ConcurrentHashMap.newKeySet();

    /** 敵のUUID -> (プレイヤーのUUID -> 与えた累積ダメージ) */
    private static final Map<UUID, Map<UUID, Float>> damageTracker = new ConcurrentHashMap<>();

    private EconomyCommon() {}

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MODID, path);
    }

    public static boolean isEconomyReady(UUID playerUuid) {
        return playerUuid != null && economyReadyUuids.contains(playerUuid);
    }

    /** クライアント側: ローカルプレイヤーの経済データが同期済みか */
    public static boolean isEconomyReady() {
        if (EconomyPlatform.isClient()) {
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
            if (mc.player != null) {
                return isEconomyReady(mc.player.getUUID());
            }
        }
        return false;
    }

    public static void setEconomyReady(UUID playerUuid, boolean ready) {
        if (playerUuid == null) {
            return;
        }
        if (ready) {
            economyReadyUuids.add(playerUuid);
        } else {
            economyReadyUuids.remove(playerUuid);
        }
    }

    public static int getCurrentBalance() {
        return currentBalance;
    }

    public static void setCurrentBalance(int balance) {
        currentBalance = balance;
    }

    public static int getCurrentBankBalance() {
        return currentBankBalance;
    }

    public static void setCurrentBankBalance(int balance) {
        currentBankBalance = balance;
    }

    public static int getCurrentDebt() {
        return currentDebt;
    }

    public static void setCurrentDebt(int debt) {
        currentDebt = debt;
    }

    public static MinecraftServer getServer() {
        if (minecraftServer != null) {
            return minecraftServer;
        }
        return EconomyPlatform.getServerSupplier.get();
    }

    public static void setServer(MinecraftServer server) {
        minecraftServer = server;
    }

    public static Map<UUID, Map<UUID, Float>> damageTracker() {
        return damageTracker;
    }
}
