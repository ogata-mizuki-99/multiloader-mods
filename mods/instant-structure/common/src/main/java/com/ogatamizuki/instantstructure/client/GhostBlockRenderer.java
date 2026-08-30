package com.ogatamizuki.instantstructure.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.ogatamizuki.instantstructure.BlockStateParserSupport;
import com.ogatamizuki.instantstructure.PlacementBounds;
import com.ogatamizuki.instantstructure.PlacementTransform;
import com.ogatamizuki.instantstructure.PreviewBlockEntry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockQuadOutput;
import net.minecraft.client.renderer.block.BlockStateModelSet;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.block.MovingBlockRenderState;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.ARGB;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public final class GhostBlockRenderer {
    private static final int GHOST_COLOR_MULTIPLIER = ARGB.colorFromFloat(0.55F, 1.0F, 1.0F, 1.0F);

    private static ModelBlockRenderer sharedRenderer;
    private static boolean sharedAmbientOcclusion;
    private static boolean sharedCutoutLeaves;

    private GhostBlockRenderer() {
    }

    public static void renderBlockModels(
            PoseStack poseStack,
            MultiBufferSource.BufferSource bufferSource,
            BlockPos origin,
            PlacementTransform transform,
            List<PreviewBlockEntry> blocks
    ) {
        if (blocks.isEmpty()) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) {
            return;
        }

        var blockLookup = level.registryAccess().lookupOrThrow(Registries.BLOCK);
        ModelBlockRenderer blockRenderer = getSharedRenderer(mc);
        BlockStateModelSet modelSet = mc.getModelManager().getBlockStateModelSet();
        boolean cutoutLeaves = mc.options.cutoutLeaves().get();
        VertexConsumer consumer = bufferSource.getBuffer(RenderTypes.translucentMovingBlock());

        BlockQuadOutput output = createGhostOutput(poseStack, consumer);

        for (PreviewBlockEntry entry : blocks) {
            BlockState rawState = BlockStateParserSupport.parse(blockLookup, entry.blockState());
            if (rawState.isAir()) {
                continue;
            }

            BlockState state = transform.transformBlockState(rawState);
            BlockPos worldPos = transform.toWorldPos(origin, new BlockPos(entry.x(), entry.y(), entry.z()));
            renderBlockAt(poseStack, blockRenderer, modelSet, cutoutLeaves, output, worldPos, state, level);
        }

        bufferSource.endBatch(RenderTypes.translucentMovingBlock());
    }

    public static void renderCached(
            PoseStack poseStack,
            MultiBufferSource.BufferSource bufferSource,
            PlacementPreviewCache cache,
            Vec3 cameraPos
    ) {
        if (cache.blocks().isEmpty()) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) {
            return;
        }

        ModelBlockRenderer blockRenderer = getSharedRenderer(mc);
        BlockStateModelSet modelSet = mc.getModelManager().getBlockStateModelSet();
        boolean cutoutLeaves = mc.options.cutoutLeaves().get();
        VertexConsumer consumer = bufferSource.getBuffer(RenderTypes.translucentMovingBlock());
        BlockQuadOutput output = createGhostOutput(poseStack, consumer);

        double radiusSq = PreviewPerformancePolicy.detailedGhostRadiusSquared();
        
        record DistBlock(PlacementPreviewCache.CachedBlock block, double distSq) {}
        
        List<DistBlock> nearBlocks = new java.util.ArrayList<>();
        for (PlacementPreviewCache.CachedBlock block : cache.blocks()) {
            BlockPos worldPos = block.worldPos();
            double distSq = cameraPos.distanceToSqr(
                    worldPos.getX() + 0.5,
                    worldPos.getY() + 0.5,
                    worldPos.getZ() + 0.5
            );
            if (distSq <= radiusSq) {
                nearBlocks.add(new DistBlock(block, distSq));
            }
        }

        int maxGhosts = PreviewPerformancePolicy.MAX_DETAILED_GHOSTS_PER_FRAME;
        if (nearBlocks.size() > maxGhosts) {
            nearBlocks.sort((b1, b2) -> Double.compare(b1.distSq, b2.distSq));
        }

        int rendered = 0;
        for (DistBlock entry : nearBlocks) {
            if (rendered >= maxGhosts) {
                break;
            }
            PlacementPreviewCache.CachedBlock block = entry.block;
            renderBlockAt(poseStack, blockRenderer, modelSet, cutoutLeaves, output, block.worldPos(), block.state(), level);
            rendered++;
        }

        bufferSource.endBatch(RenderTypes.translucentMovingBlock());
    }

    private static BlockQuadOutput createGhostOutput(PoseStack poseStack, VertexConsumer consumer) {
        return (x, y, z, quad, instance) -> {
            instance.multiplyColor(GHOST_COLOR_MULTIPLIER);
            poseStack.pushPose();
            poseStack.translate(x, y, z);
            consumer.putBakedQuad(poseStack.last(), quad, instance);
            poseStack.popPose();
        };
    }

    private static void renderBlockAt(
            PoseStack poseStack,
            ModelBlockRenderer blockRenderer,
            BlockStateModelSet modelSet,
            boolean cutoutLeaves,
            BlockQuadOutput output,
            BlockPos worldPos,
            BlockState state,
            ClientLevel level
    ) {
        MovingBlockRenderState movingState = createMovingBlock(worldPos, state, level);
        BlockStateModel model = modelSet.get(state);

        poseStack.pushPose();
        poseStack.translate(worldPos.getX(), worldPos.getY(), worldPos.getZ());
        blockRenderer.tesselateBlock(
                output,
                0.0F,
                0.0F,
                0.0F,
                movingState,
                worldPos,
                state,
                model,
                state.getSeed(worldPos)
        );
        poseStack.popPose();
    }

    private static ModelBlockRenderer getSharedRenderer(Minecraft mc) {
        boolean ambientOcclusion = mc.options.ambientOcclusion().get();
        boolean cutoutLeaves = mc.options.cutoutLeaves().get();
        if (sharedRenderer == null
                || sharedAmbientOcclusion != ambientOcclusion
                || sharedCutoutLeaves != cutoutLeaves) {
            sharedRenderer = new ModelBlockRenderer(ambientOcclusion, true, mc.getBlockColors());
            sharedAmbientOcclusion = ambientOcclusion;
            sharedCutoutLeaves = cutoutLeaves;
        }
        return sharedRenderer;
    }

    private static MovingBlockRenderState createMovingBlock(BlockPos pos, BlockState blockState, ClientLevel level) {
        MovingBlockRenderState movingBlockRenderState = new MovingBlockRenderState();
        movingBlockRenderState.randomSeedPos = pos;
        movingBlockRenderState.blockPos = pos;
        movingBlockRenderState.blockState = blockState;
        Holder<Biome> biome = level.getBiome(pos);
        movingBlockRenderState.biome = biome;
        movingBlockRenderState.cardinalLighting = level.cardinalLighting();
        movingBlockRenderState.lightEngine = level.getLightEngine();
        return movingBlockRenderState;
    }

    public static boolean isPlayerInsidePreview(
            Minecraft mc,
            BlockPos origin,
            PlacementTransform transform,
            int sizeX,
            int sizeY,
            int sizeZ
    ) {
        if (mc.player == null) {
            return false;
        }

        PlacementBounds bounds = null;
        if (ClientPlacementRegistry.previewCache != null) {
            bounds = ClientPlacementRegistry.previewCache.bounds();
        }
        if (bounds == null) {
            bounds = PlacementBounds.fromPreviewBlocks(origin, ClientPlacementRegistry.previewBlocks, transform);
        }
        if (bounds == null) {
            bounds = PlacementBounds.fromSize(origin, transform, sizeX, sizeY, sizeZ);
        }
        return bounds.intersects(mc.player.getBoundingBox());
    }
}
