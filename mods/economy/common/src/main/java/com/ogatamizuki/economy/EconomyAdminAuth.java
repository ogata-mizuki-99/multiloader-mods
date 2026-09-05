package com.ogatamizuki.economy;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.entity.player.Player;

/**
 * 経済管理操作の権限判定。
 * <ul>
 *   <li>統合サーバーのホスト（singleplayer owner）: 常に可（LAN 公開後もホストのみ）</li>
 *   <li>それ以外（LAN ゲスト / Dedicated）: OP / GameMaster 権限必須</li>
 * </ul>
 */
public final class EconomyAdminAuth {
    private EconomyAdminAuth() {
    }

    public static boolean canPerformAdminActions(Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return false;
        }
        MinecraftServer server = serverPlayer.level().getServer();
        if (server == null) {
            return false;
        }
        if (server.isSingleplayerOwner(serverPlayer.nameAndId())) {
            return true;
        }
        return hasGameMasterPermission(serverPlayer);
    }

    public static boolean canPerformAdminActionsClient() {
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        // 統合クライアントのローカルプレイヤーは常にホスト。ゲスト接続時は hasSingleplayerServer()=false。
        if (mc.hasSingleplayerServer()) {
            return true;
        }
        if (mc.player != null) {
            return mc.player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER);
        }
        return false;
    }

    private static boolean hasGameMasterPermission(ServerPlayer serverPlayer) {
        return serverPlayer.createCommandSourceStack().permissions()
                .hasPermission(Permissions.COMMANDS_GAMEMASTER);
    }
}
