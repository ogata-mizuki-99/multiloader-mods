package com.ogatamizuki.lookalike.neoforge;

import com.ogatamizuki.lookalike.Config;
import net.neoforged.neoforge.common.ModConfigSpec;

public class LookalikeNeoForgeConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    private static final ModConfigSpec.IntValue DISGUISE_DURATION = BUILDER
            .comment("変装のデフォルト持続時間（秒）。")
            .translation("lookalike.config.disguiseDurationSeconds")
            .defineInRange("disguiseDurationSeconds", 60, 1, 86400);

    private static final ModConfigSpec.BooleanValue ALLOW_DEFAULT_PLAYER_LIST = BUILDER
            .comment("true の場合、他プレイヤーの見た目がデフォルトでラジアルメニューに一覧表示される。false の場合、スキャンが必要。")
            .translation("lookalike.config.allowDefaultPlayerList")
            .define("allowDefaultPlayerList", false);

    private static final ModConfigSpec.BooleanValue HIDE_ALL_NAMETAGS = BUILDER
            .comment("true の場合、マルチプレイ時にサーバー内の全プレイヤーの頭上のネームタグを非表示にします。")
            .translation("lookalike.config.hideAllNametags")
            .define("hideAllNametags", false);

    private static final ModConfigSpec.BooleanValue ENABLE_MIRROR_CRAFTING = BUILDER
            .comment("true の場合、サバイバルモードで変装の鏡をクラフトできるようにします。")
            .translation("lookalike.config.enableMirrorCrafting")
            .define("enableMirrorCrafting", true);

    private static final ModConfigSpec.IntValue DEFAULT_CAST_TIME = BUILDER
            .comment("変装の鏡使用時などのデフォルトキャスト時間（秒）。0 で即時変身。")
            .translation("lookalike.config.defaultCastTimeSeconds")
            .defineInRange("defaultCastTimeSeconds", 0, 0, 60);

    private static final ModConfigSpec.ConfigValue<String> DEFAULT_EFFECT_TEMPLATE = BUILDER
            .comment("変装時のデフォルト演出テンプレート（WITCH_SMOKE, ENDER, PORTAL, NONE）。")
            .translation("lookalike.config.defaultEffectTemplate")
            .define("defaultEffectTemplate", "WITCH_SMOKE");

    public static final ModConfigSpec SPEC = BUILDER.build();

    public static void syncToCommon() {
        Config.disguiseDurationSeconds.bind(DISGUISE_DURATION::get, DISGUISE_DURATION::set, DISGUISE_DURATION::save);
        Config.allowDefaultPlayerList.bind(ALLOW_DEFAULT_PLAYER_LIST::get, ALLOW_DEFAULT_PLAYER_LIST::set, ALLOW_DEFAULT_PLAYER_LIST::save);
        Config.hideAllNametags.bind(HIDE_ALL_NAMETAGS::get, HIDE_ALL_NAMETAGS::set, HIDE_ALL_NAMETAGS::save);
        Config.enableMirrorCrafting.bind(ENABLE_MIRROR_CRAFTING::get, ENABLE_MIRROR_CRAFTING::set, ENABLE_MIRROR_CRAFTING::save);
        Config.defaultCastTimeSeconds.bind(DEFAULT_CAST_TIME::get, DEFAULT_CAST_TIME::set, DEFAULT_CAST_TIME::save);
        Config.defaultEffectTemplate.bind(DEFAULT_EFFECT_TEMPLATE::get, DEFAULT_EFFECT_TEMPLATE::set, DEFAULT_EFFECT_TEMPLATE::save);
    }
}
