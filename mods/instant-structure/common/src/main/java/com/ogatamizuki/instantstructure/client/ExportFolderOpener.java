package com.ogatamizuki.instantstructure.client;

import com.mojang.blaze3d.Blaze3D;
import com.ogatamizuki.instantstructure.InstantStructurePaths;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

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
            Blaze3D.openPath(dir);
        } catch (Exception e) {
            if (mc.player != null) {
                mc.player.sendSystemMessage(Component.translatable("instant_structure.message.open_folder_failed"));
            }
        }
    }
}
