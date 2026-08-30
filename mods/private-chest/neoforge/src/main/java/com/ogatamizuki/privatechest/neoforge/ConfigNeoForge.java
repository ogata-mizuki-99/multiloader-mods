package com.ogatamizuki.privatechest.neoforge;

import com.ogatamizuki.privatechest.Config;
import net.neoforged.neoforge.common.ModConfigSpec;

public final class ConfigNeoForge {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue ENABLE_LOCKER_CRAFTING = BUILDER
            .comment("true の場合、サバイバルモードでプライベートロッカー（チェスト）をクラフトできるようにします。")
            .translation("privatechest.config.enableLockerCrafting")
            .define("enableLockerCrafting", true);

    public static final ModConfigSpec SPEC = BUILDER.build();

    private ConfigNeoForge() {}

    public static void sync() {
        Config.setEnableLockerCrafting(ENABLE_LOCKER_CRAFTING.get());
    }

    public static void updateFromPush(boolean enableCrafting) {
        ENABLE_LOCKER_CRAFTING.set(enableCrafting);
        ENABLE_LOCKER_CRAFTING.save();
        sync();
    }
}
