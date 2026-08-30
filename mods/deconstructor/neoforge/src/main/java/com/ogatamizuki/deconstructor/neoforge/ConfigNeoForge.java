package com.ogatamizuki.deconstructor.neoforge;

import com.ogatamizuki.deconstructor.Config;
import net.neoforged.neoforge.common.ModConfigSpec;

public final class ConfigNeoForge {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.ConfigValue<String> EXCLUDED_ITEMS = BUILDER
            .comment("Comma- or space-separated item IDs that cannot be deconstructed (e.g. minecraft:netherite_sword).")
            .define("excludedItems", "");

    public static final ModConfigSpec SPEC = BUILDER.build();

    private ConfigNeoForge() {}

    public static void sync() {
        Config.setExcludedItems(EXCLUDED_ITEMS.get());
    }
}
