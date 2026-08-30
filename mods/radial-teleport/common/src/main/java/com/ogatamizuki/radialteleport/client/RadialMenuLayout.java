package com.ogatamizuki.radialteleport.client;

import com.ogatamizuki.radialteleport.TeleportDestination;

final class RadialMenuLayout {
    static final int INNER_RADIUS = 22;
    static final int OUTER_RADIUS = 86;
    static final int OUTER_RING_THICKNESS = 2;
    static final int SLICE_OUTER_RADIUS = OUTER_RADIUS - OUTER_RING_THICKNESS;
    static final int LABEL_RADIUS = 52;
    static final int FACE_SIZE = 14;
    static final int CENTER_Y_OFFSET = -18;
    static final int HOVER_MARGIN = 6;
    static final int TEXTURE_MARGIN = 2;
    static final int TEXTURE_SIZE = (OUTER_RADIUS + TEXTURE_MARGIN) * 2;

    static final int HUB_FILL = 0xF010141C;
    static final int HUB_RING = 0xFF56CFE1;
    static final int OUTER_RING = 0xAAFFFFFF;
    static final int LABEL_COLOR = 0xFFF4F7FB;
    static final int LABEL_HOVER = 0xFFFFF3A6;
    static final int WAYPOINT_LABEL_COLOR = 0xFFFFD080;
    static final int WAYPOINT_LABEL_HOVER = 0xFFFFF3A6;

    private RadialMenuLayout() {
    }

    static int textureCenter() {
        return TEXTURE_SIZE / 2;
    }

    static int sliceFillColor(TeleportDestination destination, int index, boolean hovered) {
        if (destination.kind() == TeleportDestination.KIND_SPAWN) {
            return hovered ? 0xE04A6FA8 : 0xD0243558;
        }
        if (destination.kind() == TeleportDestination.KIND_WAYPOINT) {
            return hovered ? 0xE08A5A28 : 0xD05A3818;
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
}
