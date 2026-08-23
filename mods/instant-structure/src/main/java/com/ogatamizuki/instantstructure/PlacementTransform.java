package com.ogatamizuki.instantstructure;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

public record PlacementTransform(int rotationDegrees, boolean flipLeftRight, boolean flipFrontBack) {
    public static final PlacementTransform NONE = new PlacementTransform(0, false, false);

    public BlockPos toWorldRelative(BlockPos local) {
        if (usesManualPlacement()) {
            return transformRelative(local);
        }
        return StructureTemplate.calculateRelativePosition(toPlaceSettings(), local);
    }

    public BlockPos toWorldPos(BlockPos anchor, BlockPos local) {
        return anchor.offset(toWorldRelative(local));
    }

    public BlockPos transformRelative(BlockPos local) {
        int x = local.getX();
        int y = local.getY();
        int z = local.getZ();
        if (flipLeftRight) {
            z = -z;
        }
        if (flipFrontBack) {
            x = -x;
        }
        return StructureTemplate.transform(
                new BlockPos(x, y, z),
                Mirror.NONE,
                StructureTemplateHelper.toRotation(rotationDegrees),
                BlockPos.ZERO
        );
    }

    @SuppressWarnings("deprecation")
    public BlockState transformBlockState(BlockState state) {
        BlockState transformed = state;
        if (flipLeftRight) {
            transformed = transformed.mirror(Mirror.LEFT_RIGHT);
        }
        if (flipFrontBack) {
            transformed = transformed.mirror(Mirror.FRONT_BACK);
        }
        return transformed.rotate(StructureTemplateHelper.toRotation(rotationDegrees));
    }

    public boolean usesManualPlacement() {
        return flipLeftRight && flipFrontBack;
    }

    public StructurePlaceSettings toPlaceSettings() {
        Mirror mirror = Mirror.NONE;
        if (flipLeftRight && !flipFrontBack) {
            mirror = Mirror.LEFT_RIGHT;
        } else if (flipFrontBack && !flipLeftRight) {
            mirror = Mirror.FRONT_BACK;
        }
        return StructureTemplateHelper.createPlaceSettings(rotationDegrees, mirror);
    }

    public BlockPos toPlacementOrigin(BlockPos anchor, int sizeX, int sizeY, int sizeZ) {
        if (usesManualPlacement()) {
            return anchor.subtract(minTransformedCorner(sizeX, sizeY, sizeZ));
        }
        StructurePlaceSettings settings = toPlaceSettings();
        return StructureTemplate.getZeroPositionWithTransform(
                anchor,
                settings.getMirror(),
                settings.getRotation(),
                sizeX,
                sizeZ
        );
    }

    private BlockPos minTransformedCorner(int sizeX, int sizeY, int sizeZ) {
        int sx = Math.max(sizeX, 1);
        int sy = Math.max(sizeY, 1);
        int sz = Math.max(sizeZ, 1);
        BlockPos[] corners = {
                BlockPos.ZERO,
                new BlockPos(sx - 1, 0, 0),
                new BlockPos(0, sy - 1, 0),
                new BlockPos(0, 0, sz - 1),
                new BlockPos(sx - 1, sy - 1, 0),
                new BlockPos(sx - 1, 0, sz - 1),
                new BlockPos(0, sy - 1, sz - 1),
                new BlockPos(sx - 1, sy - 1, sz - 1)
        };
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int minZ = Integer.MAX_VALUE;
        for (BlockPos corner : corners) {
            BlockPos relative = toWorldRelative(corner);
            minX = Math.min(minX, relative.getX());
            minY = Math.min(minY, relative.getY());
            minZ = Math.min(minZ, relative.getZ());
        }
        return new BlockPos(minX, minY, minZ);
    }
}
