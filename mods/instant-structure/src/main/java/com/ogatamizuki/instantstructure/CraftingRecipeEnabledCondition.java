package com.ogatamizuki.instantstructure;

import com.mojang.serialization.MapCodec;
import net.neoforged.neoforge.common.conditions.ICondition;

public record CraftingRecipeEnabledCondition() implements ICondition {
    public static final MapCodec<CraftingRecipeEnabledCondition> CODEC =
            MapCodec.unit(new CraftingRecipeEnabledCondition());

    @Override
    public MapCodec<? extends ICondition> codec() {
        return CODEC;
    }

    @Override
    public boolean test(IContext context) {
        return Config.ENABLE_CRAFTING_RECIPE.get();
    }
}
