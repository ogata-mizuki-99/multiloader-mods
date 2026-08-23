package com.ogatamizuki.instantstructure;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * werewolf 連携用の家テンプレート特殊座標キーと、NBT からの自動検出。
 */
public final class WerewolfHouseMetadata {
    public static final String KEY_DOOR = "door_position";
    public static final String KEY_HEAD = "head_sign_position";
    public static final String KEY_OUTSIDE = "outside_position";
    public static final String KEY_INSIDE = "inside_position";

    public static final Set<String> REQUIRED_KEYS = Set.of(KEY_DOOR, KEY_HEAD, KEY_OUTSIDE, KEY_INSIDE);

    private WerewolfHouseMetadata() {
    }

    public static boolean isComplete(Map<String, BlockPos> positions) {
        if (positions == null || positions.isEmpty()) {
            return false;
        }
        for (String key : REQUIRED_KEYS) {
            if (!positions.containsKey(key)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 固体ブロック一覧から玄関・頭・内外テレポート先を推定する。
     */
    public static Optional<Map<String, BlockPos>> detect(List<StructureTemplate.StructureBlockInfo> blocks) {
        if (blocks == null || blocks.isEmpty()) {
            return Optional.empty();
        }

        List<DoorCandidate> doors = new ArrayList<>();
        for (StructureTemplate.StructureBlockInfo info : blocks) {
            if (!(info.state().getBlock() instanceof DoorBlock)) {
                continue;
            }
            if (info.state().getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) != DoubleBlockHalf.LOWER) {
                continue;
            }
            Direction facing = info.state().getValue(BlockStateProperties.HORIZONTAL_FACING);
            doors.add(new DoorCandidate(info.pos(), facing));
        }

        if (doors.isEmpty()) {
            return Optional.empty();
        }

        DoorCandidate primary = selectPrimaryDoor(doors);
        BlockPos doorPos = primary.pos();
        Direction facing = primary.facing();

        BlockPos outside = doorPos.relative(facing);
        BlockPos inside = doorPos.relative(facing.getOpposite());
        BlockPos head = new BlockPos(doorPos.getX(), doorPos.getY() + 2, doorPos.getZ());

        Map<String, BlockPos> result = new LinkedHashMap<>();
        result.put(KEY_DOOR, doorPos);
        result.put(KEY_HEAD, head);
        result.put(KEY_OUTSIDE, outside);
        result.put(KEY_INSIDE, inside);
        return Optional.of(result);
    }

    private static DoorCandidate selectPrimaryDoor(List<DoorCandidate> doors) {
        doors.sort((a, b) -> {
            int facingCmp = Integer.compare(facingPriority(b.facing()), facingPriority(a.facing()));
            if (facingCmp != 0) {
                return facingCmp;
            }
            int zCmp = Integer.compare(a.pos().getZ(), b.pos().getZ());
            if (zCmp != 0) {
                return zCmp;
            }
            return Integer.compare(a.pos().getX(), b.pos().getX());
        });
        return doors.getFirst();
    }

    /** werewolf 標準テンプレートは玄関が北向き（-Z）のため、北向きドアを優先する。 */
    private static int facingPriority(Direction facing) {
        return facing == Direction.NORTH ? 1 : 0;
    }

    private record DoorCandidate(BlockPos pos, Direction facing) {
    }
}
