package com.ogatamizuki.instantstructure;

import net.minecraft.resources.Identifier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.function.Supplier;

public final class InstantStructureCommon {
    public static final String MODID = "instant_structure";
    public static final Logger LOGGER = LogManager.getLogger("InstantStructure");

    public static Supplier<StructureMarkerItem> STRUCTURE_MARKER;
    public static Supplier<InstantBuilderItem> INSTANT_BUILDER;

    private InstantStructureCommon() {}

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MODID, path);
    }
}
