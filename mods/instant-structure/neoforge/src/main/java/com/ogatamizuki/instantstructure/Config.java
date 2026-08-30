package com.ogatamizuki.instantstructure;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue ENABLE_CRAFTING_RECIPE = BUILDER
            .comment("If true, the structure marker and instant builder can be crafted.",
                    "If false, obtain them via creative mode or commands.")
            .define("enableCraftingRecipe", true);

    public static final ModConfigSpec.BooleanValue ENABLE_MATERIAL_CONSUMPTION = BUILDER
            .comment("If true, placing structures in survival mode will consume matching blocks from the player's inventory or anchor chest.")
            .define("enableMaterialConsumption", true);

    public static final ModConfigSpec.BooleanValue DROP_CLEARED_BLOCKS = BUILDER
            .comment("If true, blocks cleared by structure placement in survival mode will drop as items in the world.")
            .define("dropClearedBlocks", true);

    public static final ModConfigSpec SPEC = BUILDER.build();

    private Config() {
    }
}
