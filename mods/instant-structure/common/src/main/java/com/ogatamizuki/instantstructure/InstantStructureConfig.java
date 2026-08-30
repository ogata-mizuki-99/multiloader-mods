package com.ogatamizuki.instantstructure;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public final class InstantStructureConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_FILE = InstantStructurePaths.configRoot().resolve("config.json");

    public static boolean enableCraftingRecipe = true;
    public static boolean enableMaterialConsumption = true;
    public static boolean dropClearedBlocks = true;

    private InstantStructureConfig() {}

    public static void load() {
        try {
            if (!Files.exists(CONFIG_FILE)) {
                save();
                return;
            }
            try (Reader reader = Files.newBufferedReader(CONFIG_FILE)) {
                Data data = GSON.fromJson(reader, Data.class);
                if (data != null) {
                    enableCraftingRecipe = data.enableCraftingRecipe;
                    enableMaterialConsumption = data.enableMaterialConsumption;
                    dropClearedBlocks = data.dropClearedBlocks;
                }
            }
        } catch (Exception e) {
            InstantStructureCommon.LOGGER.warn("Failed to load instant-structure config.json", e);
        }
    }

    public static void save() {
        try {
            Files.createDirectories(InstantStructurePaths.configRoot());
            try (Writer writer = Files.newBufferedWriter(CONFIG_FILE)) {
                Data data = new Data();
                data.enableCraftingRecipe = enableCraftingRecipe;
                data.enableMaterialConsumption = enableMaterialConsumption;
                data.dropClearedBlocks = dropClearedBlocks;
                GSON.toJson(data, writer);
            }
        } catch (Exception e) {
            InstantStructureCommon.LOGGER.warn("Failed to save instant-structure config.json", e);
        }
    }

    private static class Data {
        boolean enableCraftingRecipe = true;
        boolean enableMaterialConsumption = true;
        boolean dropClearedBlocks = true;
    }
}
