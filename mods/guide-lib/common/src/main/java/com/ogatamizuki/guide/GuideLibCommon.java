package com.ogatamizuki.guide;

import net.minecraft.resources.Identifier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class GuideLibCommon {
    public static final String MODID = "guide_lib";
    public static final Logger LOGGER = LogManager.getLogger("GuideLib");

    private GuideLibCommon() {}

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MODID, path);
    }
}
