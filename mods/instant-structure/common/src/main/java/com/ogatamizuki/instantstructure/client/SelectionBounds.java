package com.ogatamizuki.instantstructure.client;

import net.minecraft.core.BlockPos;

public record SelectionBounds(
        int minX, int minY, int minZ,
        int maxX, int maxY, int maxZ
) {
    public static SelectionBounds from(BlockPos pos1, BlockPos pos2) {
        return new SelectionBounds(
                Math.min(pos1.getX(), pos2.getX()),
                Math.min(pos1.getY(), pos2.getY()),
                Math.min(pos1.getZ(), pos2.getZ()),
                Math.max(pos1.getX(), pos2.getX()),
                Math.max(pos1.getY(), pos2.getY()),
                Math.max(pos1.getZ(), pos2.getZ())
        );
    }

    public int sizeX() {
        return maxX - minX + 1;
    }

    public int sizeY() {
        return maxY - minY + 1;
    }

    public int sizeZ() {
        return maxZ - minZ + 1;
    }
}
