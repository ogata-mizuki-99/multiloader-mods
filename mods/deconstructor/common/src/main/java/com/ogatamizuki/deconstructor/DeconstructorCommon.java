package com.ogatamizuki.deconstructor;

import net.minecraft.resources.Identifier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class DeconstructorCommon {
    public static final String MODID = "deconstructor";
    public static final Logger LOGGER = LogManager.getLogger("Deconstructor");

    private DeconstructorCommon() {}

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MODID, path);
    }
}
