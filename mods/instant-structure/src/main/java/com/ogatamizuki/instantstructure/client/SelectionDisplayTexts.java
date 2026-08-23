package com.ogatamizuki.instantstructure.client;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

public final class SelectionDisplayTexts {
    private SelectionDisplayTexts() {
    }

    public static String point1(BlockPos pos) {
        return line("instant_structure.hud.selection_point1", pos);
    }

    public static String point2(BlockPos pos) {
        return line("instant_structure.hud.selection_point2", pos);
    }

    public static String point2Preview(BlockPos pos) {
        return line("instant_structure.hud.selection_point2_preview", pos);
    }

    public static String size(SelectionBounds bounds) {
        return Component.translatable(
                "instant_structure.hud.selection_size",
                String.valueOf(bounds.sizeX()),
                String.valueOf(bounds.sizeY()),
                String.valueOf(bounds.sizeZ())
        ).getString();
    }

    public static String exportRange(BlockPos pos1, BlockPos pos2) {
        return Component.translatable(
                "instant_structure.screen.export.range",
                String.valueOf(pos1.getX()), String.valueOf(pos1.getY()), String.valueOf(pos1.getZ()),
                String.valueOf(pos2.getX()), String.valueOf(pos2.getY()), String.valueOf(pos2.getZ())
        ).getString();
    }

    public static String exportSize(SelectionBounds bounds) {
        return Component.translatable(
                "instant_structure.screen.export.size",
                String.valueOf(bounds.sizeX()),
                String.valueOf(bounds.sizeY()),
                String.valueOf(bounds.sizeZ())
        ).getString();
    }

    private static String line(String key, BlockPos pos) {
        return Component.translatable(
                key,
                String.valueOf(pos.getX()),
                String.valueOf(pos.getY()),
                String.valueOf(pos.getZ())
        ).getString();
    }
}
