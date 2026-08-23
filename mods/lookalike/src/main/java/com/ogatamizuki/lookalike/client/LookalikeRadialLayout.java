package com.ogatamizuki.lookalike.client;

final class LookalikeRadialLayout {
    static final int INNER_RADIUS = 22;
    static final int OUTER_RADIUS = 86;
    static final int OUTER_RING_THICKNESS = 2;
    static final int SLICE_OUTER_RADIUS = OUTER_RADIUS - OUTER_RING_THICKNESS;
    static final int LABEL_RADIUS = 52;
    static final int FACE_SIZE = 14;
    static final int CENTER_Y_OFFSET = -18;
    static final int HOVER_MARGIN = 6;

    static final int HUB_FILL = 0xF010141C;
    static final int HUB_RING = 0xFF56CFE1;
    static final int OUTER_RING = 0xAAFFFFFF;

    private LookalikeRadialLayout() {
    }

    static int sliceFillColor(int index, boolean resetEntry, boolean hovered) {
        if (resetEntry) {
            return hovered ? 0xE04A6FA8 : 0xD0243558;
        }
        if (hovered) {
            return 0xE02E7D62;
        }

        int shade = (index * 37) % 5;
        return switch (shade) {
            case 0 -> 0xD0143A33;
            case 1 -> 0xD0164238;
            case 2 -> 0xD018463C;
            case 3 -> 0xD01A4A40;
            default -> 0xD01C4E44;
        };
    }

    /** 角度を [0, 360) の範囲に正規化する。Session と SliceDrawer で共有。 */
    static float normalizeDegrees(float degrees) {
        float normalized = degrees % 360.0F;
        if (normalized < 0.0F) {
            normalized += 360.0F;
        }
        return normalized;
    }
}
