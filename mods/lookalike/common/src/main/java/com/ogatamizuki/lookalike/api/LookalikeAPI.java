package com.ogatamizuki.lookalike.api;

import com.ogatamizuki.lookalike.Config;
import com.ogatamizuki.lookalike.DisguiseManager;
import com.ogatamizuki.lookalike.cast.CastEffectTemplate;
import com.ogatamizuki.lookalike.cast.CastManager;
import net.minecraft.server.level.ServerPlayer;

public class LookalikeAPI {
    /**
     * 指定したプレイヤーの見た目（スキン・ネームタグ）を、対象プレイヤーのものに変装させます。
     * 内部でGameProfileの変更および周囲のクライアントへの再スポーンパケット送信を行います。
     * 効果時間は無限（解除メソッドが呼ばれるまで）となります。
     */
    public static void disguisePlayer(ServerPlayer player, ServerPlayer target) {
        DisguiseManager.getInstance().disguise(player, target);
    }

    /**
     * 指定したプレイヤーの見た目（スキン・ネームタグ）を、対象プレイヤーのものに変装させます。
     * 指定された効果時間（秒）が経過すると、自動的に変装が解除されます。
     * 内部で効果時間タイマー（Tickベースのスケジューラー）を管理します。
     */
    public static void disguisePlayer(ServerPlayer player, ServerPlayer target, int durationSeconds) {
        DisguiseManager.getInstance().disguise(player, target, durationSeconds);
    }

    /**
     * 指定したプレイヤーの見た目（スキン・ネームタグ）を、対象プレイヤーのものに変装させます。
     * キャスト時間（変身にかかる秒数・隙）を設け、指定された演出テンプレートに基づいて変身演出を再生します。
     * キャスト中にダメージを受けたり、ダッシュ等で大きく移動した場合、キャストは中断（変身失敗）されます。
     */
    public static void disguisePlayer(
            ServerPlayer player,
            ServerPlayer target,
            int durationSeconds,
            int castTimeSeconds,
            String effectTemplateName
    ) {
        CastManager.getInstance().startCast(
                player,
                target,
                durationSeconds,
                castTimeSeconds,
                CastEffectTemplate.fromName(effectTemplateName)
        );
    }

    /**
     * 指定したプレイヤーの変装を解除し、元のスキン・ネームタグに復元します。
     */
    public static void undisguisePlayer(ServerPlayer player) {
        DisguiseManager.getInstance().undisguise(player);
    }

    /**
     * コンフィグ(lookalike-common.toml)から変装のデフォルト効果時間（秒）を取得します。
     */
    public static int getDisguiseDurationSeconds() {
        return Config.disguiseDurationSeconds.get();
    }

    public static int getDefaultCastTimeSeconds() {
        return Config.defaultCastTimeSeconds.get();
    }

    public static String getDefaultEffectTemplate() {
        return Config.defaultEffectTemplate.get();
    }
}
