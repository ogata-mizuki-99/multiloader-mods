package com.ogatamizuki.lookalike;

/**
 * Dedicated 接続中にサーバーから同期されたクライアント向けフラグ。
 * ローカルの lookalike-common.toml より優先する。
 */
public final class LookalikeClientFlags {
    private static boolean hasServerSync;
    private static boolean hideAllNametags = false;

    private LookalikeClientFlags() {
    }

    public static void apply(LookalikeClientFlagsPayload payload) {
        hasServerSync = true;
        hideAllNametags = payload.hideAllNametags();
        LookalikeMod.LOGGER.info(
                "Lookalike client flags synced from server: hideAllNametags={}",
                hideAllNametags);
    }

    public static void clear() {
        hasServerSync = false;
        hideAllNametags = Config.hideAllNametags.get();
    }

    public static boolean hasServerSync() {
        return hasServerSync;
    }

    /** ネームタグ非表示の実効値（マルチ描画用）。 */
    public static boolean hideAllNametags() {
        if (hasServerSync) {
            return hideAllNametags;
        }
        return Config.hideAllNametags.get();
    }
}
