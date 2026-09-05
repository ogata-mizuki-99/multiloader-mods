package com.ogatamizuki.deconstructor;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;

public final class Config {
    private static volatile String excludedItems = "";

    private Config() {}

    public static String getExcludedItems() {
        return excludedItems;
    }

    public static void setExcludedItems(String value) {
        excludedItems = (value == null) ? "" : value;
    }

    public static boolean isExcluded(Item item) {
        String raw = excludedItems;
        if (raw == null || raw.isBlank()) {
            return false;
        }
        String itemId = BuiltInRegistries.ITEM.getKey(item).toString();
        for (String entry : raw.split("[,\\s]+")) {
            String trimmed = entry.trim();
            if (!trimmed.isEmpty() && itemId.equals(trimmed)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isValidItemId(String id) {
        return id != null && !id.isBlank() && Identifier.tryParse(id.trim()) != null;
    }
}
