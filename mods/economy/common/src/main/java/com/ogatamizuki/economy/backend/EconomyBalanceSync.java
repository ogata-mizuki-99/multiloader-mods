package com.ogatamizuki.economy.backend;

import com.ogatamizuki.economy.EconomyCommon;
import com.ogatamizuki.economy.EconomyPlatform;
import com.ogatamizuki.economy.PlayerBalanceSyncPayload;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/** 残高のクライアント表示キャッシュ更新とクライアント HUD 同期。 */
public final class EconomyBalanceSync {
    private EconomyBalanceSync() {
    }

    public static void syncBalanceToClient(ServerPlayer player, int balance, int bankBalance, int debt) {
        if (player == null) {
            return;
        }
        EconomyPlatform.send(player, new PlayerBalanceSyncPayload(balance, bankBalance, debt));
    }

    /**
     * ローカルプレイヤーの表示用キャッシュのみ更新する。
     * Dedicated / 他プレイヤー向けには呼ばない（LAN 統合時の HUD 混線防止）。
     */
    public static void applyLocalDisplayCache(int balance, int bankBalance, int debt) {
        if (!EconomyPlatform.isClient()) {
            return;
        }
        net.minecraft.client.Minecraft.getInstance().execute(() -> {
            EconomyCommon.setCurrentBalance(balance);
            EconomyCommon.setCurrentBankBalance(bankBalance);
            EconomyCommon.setCurrentDebt(debt);
        });
    }

    public static void applyBalanceAndSync(Player player, int balance, int bankBalance, int debt) {
        if (player != null && isLocalClientPlayer(player)) {
            applyLocalDisplayCache(balance, bankBalance, debt);
        }
        if (player instanceof ServerPlayer serverPlayer) {
            syncBalanceToClient(serverPlayer, balance, bankBalance, debt);
        }
    }

    private static boolean isLocalClientPlayer(Player player) {
        if (!EconomyPlatform.isClient()) {
            return false;
        }
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        return mc.player != null && mc.player.getUUID().equals(player.getUUID());
    }
}
