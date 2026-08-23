package com.ogatamizuki.lookalike;

import net.neoforged.neoforge.common.ModConfigSpec;

public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.IntValue disguiseDurationSeconds = BUILDER
            .comment("変装のデフォルト持続時間（秒）。")
            .translation("lookalike.config.disguiseDurationSeconds")
            .defineInRange("disguiseDurationSeconds", 60, 1, 86400);

    public static final ModConfigSpec.BooleanValue allowDefaultPlayerList = BUILDER
            .comment("true の場合、他プレイヤーの見た目がデフォルトでラジアルメニューに一覧表示される。false の場合、スキャンが必要。")
            .translation("lookalike.config.allowDefaultPlayerList")
            .define("allowDefaultPlayerList", false);
    public static final ModConfigSpec.BooleanValue hideAllNametags = BUILDER
            .comment("true の場合、マルチプレイ時にサーバー内の全プレイヤーの頭上のネームタグを非表示にします。")
            .translation("lookalike.config.hideAllNametags")
            .define("hideAllNametags", false);

    public static final ModConfigSpec.BooleanValue enableMirrorCrafting = BUILDER
            .comment("true の場合、サバイバルモードで変装の鏡をクラフトできるようにします。")
            .translation("lookalike.config.enableMirrorCrafting")
            .define("enableMirrorCrafting", true);

    public static final ModConfigSpec.IntValue defaultCastTimeSeconds = BUILDER
            .comment("変装の鏡使用時などのデフォルトキャスト時間（秒）。0 で即時変身。")
            .translation("lookalike.config.defaultCastTimeSeconds")
            .defineInRange("defaultCastTimeSeconds", 0, 0, 60);

    public static final ModConfigSpec.ConfigValue<String> defaultEffectTemplate = BUILDER
            .comment("変装時のデフォルト演出テンプレート（WITCH_SMOKE, ENDER, PORTAL, NONE）。")
            .translation("lookalike.config.defaultEffectTemplate")
            .define("defaultEffectTemplate", "WITCH_SMOKE");

    static final ModConfigSpec SPEC = BUILDER.build();
}
