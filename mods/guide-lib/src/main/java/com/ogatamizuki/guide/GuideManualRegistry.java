package com.ogatamizuki.guide;

import com.ogatamizuki.guide.model.GuideManualDefinition;
import net.minecraft.resources.Identifier;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class GuideManualRegistry {
    private static Map<Identifier, GuideManualDefinition> byItemId = Map.of();

    private GuideManualRegistry() {}

    public static void setManuals(Map<Identifier, GuideManualDefinition> loaded) {
        byItemId = Collections.unmodifiableMap(new LinkedHashMap<>(loaded));
    }

    public static boolean isEmpty() {
        return byItemId.isEmpty();
    }

    public static GuideManualDefinition byItem(Identifier itemId) {
        return byItemId.get(itemId);
    }
}
