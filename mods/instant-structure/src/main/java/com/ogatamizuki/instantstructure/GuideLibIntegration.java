package com.ogatamizuki.instantstructure;

import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;

public class GuideLibIntegration {
    private static Boolean available = null;

    public static boolean isAvailable() {
        if (available == null) {
            available = ModList.get().isLoaded("guide");
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
                    .invoke(null, "instant_structure", "instant_structure");

            return (ItemStack) createManualMethod.invoke(null, bookId, "item.instant_structure.guide_book", null);
        } catch (Exception e) {
            InstantStructureMod.LOGGER.error("Failed to create Guide Book via reflection", e);
            return ItemStack.EMPTY;
        }
    }
}
