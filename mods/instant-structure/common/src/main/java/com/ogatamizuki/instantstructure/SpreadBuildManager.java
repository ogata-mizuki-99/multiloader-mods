package com.ogatamizuki.instantstructure;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class SpreadBuildManager {
    public static final int SPREAD_THRESHOLD = 8000;
    public static final int BLOCKS_PER_TICK = 2000;

    private static final List<SpreadBuildTask> ACTIVE_TASKS = new CopyOnWriteArrayList<>();

    private SpreadBuildManager() {
    }

    public static boolean hasActiveTasks() {
        return !ACTIVE_TASKS.isEmpty();
    }

    public static boolean queueIfLarge(
            ServerLevel level,
            Path nbtPath,
            StructureTemplate template,
            BlockPos origin,
            PlacementTransform transform,
            net.minecraft.server.level.ServerPlayer player
    ) {
        List<StructureTemplate.StructureBlockInfo> blocks;
        try {
            blocks = StructureTemplateHelper.collectSolidBlockInfos(level, nbtPath);
        } catch (IOException e) {
            InstantStructureCommon.LOGGER.error("Failed to read template for spread build", e);
            return false;
        }
        if (blocks.size() <= SPREAD_THRESHOLD) {
            return false;
        }
        ACTIVE_TASKS.add(new SpreadBuildTask(level, blocks, origin, transform, player));
        return true;
    }

    public static void tickServer() {
        if (ACTIVE_TASKS.isEmpty()) {
            return;
        }
        Iterator<SpreadBuildTask> iterator = ACTIVE_TASKS.iterator();
        while (iterator.hasNext()) {
            SpreadBuildTask task = iterator.next();
            if (task.tick(BLOCKS_PER_TICK)) {
                ACTIVE_TASKS.remove(task);
            }
        }
    }

    private static final class SpreadBuildTask {
        private final ServerLevel level;
        private final List<StructureTemplate.StructureBlockInfo> blocks;
        private final BlockPos origin;
        private final PlacementTransform transform;
        private final net.minecraft.server.level.ServerPlayer player;
        private int index;

        private SpreadBuildTask(
                ServerLevel level,
                List<StructureTemplate.StructureBlockInfo> blocks,
                BlockPos origin,
                PlacementTransform transform,
                net.minecraft.server.level.ServerPlayer player
        ) {
            this.level = level;
            this.blocks = new ArrayList<>(blocks);
            this.origin = origin;
            this.transform = transform;
            this.player = player;
        }

        private boolean tick(int count) {
            for (int i = 0; i < count && index < blocks.size(); i++, index++) {
                StructureTemplate.StructureBlockInfo info = blocks.get(index);
                BlockPos worldPos = origin.offset(transform.toWorldRelative(info.pos()));
                BlockState state = transform.transformBlockState(info.state());
                level.setBlock(worldPos, state, 3);
            }
            if (player != null) {
                int percent = (int) ((double) index / blocks.size() * 100);
                player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("instant_structure.message.building_progress", percent, index, blocks.size()), true);
                if (index >= blocks.size()) {
                    player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("instant_structure.message.building_completed").withStyle(net.minecraft.ChatFormatting.GREEN), true);
                }
            }
            return index >= blocks.size();
        }
    }
}
