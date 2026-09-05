package com.ogatamizuki.privatechest.client;

import net.minecraft.core.Direction;

/**
 * Converts atlas pixel coordinates into {@code texOffs} for {@link net.minecraft.client.model.geom.builders.CubeListBuilder}.
 * <p>
 * Model faces use 16 units per block; each 64×64 atlas tile spans 16 UV units.
 * Cube UV layout depends on face direction, so pixel origin alone is not enough.
 */
final class LockerAtlasUv {
    private static final float UV_UNITS_PER_PIXEL = 16.0F / LockerBlockEntityRenderer.TILE_SIZE;
    /**
     * Horizontal (U) tweak for thin {@link Direction#SOUTH} door panels (0 = correct by eye).
     */
    private static final int DOOR_SOUTH_U_CORRECTION = 0;

    private LockerAtlasUv() {
    }

    static int u(Direction face, float width, float height, float depth, int pixelU) {
        float tileU = pixelU * UV_UNITS_PER_PIXEL;
        return Math.round(switch (face) {
            case WEST -> tileU;
            case EAST -> tileU - depth - width;
            case NORTH -> tileU - depth;
            case SOUTH -> tileU - depth - width - depth;
            case DOWN -> tileU - depth;
            case UP -> tileU - depth - width;
        });
    }

    static int v(Direction face, float width, float height, float depth, int pixelV) {
        float tileV = pixelV * UV_UNITS_PER_PIXEL;
        return Math.round(switch (face) {
            case WEST, EAST, NORTH, SOUTH, UP -> tileV - depth;
            case DOWN -> tileV;
        });
    }

    static int ventU() {
        return doorSouthU(LockerBlockEntityRenderer.UV_VENT_PIXEL_U);
    }

    static int ventV() {
        return doorSouthV();
    }

    static int knobU() {
        return doorSouthU(LockerBlockEntityRenderer.UV_KNOB_PIXEL_U);
    }

    static int knobV() {
        return doorSouthV();
    }

    private static int doorSouthU(int pixelU) {
        float width = 16.0F;
        float depth = LockerBlockEntityRenderer.WALL_THICKNESS;
        return u(Direction.SOUTH, width, width, depth, pixelU) + DOOR_SOUTH_U_CORRECTION;
    }

    private static int doorSouthV() {
        float depth = LockerBlockEntityRenderer.WALL_THICKNESS;
        return v(Direction.SOUTH, 16.0F, 16.0F, depth, LockerBlockEntityRenderer.UV_PIXEL_V);
    }
}
