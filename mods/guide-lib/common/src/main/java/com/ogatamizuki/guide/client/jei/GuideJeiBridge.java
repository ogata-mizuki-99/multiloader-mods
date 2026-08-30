package com.ogatamizuki.guide.client.jei;

import com.ogatamizuki.guide.platform.Services;
import net.minecraft.world.item.ItemStack;

import java.lang.reflect.Method;

/**
 * JEI 未導入時に JEI クラスをロードしないための反射ブリッジ。
 */
public final class GuideJeiBridge {
    private static final String JEI_MOD_ID = "jei";
    private static final String JEI_CLIENT_CLASS = "com.ogatamizuki.guide.client.jei.GuideJeiClient";

    private GuideJeiBridge() {}

    public static boolean isLoaded() {
        return Services.PLATFORM.isModLoaded(JEI_MOD_ID);
    }

    public static void showIngredientUses(ItemStack stack) {
        invoke("showIngredientUses", stack);
    }

    public static void showIngredientRecipes(ItemStack stack) {
        invoke("showIngredientRecipes", stack);
    }

    public static boolean handleItemClick(ItemStack stack, int button) {
        if (!isLoaded() || stack.isEmpty()) {
            return false;
        }
        if (button == 0) {
            showIngredientRecipes(stack);
            return true;
        }
        if (button == 1) {
            showIngredientUses(stack);
            return true;
        }
        return false;
    }

    private static void invoke(String methodName, ItemStack stack) {
        if (!isLoaded() || stack.isEmpty()) {
            return;
        }
        try {
            Class<?> clientClass = Class.forName(JEI_CLIENT_CLASS);
            Method method = clientClass.getDeclaredMethod(methodName, ItemStack.class);
            method.invoke(null, stack);
        } catch (ReflectiveOperationException ignored) {
        }
    }
}
