package com.ogatamizuki.instantstructure.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

public final class SelectionWireframeRenderer {
    /** Block surface z-fighting を避けるための描画オフセット */
    private static final double SURFACE_OFFSET = 0.04;

    private SelectionWireframeRenderer() {
    }

    public static void drawBox(
            PoseStack poseStack,
            VertexConsumer consumer,
            BlockPos pos1,
            BlockPos pos2,
            float r,
            float g,
            float b,
            float a,
            float lineWidth,
            Vec3 cameraPos
    ) {
        double minX = Math.min(pos1.getX(), pos2.getX());
        double maxX = Math.max(pos1.getX(), pos2.getX()) + 1.0;
        double minY = Math.min(pos1.getY(), pos2.getY());
        double maxY = Math.max(pos1.getY(), pos2.getY()) + 1.0;
        double minZ = Math.min(pos1.getZ(), pos2.getZ());
        double maxZ = Math.max(pos1.getZ(), pos2.getZ()) + 1.0;

        drawBox(
                poseStack,
                consumer,
                minX - SURFACE_OFFSET,
                minY + SURFACE_OFFSET,
                minZ - SURFACE_OFFSET,
                maxX + SURFACE_OFFSET,
                maxY + SURFACE_OFFSET,
                maxZ + SURFACE_OFFSET,
                r, g, b, a,
                lineWidth,
                cameraPos
        );
    }

    public static void drawBox(
            PoseStack poseStack,
            VertexConsumer consumer,
            double minX, double minY, double minZ,
            double maxX, double maxY, double maxZ,
            float r, float g, float b, float a,
            float lineWidth,
            Vec3 cameraPos
    ) {
        var pose = poseStack.last();
        var mat = pose.pose();

        // Bottom (4 edges)
        edge(consumer, pose, mat, minX, minY, minZ, maxX, minY, minZ, r, g, b, a, lineWidth, cameraPos);
        edge(consumer, pose, mat, maxX, minY, minZ, maxX, minY, maxZ, r, g, b, a, lineWidth, cameraPos);
        edge(consumer, pose, mat, maxX, minY, maxZ, minX, minY, maxZ, r, g, b, a, lineWidth, cameraPos);
        edge(consumer, pose, mat, minX, minY, maxZ, minX, minY, minZ, r, g, b, a, lineWidth, cameraPos);

        // Top (4 edges)
        edge(consumer, pose, mat, minX, maxY, minZ, maxX, maxY, minZ, r, g, b, a, lineWidth, cameraPos);
        edge(consumer, pose, mat, maxX, maxY, minZ, maxX, maxY, maxZ, r, g, b, a, lineWidth, cameraPos);
        edge(consumer, pose, mat, maxX, maxY, maxZ, minX, maxY, maxZ, r, g, b, a, lineWidth, cameraPos);
        edge(consumer, pose, mat, minX, maxY, maxZ, minX, maxY, minZ, r, g, b, a, lineWidth, cameraPos);

        // Vertical pillars (4 edges)
        edge(consumer, pose, mat, minX, minY, minZ, minX, maxY, minZ, r, g, b, a, lineWidth, cameraPos);
        edge(consumer, pose, mat, maxX, minY, minZ, maxX, maxY, minZ, r, g, b, a, lineWidth, cameraPos);
        edge(consumer, pose, mat, maxX, minY, maxZ, maxX, maxY, maxZ, r, g, b, a, lineWidth, cameraPos);
        edge(consumer, pose, mat, minX, minY, maxZ, minX, maxY, maxZ, r, g, b, a, lineWidth, cameraPos);
    }

    private static void edge(
            VertexConsumer consumer,
            PoseStack.Pose pose,
            org.joml.Matrix4f mat,
            double x1, double y1, double z1,
            double x2, double y2, double z2,
            float r, float g, float b, float a,
            float lineWidth,
            Vec3 cameraPos
    ) {
        double dx = Math.abs(x2 - x1);
        double dy = Math.abs(y2 - y1);
        double dz = Math.abs(z2 - z1);

        float nxA;
        float nyA;
        float nzA;
        float nxB;
        float nyB;
        float nzB;

        if (dy < 1.0e-6 && dz < 1.0e-6) {
            nxA = 0.0F;
            nyA = 1.0F;
            nzA = 0.0F;
            nxB = 0.0F;
            nyB = 0.0F;
            nzB = 1.0F;
        } else if (dx < 1.0e-6 && dz < 1.0e-6) {
            nxA = 1.0F;
            nyA = 0.0F;
            nzA = 0.0F;
            nxB = 0.0F;
            nyB = 0.0F;
            nzB = 1.0F;
        } else {
            nxA = 0.0F;
            nyA = 1.0F;
            nzA = 0.0F;
            nxB = 1.0F;
            nyB = 0.0F;
            nzB = 0.0F;
        }

        double mx = (x1 + x2) * 0.5;
        double my = (y1 + y2) * 0.5;
        double mz = (z1 + z2) * 0.5;
        double vx = cameraPos.x - mx;
        double vy = cameraPos.y - my;
        double vz = cameraPos.z - mz;

        double dotA = nxA * vx + nyA * vy + nzA * vz;
        double dotB = nxB * vx + nyB * vy + nzB * vz;

        if (Math.abs(dotA) < 1.0e-4 && Math.abs(dotB) < 1.0e-4) {
            emitEdge(consumer, pose, mat, x1, y1, z1, x2, y2, z2, r, g, b, a, lineWidth, nxA, nyA, nzA);
            emitEdge(consumer, pose, mat, x1, y1, z1, x2, y2, z2, r, g, b, a, lineWidth, nxB, nyB, nzB);
            return;
        }

        if (Math.abs(dotA) >= Math.abs(dotB)) {
            float sign = dotA < 0.0 ? -1.0F : 1.0F;
            emitEdge(consumer, pose, mat, x1, y1, z1, x2, y2, z2, r, g, b, a, lineWidth, nxA * sign, nyA * sign, nzA * sign);
        } else {
            float sign = dotB < 0.0 ? -1.0F : 1.0F;
            emitEdge(consumer, pose, mat, x1, y1, z1, x2, y2, z2, r, g, b, a, lineWidth, nxB * sign, nyB * sign, nzB * sign);
        }
    }

    private static void emitEdge(
            VertexConsumer consumer,
            PoseStack.Pose pose,
            org.joml.Matrix4f mat,
            double x1, double y1, double z1,
            double x2, double y2, double z2,
            float r, float g, float b, float a,
            float lineWidth,
            float nx, float ny, float nz
    ) {
        consumer.addVertex(mat, (float) x1, (float) y1, (float) z1)
                .setColor(r, g, b, a)
                .setNormal(pose, nx, ny, nz)
                .setLineWidth(lineWidth);
        consumer.addVertex(mat, (float) x2, (float) y2, (float) z2)
                .setColor(r, g, b, a)
                .setNormal(pose, nx, ny, nz)
                .setLineWidth(lineWidth);
    }
}
