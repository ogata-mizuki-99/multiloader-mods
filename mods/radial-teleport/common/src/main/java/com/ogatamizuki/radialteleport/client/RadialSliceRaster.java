package com.ogatamizuki.radialteleport.client;

import com.mojang.blaze3d.platform.NativeImage;
import com.ogatamizuki.radialteleport.TeleportDestination;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.List;

final class RadialSliceRaster {
    private RadialSliceRaster() {
    }

    static void clearTransparent(NativeImage image) {
        int size = image.getWidth();
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                image.setPixel(x, y, 0);
            }
        }
    }

    static void bakeBase(NativeImage image, List<TeleportDestination> destinations) {
        clearTransparent(image);
        int center = RadialMenuLayout.textureCenter();
        int sliceCount = destinations.size();
        if (sliceCount <= 0) {
            return;
        }

        float sliceAngle = 360.0F / sliceCount;
        for (int i = 0; i < sliceCount; i++) {
            float start = -90.0F + sliceAngle * i;
            float end = start + sliceAngle;
            fillAnnularSector(
                    image,
                    center,
                    center,
                    RadialMenuLayout.INNER_RADIUS,
                    RadialMenuLayout.SLICE_OUTER_RADIUS,
                    start,
                    end,
                    RadialMenuLayout.sliceFillColor(destinations.get(i), i, false)
            );
        }

        strokeCircle(image, center, center, RadialMenuLayout.OUTER_RADIUS, RadialMenuLayout.OUTER_RING_THICKNESS, RadialMenuLayout.OUTER_RING);
        fillRing(image, center, center, RadialMenuLayout.INNER_RADIUS - 1, RadialMenuLayout.INNER_RADIUS + 1, RadialMenuLayout.HUB_RING);
        fillCircle(image, center, center, RadialMenuLayout.INNER_RADIUS - 2, RadialMenuLayout.HUB_FILL);
    }

    static void bakeHover(NativeImage image, List<TeleportDestination> destinations, int hoveredIndex) {
        clearTransparent(image);
        if (hoveredIndex < 0 || hoveredIndex >= destinations.size()) {
            return;
        }

        int center = RadialMenuLayout.textureCenter();
        float sliceAngle = 360.0F / destinations.size();
        float start = -90.0F + sliceAngle * hoveredIndex;
        float end = start + sliceAngle;
        fillAnnularSector(
                image,
                center,
                center,
                RadialMenuLayout.INNER_RADIUS,
                RadialMenuLayout.SLICE_OUTER_RADIUS,
                start,
                end,
                RadialMenuLayout.sliceFillColor(destinations.get(hoveredIndex), hoveredIndex, true)
        );
    }

    static void drawMenu(
            GuiGraphicsExtractor graphics,
            int centerX,
            int centerY,
            List<TeleportDestination> destinations,
            int hoveredIndex
    ) {
        int sliceCount = destinations.size();
        if (sliceCount <= 0) {
            return;
        }

        float sliceAngle = 360.0F / sliceCount;
        for (int i = 0; i < sliceCount; i++) {
            if (i == hoveredIndex) {
                continue;
            }
            float start = -90.0F + sliceAngle * i;
            float end = start + sliceAngle;
            fillAnnularSectorOnGraphics(
                    graphics,
                    centerX,
                    centerY,
                    RadialMenuLayout.INNER_RADIUS,
                    RadialMenuLayout.SLICE_OUTER_RADIUS,
                    start,
                    end,
                    RadialMenuLayout.sliceFillColor(destinations.get(i), i, false)
            );
        }

        if (hoveredIndex >= 0 && hoveredIndex < sliceCount) {
            float start = -90.0F + sliceAngle * hoveredIndex;
            float end = start + sliceAngle;
            fillAnnularSectorOnGraphics(
                    graphics,
                    centerX,
                    centerY,
                    RadialMenuLayout.INNER_RADIUS,
                    RadialMenuLayout.SLICE_OUTER_RADIUS,
                    start,
                    end,
                    RadialMenuLayout.sliceFillColor(destinations.get(hoveredIndex), hoveredIndex, true)
            );
        }

        strokeCircleOnGraphics(
                graphics,
                centerX,
                centerY,
                RadialMenuLayout.OUTER_RADIUS,
                RadialMenuLayout.OUTER_RING_THICKNESS,
                RadialMenuLayout.OUTER_RING
        );
        fillRingOnGraphics(
                graphics,
                centerX,
                centerY,
                RadialMenuLayout.INNER_RADIUS - 1,
                RadialMenuLayout.INNER_RADIUS + 1,
                RadialMenuLayout.HUB_RING
        );
        fillCircleOnGraphics(
                graphics,
                centerX,
                centerY,
                RadialMenuLayout.INNER_RADIUS - 2,
                RadialMenuLayout.HUB_FILL
        );
    }

    private static void fillAnnularSectorOnGraphics(
            GuiGraphicsExtractor graphics,
            int centerX,
            int centerY,
            int innerRadius,
            int outerRadius,
            float startDegrees,
            float endDegrees,
            int color
    ) {
        int minY = centerY - outerRadius;
        int maxY = centerY + outerRadius;
        long innerRadiusSq = (long) innerRadius * innerRadius;
        long outerRadiusSq = (long) outerRadius * outerRadius;

        for (int y = minY; y <= maxY; y++) {
            int dy = y - centerY;
            long dySq = (long) dy * dy;
            if (dySq > outerRadiusSq) {
                continue;
            }

            int outerHalf = (int) Math.sqrt(Math.max(0L, outerRadiusSq - dySq));
            int xMin = centerX - outerHalf;
            int xMax = centerX + outerHalf;
            int x = xMin;

            while (x <= xMax) {
                while (x <= xMax && !isPixelInAnnularSector(x, y, centerX, centerY, innerRadiusSq, outerRadiusSq, startDegrees, endDegrees)) {
                    x++;
                }
                int spanStart = x;
                while (x <= xMax && isPixelInAnnularSector(x, y, centerX, centerY, innerRadiusSq, outerRadiusSq, startDegrees, endDegrees)) {
                    x++;
                }
                if (spanStart < x) {
                    graphics.fill(spanStart, y, x, y + 1, color);
                }
            }
        }
    }

    private static boolean isPixelInAnnularSector(
            int x,
            int y,
            int centerX,
            int centerY,
            long innerRadiusSq,
            long outerRadiusSq,
            float startDegrees,
            float endDegrees
    ) {
        int dx = x - centerX;
        int dy = y - centerY;
        long distSq = (long) dx * dx + (long) dy * dy;
        if (distSq < innerRadiusSq || distSq > outerRadiusSq) {
            return false;
        }
        return isAngleInRange(dx, dy, startDegrees, endDegrees);
    }

    private static void strokeCircleOnGraphics(
            GuiGraphicsExtractor graphics,
            int centerX,
            int centerY,
            int radius,
            int thickness,
            int color
    ) {
        if (thickness <= 0) {
            return;
        }
        fillRingOnGraphics(graphics, centerX, centerY, radius - thickness + 1, radius + 1, color);
    }

    private static void fillCircleOnGraphics(
            GuiGraphicsExtractor graphics,
            int centerX,
            int centerY,
            int radius,
            int color
    ) {
        int minY = centerY - radius;
        int maxY = centerY + radius;
        for (int y = minY; y <= maxY; y++) {
            int dy = y - centerY;
            int span = (int) Math.sqrt(Math.max(0, radius * radius - (long) dy * dy));
            graphics.fill(centerX - span, y, centerX + span + 1, y + 1, color);
        }
    }

    private static void fillRingOnGraphics(
            GuiGraphicsExtractor graphics,
            int centerX,
            int centerY,
            int innerRadius,
            int outerRadius,
            int color
    ) {
        int minY = centerY - outerRadius;
        int maxY = centerY + outerRadius;
        for (int y = minY; y <= maxY; y++) {
            int dy = y - centerY;
            int outerSpan = (int) Math.sqrt(Math.max(0, outerRadius * outerRadius - (long) dy * dy));
            int innerSpan = innerRadius > 0 && Math.abs(dy) < innerRadius
                    ? (int) Math.sqrt(Math.max(0, innerRadius * innerRadius - (long) dy * dy))
                    : 0;
            if (outerSpan <= innerSpan) {
                continue;
            }

            graphics.fill(centerX - outerSpan, y, centerX - innerSpan, y + 1, color);
            graphics.fill(centerX + innerSpan + 1, y, centerX + outerSpan + 1, y + 1, color);
        }
    }

    private static void fillAnnularSector(
            NativeImage image,
            int centerX,
            int centerY,
            int innerRadius,
            int outerRadius,
            float startDegrees,
            float endDegrees,
            int color
    ) {
        int minY = Math.max(0, centerY - outerRadius);
        int maxY = Math.min(image.getHeight() - 1, centerY + outerRadius);
        long innerRadiusSq = (long) innerRadius * innerRadius;
        long outerRadiusSq = (long) outerRadius * outerRadius;

        for (int y = minY; y <= maxY; y++) {
            int dy = y - centerY;
            int outerHalf = (int) Math.sqrt(Math.max(0, outerRadiusSq - (long) dy * dy));
            int xMin = Math.max(0, centerX - outerHalf);
            int xMax = Math.min(image.getWidth() - 1, centerX + outerHalf);

            for (int x = xMin; x <= xMax; x++) {
                int dx = x - centerX;
                long distSq = (long) dx * dx + (long) dy * dy;
                if (distSq < innerRadiusSq || distSq > outerRadiusSq) {
                    continue;
                }
                if (!isAngleInRange(dx, dy, startDegrees, endDegrees)) {
                    continue;
                }
                image.setPixel(x, y, color);
            }
        }
    }

    private static void strokeCircle(NativeImage image, int centerX, int centerY, int radius, int thickness, int color) {
        if (thickness <= 0) {
            return;
        }
        fillRing(image, centerX, centerY, radius - thickness + 1, radius + 1, color);
    }

    private static void fillCircle(NativeImage image, int centerX, int centerY, int radius, int color) {
        int minY = Math.max(0, centerY - radius);
        int maxY = Math.min(image.getHeight() - 1, centerY + radius);
        for (int y = minY; y <= maxY; y++) {
            int dy = y - centerY;
            int span = (int) Math.sqrt(Math.max(0, radius * radius - (long) dy * dy));
            int xMin = Math.max(0, centerX - span);
            int xMax = Math.min(image.getWidth() - 1, centerX + span);
            for (int x = xMin; x <= xMax; x++) {
                image.setPixel(x, y, color);
            }
        }
    }

    private static void fillRing(NativeImage image, int centerX, int centerY, int innerRadius, int outerRadius, int color) {
        int minY = Math.max(0, centerY - outerRadius);
        int maxY = Math.min(image.getHeight() - 1, centerY + outerRadius);
        for (int y = minY; y <= maxY; y++) {
            int dy = y - centerY;
            int outerSpan = (int) Math.sqrt(Math.max(0, outerRadius * outerRadius - (long) dy * dy));
            int innerSpan = innerRadius > 0 && Math.abs(dy) < innerRadius
                    ? (int) Math.sqrt(Math.max(0, innerRadius * innerRadius - (long) dy * dy))
                    : 0;
            if (outerSpan <= innerSpan) {
                continue;
            }

            int leftOuter = Math.max(0, centerX - outerSpan);
            int leftInner = Math.max(0, centerX - innerSpan);
            int rightInner = Math.min(image.getWidth() - 1, centerX + innerSpan);
            int rightOuter = Math.min(image.getWidth() - 1, centerX + outerSpan);

            for (int x = leftOuter; x < leftInner; x++) {
                image.setPixel(x, y, color);
            }
            for (int x = rightInner + 1; x <= rightOuter; x++) {
                image.setPixel(x, y, color);
            }
        }
    }

    private static boolean isAngleInRange(int dx, int dy, float startDegrees, float endDegrees) {
        if (endDegrees - startDegrees >= 359.9F) {
            return true;
        }

        float angle = (float) Math.toDegrees(Math.atan2(dy, dx));
        float normalized = normalizeDegrees(angle);
        float start = normalizeDegrees(startDegrees);
        float end = normalizeDegrees(endDegrees);

        if (start <= end) {
            return normalized >= start && normalized < end;
        }
        return normalized >= start || normalized < end;
    }

    private static float normalizeDegrees(float degrees) {
        float normalized = degrees % 360.0F;
        if (normalized < 0.0F) {
            normalized += 360.0F;
        }
        return normalized;
    }
}
