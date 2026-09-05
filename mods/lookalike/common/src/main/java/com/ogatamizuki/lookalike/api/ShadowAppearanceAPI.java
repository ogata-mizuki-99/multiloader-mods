package com.ogatamizuki.lookalike.api;

import com.ogatamizuki.lookalike.NetworkPayloads;
import com.ogatamizuki.lookalike.ShadowAppearanceManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.UUID;

/**
 * 正体不明の「謎の影」見た目を適用する汎用 API。
 * スキン・頭上ネームタグを暗色シルエットに差し替え、クライアントで煙パーティクルを描画します。
 */
public final class ShadowAppearanceAPI {
    private ShadowAppearanceAPI() {
    }

    public static void enableShadow(ServerPlayer player) {
        ShadowAppearanceManager.getInstance().enableShadow(player);
    }

    public static void enableShadow(ServerPlayer player, int durationSeconds) {
        ShadowAppearanceManager.getInstance().enableShadow(player, durationSeconds);
    }

    public static void disableShadow(ServerPlayer player) {
        ShadowAppearanceManager.getInstance().disableShadow(player);
    }

    public static void disableAllShadows(MinecraftServer server) {
        ShadowAppearanceManager.getInstance().disableAllShadows(server);
    }

    public static boolean isShadow(UUID playerUuid) {
        return ShadowAppearanceManager.getInstance().isShadow(playerUuid);
    }

    public static void setPathVisualization(
            MinecraftServer server,
            List<NetworkPayloads.ShadowPathEntry> paths,
            boolean enabled
    ) {
        ShadowAppearanceManager.getInstance().setPathVisualization(server, paths, enabled);
    }

    public static void syncAppearanceTo(ServerPlayer player) {
        ShadowAppearanceManager.getInstance().syncAppearanceTo(player);
    }
}
