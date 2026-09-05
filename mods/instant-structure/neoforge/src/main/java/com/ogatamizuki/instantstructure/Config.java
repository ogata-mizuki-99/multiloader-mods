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

    /** Copy NeoForge ModConfigSpec values into the shared runtime config used by build logic. */
    public static void syncToCommon() {
        InstantStructureConfig.enableCraftingRecipe = ENABLE_CRAFTING_RECIPE.get();
        InstantStructureConfig.enableMaterialConsumption = ENABLE_MATERIAL_CONSUMPTION.get();
        InstantStructureConfig.dropClearedBlocks = DROP_CLEARED_BLOCKS.get();
    }

    /** Copy shared runtime config into ModConfigSpec (after GUI / push). */
    public static void applyFromCommon() {
        ENABLE_CRAFTING_RECIPE.set(InstantStructureConfig.enableCraftingRecipe);
        ENABLE_CRAFTING_RECIPE.save();
        ENABLE_MATERIAL_CONSUMPTION.set(InstantStructureConfig.enableMaterialConsumption);
        ENABLE_MATERIAL_CONSUMPTION.save();
        DROP_CLEARED_BLOCKS.set(InstantStructureConfig.dropClearedBlocks);
        DROP_CLEARED_BLOCKS.save();
    }
}
