package com.ogatamizuki.lookalike;

import net.minecraft.world.item.ItemStack;

public class GuideLibIntegration {
    private static Boolean available = null;

    public static boolean isAvailable() {
        if (available == null) {
            available = LookalikePlatform.isModLoaded("guide_lib");
        }
        return available;
    }

    public static ItemStack createGuideBook() {
        if (!isAvailable()) {
            return ItemStack.EMPTY;
        }
        try {
            Class<?> guideItemsClass = Class.forName("com.ogatamizuki.guide.GuideItems");
            Class<?> identifierClass = Class.forName("net.minecraft.resources.Identifier");
            java.lang.reflect.Method createManualMethod = guideItemsClass.getMethod(
                    "createManual",
                    identifierClass,
                    String.class,
                    identifierClass
            );
            Object bookId = identifierClass.getMethod("fromNamespaceAndPath", String.class, String.class)
                    .invoke(null, "lookalike", "guide_book");

            return (ItemStack) createManualMethod.invoke(null, bookId, "item.lookalike.guide_book", null);
        } catch (Exception e) {
            LookalikeCommon.LOGGER.error("Failed to create Guide Book via reflection", e);
            return ItemStack.EMPTY;
        }
    }
}
