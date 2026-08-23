package com.ogatamizuki.lookalike;

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
        boolean enabled = Config.enableMirrorCrafting.get();
        LookalikeMod.LOGGER.info("Lookalike Recipe Condition checked: enableMirrorCrafting = " + enabled);
        return enabled;
    }
}
