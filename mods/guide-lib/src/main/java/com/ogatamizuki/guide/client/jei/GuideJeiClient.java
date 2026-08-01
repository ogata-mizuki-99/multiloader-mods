package com.ogatamizuki.guide.client.jei;

import mezz.jei.api.recipe.IFocus;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.world.item.ItemStack;

public final class GuideJeiClient {
    private static IJeiRuntime runtime;

    private GuideJeiClient() {}

    public static void setRuntime(IJeiRuntime jeiRuntime) {
        runtime = jeiRuntime;
    }

    public static void showIngredientUses(ItemStack stack) {
        show(stack, RecipeIngredientRole.INPUT);
    }

    public static void showIngredientRecipes(ItemStack stack) {
        show(stack, RecipeIngredientRole.OUTPUT);
    }

    private static void show(ItemStack stack, RecipeIngredientRole role) {
        if (runtime == null || stack.isEmpty()) {
            return;
        }
        var helpers = runtime.getJeiHelpers();
        var typedOptional = helpers.getIngredientManager().createTypedIngredient(stack, false);
        if (typedOptional.isEmpty()) {
            return;
        }
        IFocus<?> focus = helpers.getFocusFactory().createFocus(role, typedOptional.get());
        runtime.getRecipesGui().show(focus);
    }
}
