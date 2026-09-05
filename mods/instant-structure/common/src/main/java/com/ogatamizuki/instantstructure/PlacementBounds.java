package com.ogatamizuki.instantstructure;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.AABB;

import java.util.List;

public record PlacementBounds(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
    public static PlacementBounds from(StructureTemplate template, BlockPos origin, StructurePlaceSettings settings) {
        net.minecraft.core.Vec3i size = template.getSize();
        BlockPos minCorner = StructureTemplate.calculateRelativePosition(settings, BlockPos.ZERO);
        BlockPos maxCorner = StructureTemplate.calculateRelativePosition(
                settings,
                new BlockPos(size.getX() - 1, size.getY() - 1, size.getZ() - 1)
        );

        return new PlacementBounds(
                Math.min(origin.getX() + minCorner.getX(), origin.getX() + maxCorner.getX()),
                Math.min(origin.getY() + minCorner.getY(), origin.getY() + maxCorner.getY()),
                Math.min(origin.getZ() + minCorner.getZ(), origin.getZ() + maxCorner.getZ()),
                Math.max(origin.getX() + minCorner.getX(), origin.getX() + maxCorner.getX()),
                Math.max(origin.getY() + minCorner.getY(), origin.getY() + maxCorner.getY()),
                Math.max(origin.getZ() + minCorner.getZ(), origin.getZ() + maxCorner.getZ())
        );
    }

    public static PlacementBounds from(StructureTemplate template, BlockPos origin, PlacementTransform transform) {
        if (transform.usesManualPlacement()) {
            Vec3i size = template.getSize();
            int sx = Math.max(size.getX(), 1);
            int sy = Math.max(size.getY(), 1);
            int sz = Math.max(size.getZ(), 1);
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
            int maxX = Integer.MIN_VALUE;
            int maxY = Integer.MIN_VALUE;
            int maxZ = Integer.MIN_VALUE;
            for (BlockPos corner : corners) {
                BlockPos relative = transform.toWorldRelative(corner);
                minX = Math.min(minX, relative.getX());
                minY = Math.min(minY, relative.getY());
                minZ = Math.min(minZ, relative.getZ());
                maxX = Math.max(maxX, relative.getX());
                maxY = Math.max(maxY, relative.getY());
                maxZ = Math.max(maxZ, relative.getZ());
            }
            return new PlacementBounds(
                    origin.getX() + minX,
                    origin.getY() + minY,
                    origin.getZ() + minZ,
                    origin.getX() + maxX,
                    origin.getY() + maxY,
                    origin.getZ() + maxZ
            );
        }
        return from(template, origin, transform.toPlaceSettings());
    }

    public static PlacementBounds fromSize(BlockPos origin, PlacementTransform transform, int sizeX, int sizeY, int sizeZ) {
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
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;
        int maxZ = Integer.MIN_VALUE;
        for (BlockPos corner : corners) {
            BlockPos relative = transform.toWorldRelative(corner);
            minX = Math.min(minX, relative.getX());
            minY = Math.min(minY, relative.getY());
            minZ = Math.min(minZ, relative.getZ());
            maxX = Math.max(maxX, relative.getX());
            maxY = Math.max(maxY, relative.getY());
            maxZ = Math.max(maxZ, relative.getZ());
        }
        return new PlacementBounds(
                origin.getX() + minX,
                origin.getY() + minY,
                origin.getZ() + minZ,
                origin.getX() + maxX,
                origin.getY() + maxY,
                origin.getZ() + maxZ
        );
    }

    public static PlacementBounds fromBlockInfos(
            BlockPos origin,
            PlacementTransform transform,
            List<StructureTemplate.StructureBlockInfo> blocks
    ) {
        if (blocks.isEmpty()) {
            return null;
        }
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;
        int maxZ = Integer.MIN_VALUE;

        for (StructureTemplate.StructureBlockInfo info : blocks) {
            BlockPos relative = transform.toWorldRelative(info.pos());
            minX = Math.min(minX, relative.getX());
            minY = Math.min(minY, relative.getY());
            minZ = Math.min(minZ, relative.getZ());
            maxX = Math.max(maxX, relative.getX());
            maxY = Math.max(maxY, relative.getY());
            maxZ = Math.max(maxZ, relative.getZ());
        }

        return new PlacementBounds(
                origin.getX() + minX,
                origin.getY() + minY,
                origin.getZ() + minZ,
                origin.getX() + maxX,
                origin.getY() + maxY,
                origin.getZ() + maxZ
        );
    }

    public static PlacementBounds fromPreviewBlocks(BlockPos origin, List<PreviewBlockEntry> blocks, PlacementTransform transform) {
        if (blocks.isEmpty()) {
            return null;
        }
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;
        int maxZ = Integer.MIN_VALUE;

        for (PreviewBlockEntry entry : blocks) {
            BlockPos relative = transform.toWorldRelative(new BlockPos(entry.x(), entry.y(), entry.z()));
            minX = Math.min(minX, relative.getX());
            minY = Math.min(minY, relative.getY());
            minZ = Math.min(minZ, relative.getZ());
            maxX = Math.max(maxX, relative.getX());
            maxY = Math.max(maxY, relative.getY());
            maxZ = Math.max(maxZ, relative.getZ());
        }

        return new PlacementBounds(
                origin.getX() + minX,
                origin.getY() + minY,
                origin.getZ() + minZ,
                origin.getX() + maxX,
                origin.getY() + maxY,
                origin.getZ() + maxZ
        );
    }

    public AABB toAabb() {
        return new AABB(minX, minY, minZ, maxX + 1.0, maxY + 1.0, maxZ + 1.0);
    }

    public boolean intersects(AABB box) {
        return box.intersects(toAabb());
    }

    public boolean containsAnyPlayer(net.minecraft.server.level.ServerLevel level) {
        for (net.minecraft.world.entity.player.Player player : level.players()) {
            if (intersects(player.getBoundingBox())) {
                return true;
            }
        }
        return false;
    }
}
