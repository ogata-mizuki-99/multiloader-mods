package com.ogatamizuki.guide.client.page;

import com.ogatamizuki.guide.GuideLibCommon;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class PageRenderers {
    private static final Map<String, PageRenderer> RENDERERS = new HashMap<>();
    private static final Set<String> WARNED_TYPES = new HashSet<>();

    static {
        TextPageRenderer text = new TextPageRenderer();
        CraftingPageRenderer crafting = new CraftingPageRenderer();
        SpotlightPageRenderer spotlight = new SpotlightPageRenderer();
        ImagePageRenderer image = new ImagePageRenderer();
        RENDERERS.put("text", text);
        RENDERERS.put("crafting", crafting);
        RENDERERS.put("spotlight", spotlight);
        RENDERERS.put("image", image);
    }

    private PageRenderers() {}

    public static PageRenderer get(String type) {
        PageRenderer renderer = RENDERERS.get(type);
        if (renderer == null) {
            if (WARNED_TYPES.add(type)) {
                GuideLibCommon.LOGGER.warn("Unknown guide page type '{}', rendering as text", type);
            }
            return RENDERERS.get("text");
        }
        return renderer;
    }
}
