package com.ogatamizuki.instantstructure;

import java.nio.file.Files;
import java.nio.file.Path;

public final class InstantStructurePaths {
    private static final String ROOT = "instant-structure";

    private InstantStructurePaths() {
    }

    public static Path configRoot() {
        return InstantStructurePlatform.getConfigDir().resolve(ROOT);
    }

    public static Path exportsDir() {
        return configRoot().resolve("exports");
    }

    public static Path templatesStructureDir() {
        return configRoot().resolve("templates-structure");
    }

    public static void ensureExportsDir() {
        try {
            Files.createDirectories(exportsDir());
        } catch (Exception e) {
            InstantStructureCommon.LOGGER.error("Failed to create exports directory", e);
        }
    }

    public static void ensureTemplatesStructureDir() {
        try {
            Path root = templatesStructureDir();
            Files.createDirectories(root.resolve("houses"));
            Files.createDirectories(root.resolve("arenas"));
            Files.createDirectories(root.resolve("custom"));
        } catch (Exception e) {
            InstantStructureCommon.LOGGER.error("Failed to create templates-structure directory", e);
        }
    }
}
