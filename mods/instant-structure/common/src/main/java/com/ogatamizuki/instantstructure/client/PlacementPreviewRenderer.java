package com.ogatamizuki.instantstructure.client;

import com.ogatamizuki.instantstructure.PlacementBounds;
import com.ogatamizuki.instantstructure.PlacementTransform;
import com.ogatamizuki.instantstructure.PreviewBlockEntry;
import net.minecraft.core.BlockPos;

import java.util.List;

public final class PlacementPreviewRenderer {
    private PlacementPreviewRenderer() {
    }

    public static void renderWireframe(
            BlockPos anchor,
            PlacementTransform transform,
            int sizeX,
            int sizeY,
            int sizeZ,
            List<PreviewBlockEntry> previewBlocks,
            float lineWidth,
            boolean tentativelyConfirmed
    ) {
        Bounds bounds = resolveBounds(anchor, transform, sizeX, sizeY, sizeZ, previewBlocks);
        float r = tentativelyConfirmed ? 0.0F : 1.0F;
        float g = tentativelyConfirmed ? 1.0F : 0.85F;
        float b = tentativelyConfirmed ? 0.0F : 0.0F;

        SelectionWireframeRenderer.drawBox(
                bounds.minX(), bounds.minY(), bounds.minZ(),
                bounds.maxX(), bounds.maxY(), bounds.maxZ(),
                r, g, b, 1.0F,
                lineWidth
        );
    }

    public static void renderAnchorWireframe(
            BlockPos anchorBlock,
            float lineWidth
    ) {
        SelectionWireframeRenderer.drawBox(
                anchorBlock,
                anchorBlock,
                1.0F, 0.0F, 0.0F, 1.0F,
                lineWidth
        );
    }

    private static Bounds resolveBounds(
            BlockPos anchor,
            PlacementTransform transform,
            int sizeX,
            int sizeY,
            int sizeZ,
            List<PreviewBlockEntry> previewBlocks
    ) {
        if (ClientPlacementRegistry.previewCache != null
                && ClientPlacementRegistry.previewCache.bounds() != null) {
            var b = ClientPlacementRegistry.previewCache.bounds();
            return new Bounds(b.minX(), b.minY(), b.minZ(), b.maxX() + 1.0, b.maxY() + 1.0, b.maxZ() + 1.0);
        }
        if (ClientPlacementRegistry.isSimplifiedGhostMode()) {
            var b = PlacementBounds.fromSize(anchor, transform, sizeX, sizeY, sizeZ);
            return new Bounds(b.minX(), b.minY(), b.minZ(), b.maxX() + 1.0, b.maxY() + 1.0, b.maxZ() + 1.0);
        }
        return computeBounds(anchor, transform, sizeX, sizeY, sizeZ, previewBlocks);
    }

    private static Bounds computeBounds(
            BlockPos anchor,
            PlacementTransform transform,
            int sizeX,
            int sizeY,
            int sizeZ,
            List<PreviewBlockEntry> previewBlocks
    ) {
        if (!previewBlocks.isEmpty()) {
            int minX = Integer.MAX_VALUE;
            int minY = Integer.MAX_VALUE;
            int minZ = Integer.MAX_VALUE;
            int maxX = Integer.MIN_VALUE;
            int maxY = Integer.MIN_VALUE;
            int maxZ = Integer.MIN_VALUE;

            for (PreviewBlockEntry entry : previewBlocks) {
                BlockPos relative = transform.toWorldRelative(new BlockPos(entry.x(), entry.y(), entry.z()));
                minX = Math.min(minX, relative.getX());
                minY = Math.min(minY, relative.getY());
                minZ = Math.min(minZ, relative.getZ());
                maxX = Math.max(maxX, relative.getX());
                maxY = Math.max(maxY, relative.getY());
                maxZ = Math.max(maxZ, relative.getZ());
            }

            return new Bounds(
                    anchor.getX() + minX,
                    anchor.getY() + minY,
                    anchor.getZ() + minZ,
                    anchor.getX() + maxX + 1.0,
                    anchor.getY() + maxY + 1.0,
                    anchor.getZ() + maxZ + 1.0
            );
        }

        var b = PlacementBounds.fromSize(anchor, transform, sizeX, sizeY, sizeZ);
        return new Bounds(b.minX(), b.minY(), b.minZ(), b.maxX() + 1.0, b.maxY() + 1.0, b.maxZ() + 1.0);
    }

    private record Bounds(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
    }
}
