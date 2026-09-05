package com.ogatamizuki.instantstructure.client;

import net.minecraft.core.BlockPos;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.util.ARGB;
import net.minecraft.world.phys.AABB;

public final class SelectionWireframeRenderer {
    /** Block surface z-fighting を避けるための描画オフセット */
    private static final double SURFACE_OFFSET = 0.04;

    private SelectionWireframeRenderer() {
    }

    public static void drawBox(
            BlockPos pos1,
            BlockPos pos2,
            float r,
            float g,
            float b,
            float a,
            float lineWidth
    ) {
        double minX = Math.min(pos1.getX(), pos2.getX());
        double maxX = Math.max(pos1.getX(), pos2.getX()) + 1.0;
        double minY = Math.min(pos1.getY(), pos2.getY());
        double maxY = Math.max(pos1.getY(), pos2.getY()) + 1.0;
        double minZ = Math.min(pos1.getZ(), pos2.getZ());
        double maxZ = Math.max(pos1.getZ(), pos2.getZ()) + 1.0;

        drawBox(
                minX - SURFACE_OFFSET,
                minY + SURFACE_OFFSET,
                minZ - SURFACE_OFFSET,
                maxX + SURFACE_OFFSET,
                maxY + SURFACE_OFFSET,
                maxZ + SURFACE_OFFSET,
                r, g, b, a,
                lineWidth
        );
    }

    public static void drawBox(
            double minX, double minY, double minZ,
            double maxX, double maxY, double maxZ,
            float r, float g, float b, float a,
            float lineWidth
    ) {
        int color = ARGB.colorFromFloat(a, r, g, b);
        Gizmos.cuboid(new AABB(minX, minY, minZ, maxX, maxY, maxZ), GizmoStyle.stroke(color, lineWidth));
    }
}
