package com.ogatamizuki.privatechest;

import net.neoforged.neoforge.common.ModConfigSpec;

public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue enableLockerCrafting = BUILDER
            .comment("true の場合、サバイバルモードでプライベートロッカー（チェスト）をクラフトできるようにします。")
            .translation("privatechest.config.enableLockerCrafting")
            .define("enableLockerCrafting", true);

    static final ModConfigSpec SPEC = BUILDER.build();
}
