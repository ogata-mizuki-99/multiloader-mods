package com.ogatamizuki.instantstructure.client;

import com.ogatamizuki.instantstructure.BlockStateParserSupport;
import com.ogatamizuki.instantstructure.PlacementBounds;
import com.ogatamizuki.instantstructure.PlacementTransform;
import com.ogatamizuki.instantstructure.PreviewBlockEntry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

public final class PlacementPreviewCache {
    public record CachedBlock(BlockPos worldPos, BlockState state) {
    }

    private final List<CachedBlock> blocks;
    private final PlacementBounds bounds;

    private PlacementPreviewCache(List<CachedBlock> blocks, PlacementBounds bounds) {
        this.blocks = List.copyOf(blocks);
        this.bounds = bounds;
    }

    public List<CachedBlock> blocks() {
        return blocks;
    }

    public PlacementBounds bounds() {
        return bounds;
    }

    public int blockCount() {
        return blocks.size();
    }

    public static PlacementPreviewCache build(
            BlockPos origin,
            PlacementTransform transform,
            List<PreviewBlockEntry> entries,
            HolderLookup<Block> blockLookup
    ) {
        List<CachedBlock> cached = new ArrayList<>(entries.size());
        for (PreviewBlockEntry entry : entries) {
            BlockState rawState = BlockStateParserSupport.parse(blockLookup, entry.blockState());
            if (rawState.isAir()) {
                continue;
            }
            BlockState state = transform.transformBlockState(rawState);
            BlockPos worldPos = transform.toWorldPos(origin, new BlockPos(entry.x(), entry.y(), entry.z()));
            cached.add(new CachedBlock(worldPos, state));
        }

        PlacementBounds bounds = PlacementBounds.fromPreviewBlocks(origin, entries, transform);
        if (bounds == null && !cached.isEmpty()) {
            int minX = Integer.MAX_VALUE;
            int minY = Integer.MAX_VALUE;
            int minZ = Integer.MAX_VALUE;
            int maxX = Integer.MIN_VALUE;
            int maxY = Integer.MIN_VALUE;
            int maxZ = Integer.MIN_VALUE;
            for (CachedBlock block : cached) {
                BlockPos pos = block.worldPos();
                minX = Math.min(minX, pos.getX());
                minY = Math.min(minY, pos.getY());
                minZ = Math.min(minZ, pos.getZ());
                maxX = Math.max(maxX, pos.getX());
                maxY = Math.max(maxY, pos.getY());
                maxZ = Math.max(maxZ, pos.getZ());
            }
            bounds = new PlacementBounds(minX, minY, minZ, maxX, maxY, maxZ);
        }
        return new PlacementPreviewCache(cached, bounds);
    }
}
