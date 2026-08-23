package com.ogatamizuki.instantstructure.client;

import com.ogatamizuki.instantstructure.InstantStructurePaths;
import com.ogatamizuki.instantstructure.PreviewBlockEntry;
import com.ogatamizuki.instantstructure.StructureTemplateHelper;
import net.minecraft.client.Minecraft;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class ClientPreviewLoader {
    private static final Map<String, List<PreviewBlockEntry>> PARSED_CACHE = new ConcurrentHashMap<>();

    private ClientPreviewLoader() {
    }

    public static void requestPreview(String category, String templateName) {
        Path nbtPath = StructureTemplateHelper.resolveTemplatePath(
                InstantStructurePaths.configRoot(),
                category,
                templateName
        );
        if (!Files.exists(nbtPath)) {
            return;
        }

        String cacheKey = cacheKey(category, templateName);
        List<PreviewBlockEntry> cached = PARSED_CACHE.get(cacheKey);
        if (cached != null) {
            applyIfCurrent(category, templateName, cached);
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        mc.execute(() -> {
            if (mc.level == null) {
                return;
            }
            try {
                List<PreviewBlockEntry> blocks = StructureTemplateHelper.extractSolidBlocks(mc.level, nbtPath);
                if (!blocks.isEmpty()) {
                    PARSED_CACHE.put(cacheKey, List.copyOf(blocks));
                }
                applyIfCurrent(category, templateName, blocks);
            } catch (Exception ignored) {
                // Server preview packet remains the primary source.
            }
        });
    }

    public static void storeFromPayload(String category, String templateName, List<PreviewBlockEntry> blocks) {
        if (blocks.isEmpty()) {
            return;
        }
        PARSED_CACHE.put(cacheKey(category, templateName), List.copyOf(blocks));
        applyIfCurrent(category, templateName, blocks);
    }

    private static String cacheKey(String category, String templateName) {
        return category.toLowerCase() + "/" + templateName;
    }

    private static void applyIfCurrent(String category, String templateName, List<PreviewBlockEntry> blocks) {
        if (blocks.isEmpty()) {
            return;
        }
        if (!ClientPlacementRegistry.active) {
            return;
        }
        if (!category.equals(ClientPlacementRegistry.category)) {
            return;
        }
        if (!templateName.equals(ClientPlacementRegistry.templateName)) {
            return;
        }
        ClientPlacementRegistry.previewBlocks = blocks;
        ClientPlacementRegistry.previewCache = null;
    }
}
