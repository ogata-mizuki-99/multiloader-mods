package com.ogatamizuki.radialteleport;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Resolves a standing position with two blocks of headroom near the requested coordinates.
 */
public final class SafeTeleportPosition {
    private static final int HORIZONTAL_RADIUS = 3;
    private static final int VERTICAL_RANGE = 6;

    private SafeTeleportPosition() {
    }

    public record Resolved(double x, double y, double z) {
    }

    public static Resolved resolve(ServerLevel level, double x, double y, double z) {
        BlockPos origin = BlockPos.containing(x, y, z);
        BlockPos safeFeet = findSafeFeet(level, origin);
        if (safeFeet != null) {
            return new Resolved(safeFeet.getX() + 0.5D, safeFeet.getY(), safeFeet.getZ() + 0.5D);
        }
        return new Resolved(x, y, z);
    }

    private static BlockPos findSafeFeet(ServerLevel level, BlockPos origin) {
        if (isSafeStanding(level, origin)) {
            return origin;
        }

        for (int radius = 1; radius <= HORIZONTAL_RADIUS; radius++) {
            for (int dy = -2; dy <= VERTICAL_RANGE; dy++) {
                for (int dx = -radius; dx <= radius; dx++) {
                    for (int dz = -radius; dz <= radius; dz++) {
                        if (Math.abs(dx) != radius && Math.abs(dz) != radius) {
                            continue;
                        }
                        BlockPos candidate = origin.offset(dx, dy, dz);
                        if (isSafeStanding(level, candidate)) {
                            return candidate;
                        }
                    }
                }
            }
        }
        return null;
    }

    private static boolean isSafeStanding(ServerLevel level, BlockPos feet) {
        if (!level.isLoaded(feet)) {
            return false;
        }
        int minY = level.getMinY();
        int maxY = level.getMaxY() - 2;
        if (feet.getY() < minY || feet.getY() > maxY) {
            return false;
        }

        BlockPos ground = feet.below();
        BlockState groundState = level.getBlockState(ground);
        if (!hasSolidTopSurface(level, ground, groundState)) {
            return false;
        }

        return isPassableForPlayer(level, feet) && isPassableForPlayer(level, feet.above());
    }

    private static boolean hasSolidTopSurface(ServerLevel level, BlockPos pos, BlockState state) {
        if (state.isAir()) {
            return false;
        }
        VoxelShape collision = state.getCollisionShape(level, pos);
        return !collision.isEmpty();
    }

    private static boolean isPassableForPlayer(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.isAir()) {
            return true;
        }
        VoxelShape collision = state.getCollisionShape(level, pos);
        return collision.isEmpty();
    }
}
