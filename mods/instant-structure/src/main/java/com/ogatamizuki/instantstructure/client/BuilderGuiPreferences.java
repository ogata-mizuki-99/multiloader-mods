package com.ogatamizuki.instantstructure.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.ogatamizuki.instantstructure.InstantStructureMod;
import com.ogatamizuki.instantstructure.InstantStructurePaths;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public final class BuilderGuiPreferences {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = InstantStructurePaths.configRoot().resolve("builder-gui.json");

    private static String lastCategory = "houses";
    private static String lastTemplateName = "";
    private static boolean loaded = false;

    private BuilderGuiPreferences() {
    }

    public static void ensureLoaded() {
        if (loaded) {
            return;
        }
        loaded = true;
        if (!Files.exists(FILE)) {
            return;
        }
        try (Reader reader = Files.newBufferedReader(FILE)) {
            Data data = GSON.fromJson(reader, Data.class);
            if (data != null) {
                if (data.category != null && !data.category.isBlank()) {
                    lastCategory = data.category;
                }
                if (data.templateName != null) {
                    lastTemplateName = data.templateName;
                }
            }
        } catch (Exception e) {
            InstantStructureMod.LOGGER.warn("Failed to load builder GUI preferences", e);
        }
    }

    public static String lastCategory() {
        ensureLoaded();
        return lastCategory;
    }

    public static String lastTemplateName() {
        ensureLoaded();
        return lastTemplateName;
    }

    public static void saveSelection(String category, String templateName) {
        ensureLoaded();
        lastCategory = category;
        lastTemplateName = templateName;
        try {
            Files.createDirectories(FILE.getParent());
            try (Writer writer = Files.newBufferedWriter(FILE)) {
                GSON.toJson(new Data(category, templateName), writer);
            }
        } catch (Exception e) {
            InstantStructureMod.LOGGER.warn("Failed to save builder GUI preferences", e);
        }
    }

    private record Data(String category, String templateName) {
    }
}
