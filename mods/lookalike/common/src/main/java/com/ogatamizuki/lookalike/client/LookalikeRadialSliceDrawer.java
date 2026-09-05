package com.ogatamizuki.lookalike.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.List;

final class LookalikeRadialSliceDrawer {
    private LookalikeRadialSliceDrawer() {
    }

    static void drawMenu(
            GuiGraphicsExtractor graphics,
            int centerX,
            int centerY,
            List<LookalikeRadialSession.MenuEntry> entries,
            int hoveredIndex
    ) {
        int sliceCount = entries.size();
        if (sliceCount <= 0) {
            return;
        }

        float sliceAngle = 360.0F / sliceCount;
        for (int i = 0; i < sliceCount; i++) {
            if (i == hoveredIndex) {
                continue;
            }
            drawSlice(graphics, centerX, centerY, sliceAngle, i, false);
        }

        if (hoveredIndex >= 0 && hoveredIndex < sliceCount) {
            drawSlice(graphics, centerX, centerY, sliceAngle, hoveredIndex, true);
        }

        strokeCircle(
                graphics,
                centerX,
                centerY,
                LookalikeRadialLayout.OUTER_RADIUS,
                LookalikeRadialLayout.OUTER_RING_THICKNESS,
                LookalikeRadialLayout.OUTER_RING
        );
        fillRing(
                graphics,
                centerX,
                centerY,
                LookalikeRadialLayout.INNER_RADIUS - 1,
                LookalikeRadialLayout.INNER_RADIUS + 1,
                LookalikeRadialLayout.HUB_RING
        );
        fillCircle(
                graphics,
                centerX,
                centerY,
                LookalikeRadialLayout.INNER_RADIUS - 2,
                LookalikeRadialLayout.HUB_FILL
        );
    }

    private static void drawSlice(
            GuiGraphicsExtractor graphics,
            int centerX,
            int centerY,
            float sliceAngle,
            int index,
            boolean hovered
    ) {
        float start = -90.0F + sliceAngle * index;
        float end = start + sliceAngle;
        boolean resetEntry = index == 0;
        fillAnnularSector(
                graphics,
                centerX,
                centerY,
                LookalikeRadialLayout.INNER_RADIUS,
                LookalikeRadialLayout.SLICE_OUTER_RADIUS,
                start,
                end,
                LookalikeRadialLayout.sliceFillColor(index, resetEntry, hovered)
        );
    }

    private static void fillAnnularSector(
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

    private static void strokeCircle(
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
        fillRing(graphics, centerX, centerY, radius - thickness + 1, radius + 1, color);
    }

    private static void fillCircle(
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

    private static void fillRing(
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

    private static boolean isAngleInRange(int dx, int dy, float startDegrees, float endDegrees) {
        if (endDegrees - startDegrees >= 359.9F) {
            return true;
        }

        float angle = (float) Math.toDegrees(Math.atan2(dy, dx));
        float normalized = LookalikeRadialLayout.normalizeDegrees(angle);
        float start = LookalikeRadialLayout.normalizeDegrees(startDegrees);
        float end = LookalikeRadialLayout.normalizeDegrees(endDegrees);

        if (start <= end) {
            return normalized >= start && normalized < end;
        }
        return normalized >= start || normalized < end;
    }

}
