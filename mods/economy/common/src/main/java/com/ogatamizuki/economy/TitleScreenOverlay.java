package com.ogatamizuki.economy;

import net.neoforged.neoforge.client.event.ScreenEvent;

/**
 * タイトル画面への画像オーバーレイ＋SE再生（将来再利用用ナレッジ）。
 *
 * <p>再有効化手順:
 * <ol>
 *   <li>{@link EconomyMod#TITLE_TRANSITION_SE} の SoundEvent 登録を有効化</li>
 *   <li>{@code assets/economy/sounds.json} に SE エントリを追加</li>
 *   <li>{@code assets/economy/sounds/se/<name>.ogg} を配置</li>
 *   <li>{@code assets/economy/textures/title/<image>.png} を配置</li>
 *   <li>{@link EconomyModClient} で NeoForge イベント登録のコメントを解除</li>
 *   <li>本クラスの {@link #ENABLED} を {@code true} に変更</li>
 * </ol>
 */
public final class TitleScreenOverlay {
    /** タイトル演出を有効にする場合は true。資産未配置時は false のまま。 */
    //private static final boolean ENABLED = false;

    /*
     * --- 参照実装（pose_dokkiri_daiseikou.png + tettetere.ogg で使用していたコード） ---
     *
     * private static final Identifier POSE_TEXTURE = Identifier.fromNamespaceAndPath(
     *         EconomyCommon.MODID, "textures/title/pose_dokkiri_daiseikou.png");
     * private static final int TEX_SIZE = 400;
     * private static final int MARGIN = 32;
     * private static boolean titleSoundPlayed;
     * private static boolean titleSoundScheduled;
     *
     * public static void onScreenOpening(ScreenEvent.Opening event) {
     *     if (!ENABLED) return;
     *     if (event.getScreen() instanceof TitleScreen) {
     *         titleSoundPlayed = false;
     *         titleSoundScheduled = false;
     *     }
     * }
     *
     * public static void onRenderBackground(ScreenEvent.Render.Background event) {
     *     if (!ENABLED || !(event.getScreen() instanceof TitleScreen)) return;
     *
     *     Minecraft mc = Minecraft.getInstance();
     *     int screenWidth = mc.getWindow().getGuiScaledWidth();
     *     int screenHeight = mc.getWindow().getGuiScaledHeight();
     *     int maxWidth = screenWidth / 2 - MARGIN * 2;
     *     int maxHeight = screenHeight - MARGIN * 3;
     *     int displaySize = Math.min(TEX_SIZE, Math.min(maxWidth, maxHeight));
     *     int x = screenWidth - displaySize - MARGIN;
     *     int y = (screenHeight - displaySize) / 2;
     *
     *     GuiGraphicsExtractor gui = event.getGuiGraphics();
     *     gui.blitInscribed(POSE_TEXTURE, x + 40, y + 30, displaySize, displaySize, TEX_SIZE, TEX_SIZE, true, true);
     *     scheduleTitleSoundAfterFirstPoseRender(mc);
     * }
     *
     * private static void scheduleTitleSoundAfterFirstPoseRender(Minecraft mc) {
     *     if (titleSoundPlayed || titleSoundScheduled) return;
     *     titleSoundScheduled = true;
     *     mc.execute(() -> {
     *         if (titleSoundPlayed || !(mc.gui.screen() instanceof TitleScreen)) return;
     *         titleSoundPlayed = true;
     *         mc.getSoundManager().play(
     *                 SimpleSoundInstance.forUI(EconomyMod.TITLE_TRANSITION_SE.get(), 1.0F));
     *     });
     * }
     */

    private TitleScreenOverlay() {
    }

    public static void onScreenOpening(ScreenEvent.Opening event) {
        // 再有効化時は上記参照実装をここへ戻す
    }

    public static void onRenderBackground(ScreenEvent.Render.Background event) {
        // 再有効化時は上記参照実装をここへ戻す
    }
}
