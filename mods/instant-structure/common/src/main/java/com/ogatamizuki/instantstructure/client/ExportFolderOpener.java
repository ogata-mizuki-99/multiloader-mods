package com.ogatamizuki.instantstructure.client;

import com.ogatamizuki.instantstructure.InstantStructurePaths;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;

import java.nio.file.Path;

public final class ExportFolderOpener {
    private ExportFolderOpener() {
    }

    public static void openExportsFolder(Minecraft mc) {
        openFolder(mc, InstantStructurePaths.templatesStructureDir(), InstantStructurePaths::ensureTemplatesStructureDir);
    }

    public static void openTemplatesStructureFolder(Minecraft mc) {
        openFolder(mc, InstantStructurePaths.templatesStructureDir(), InstantStructurePaths::ensureTemplatesStructureDir);
    }

    private static void openFolder(Minecraft mc, Path dir, Runnable ensureDir) {
        try {
            ensureDir.run();
            Util.getPlatform().openPath(dir);
        } catch (Exception e) {
            if (mc.player != null) {
                mc.player.sendSystemMessage(Component.translatable("instant_structure.message.open_folder_failed"));
            }
        }
    }
}
