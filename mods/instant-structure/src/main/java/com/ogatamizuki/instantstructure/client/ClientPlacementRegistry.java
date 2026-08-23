package com.ogatamizuki.instantstructure.client;

import com.ogatamizuki.instantstructure.InstantStructureMod;
import com.ogatamizuki.instantstructure.PlacementTransform;
import com.ogatamizuki.instantstructure.PreviewBlockEntry;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.Collections;
import java.util.List;

public class ClientPlacementRegistry {
    public static boolean active = false;
    public static boolean tentativelyConfirmed = false;
    public static String category = "";
    public static String templateName = "";
    public static int sizeX = 1;
    public static int sizeY = 1;
    public static int sizeZ = 1;
    public static int rotation = 0;
    public static boolean mirrorLeftRight = false;
    public static boolean mirrorFrontBack = false;
    public static int placementYOffset = 0;
    public static BlockPos lockedAnchor = null;
    public static BlockPos lockedPlacementOrigin = null;
    public static List<PreviewBlockEntry> previewBlocks = Collections.emptyList();
    public static PlacementPreviewCache previewCache = null;

    public static PlacementTransform placementTransform() {
        return new PlacementTransform(rotation, mirrorLeftRight, mirrorFrontBack);
    }

    public static void reset() {
        active = false;
        tentativelyConfirmed = false;
        category = "";
        templateName = "";
        sizeX = 1;
        sizeY = 1;
        sizeZ = 1;
        rotation = 0;
        mirrorLeftRight = false;
        mirrorFrontBack = false;
        placementYOffset = 0;
        lockedAnchor = null;
        lockedPlacementOrigin = null;
        previewBlocks = Collections.emptyList();
        previewCache = null;
    }

    public static void clearTentative() {
        tentativelyConfirmed = false;
        lockedAnchor = null;
        lockedPlacementOrigin = null;
        previewCache = null;
    }

    public static int previewBlockCount() {
        return previewBlocks.size();
    }

    public static boolean isLargePreview() {
        return PreviewPerformancePolicy.isLargePreview(previewBlockCount());
    }

    /** 大規模: 仮確定前は枠のみ。小規模: 常に詳細ゴースト。 */
    public static boolean shouldRenderDetailedGhosts() {
        if (previewBlocks.isEmpty()) {
            return false;
        }
        if (!isLargePreview()) {
            return true;
        }
        return tentativelyConfirmed && previewCache != null;
    }

    public static boolean isSimplifiedGhostMode() {
        return isLargePreview() && !tentativelyConfirmed;
    }

    public static boolean isHoldingBuilder(Minecraft mc) {
        if (mc.player == null) {
            return false;
        }
        return mc.player.getMainHandItem().is(InstantStructureMod.INSTANT_BUILDER.get())
                || mc.player.getOffhandItem().is(InstantStructureMod.INSTANT_BUILDER.get());
    }

    public static BlockPos resolveCrosshairAnchor(Minecraft mc) {
        HitResult hit = mc.hitResult;
        if (hit == null || hit.getType() != HitResult.Type.BLOCK) {
            return null;
        }
        BlockPos base = ((BlockHitResult) hit).getBlockPos().relative(((BlockHitResult) hit).getDirection());
        return base.offset(0, placementYOffset, 0);
    }

    public static BlockPos resolvePlacementOrigin(Minecraft mc) {
        if (tentativelyConfirmed && lockedPlacementOrigin != null) {
            return lockedPlacementOrigin;
        }
        BlockPos anchor = resolveCrosshairAnchor(mc);
        if (anchor == null) {
            return null;
        }
        return placementTransform().toPlacementOrigin(anchor, sizeX, sizeY, sizeZ);
    }

    public static boolean lockTentativeAnchor(Minecraft mc) {
        BlockPos anchor = resolveCrosshairAnchor(mc);
        if (anchor == null) {
            return false;
        }
        lockedAnchor = anchor;
        lockedPlacementOrigin = placementTransform().toPlacementOrigin(anchor, sizeX, sizeY, sizeZ);
        tentativelyConfirmed = true;
        if (isLargePreview() && lockedPlacementOrigin != null && mc.level != null) {
            previewCache = PlacementPreviewCache.build(
                    lockedPlacementOrigin,
                    placementTransform(),
                    previewBlocks,
                    mc.level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.BLOCK)
            );
        }
        return true;
    }
}
