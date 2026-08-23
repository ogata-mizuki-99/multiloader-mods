package com.ogatamizuki.instantstructure.client;

import com.ogatamizuki.instantstructure.InstantStructureMod;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

public class ClientSelectionRegistry {
    public static boolean hasStart = false;
    public static boolean hasBoth = false;
    public static boolean confirmed = false;
    public static BlockPos pos1 = null;
    public static BlockPos pos2 = null;

    /** @deprecated use {@link #hasBoth} */
    @Deprecated
    public static boolean active;

    public static void apply(
            boolean hasStart,
            boolean hasBoth,
            boolean confirmed,
            BlockPos pos1,
            BlockPos pos2
    ) {
        ClientSelectionRegistry.hasStart = hasStart;
        ClientSelectionRegistry.hasBoth = hasBoth;
        ClientSelectionRegistry.confirmed = confirmed;
        ClientSelectionRegistry.active = hasBoth;
        ClientSelectionRegistry.pos1 = pos1;
        ClientSelectionRegistry.pos2 = pos2;
    }

    public static void clear() {
        apply(false, false, false, null, null);
    }

    public static boolean isHoldingMarker(Minecraft mc) {
        if (mc.player == null) {
            return false;
        }
        return mc.player.getMainHandItem().is(InstantStructureMod.STRUCTURE_MARKER.get())
                || mc.player.getOffhandItem().is(InstantStructureMod.STRUCTURE_MARKER.get());
    }

    public static BlockPos resolvePreviewEnd(Minecraft mc) {
        if (hasBoth && pos2 != null) {
            return pos2;
        }
        HitResult hit = mc.hitResult;
        if (hit != null && hit.getType() == HitResult.Type.BLOCK) {
            return ((BlockHitResult) hit).getBlockPos();
        }
        return null;
    }

    public static SelectionBounds currentBounds(Minecraft mc) {
        if (!hasStart || pos1 == null) {
            return null;
        }
        BlockPos end = resolvePreviewEnd(mc);
        if (end == null) {
            return null;
        }
        return SelectionBounds.from(pos1, end);
    }
}
