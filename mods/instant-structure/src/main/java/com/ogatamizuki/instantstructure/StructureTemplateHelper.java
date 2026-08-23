package com.ogatamizuki.instantstructure;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class StructureTemplateHelper {
    private StructureTemplateHelper() {
    }

    public static StructureTemplate loadTemplate(ServerLevel level, Path nbtPath) throws IOException {
        CompoundTag compoundTag = NbtIo.readCompressed(nbtPath, net.minecraft.nbt.NbtAccounter.unlimitedHeap());
        StructureTemplate template = new StructureTemplate();
        template.load(level.holderLookup(Registries.BLOCK), compoundTag);
        return template;
    }

    public static List<PreviewBlockEntry> extractSolidBlocks(Level level, Path nbtPath) throws IOException {
        return extractSolidBlocks(level.registryAccess(), nbtPath);
    }

    public static List<PreviewBlockEntry> extractSolidBlocks(RegistryAccess registryAccess, Path nbtPath) throws IOException {
        List<PreviewBlockEntry> blocks = new ArrayList<>();
        for (StructureTemplate.StructureBlockInfo info : readBlockInfos(registryAccess, nbtPath)) {
            if (!info.state().isAir()) {
                BlockPos pos = info.pos();
                blocks.add(new PreviewBlockEntry(
                        pos.getX(),
                        pos.getY(),
                        pos.getZ(),
                        BlockStateParserSupport.serialize(info.state())
                ));
            }
        }
        return blocks;
    }

    public static List<StructureTemplate.StructureBlockInfo> collectSolidBlockInfos(Level level, Path nbtPath) throws IOException {
        return collectSolidBlockInfos(level.registryAccess(), nbtPath);
    }

    public static List<StructureTemplate.StructureBlockInfo> collectSolidBlockInfos(RegistryAccess registryAccess, Path nbtPath) throws IOException {
        List<StructureTemplate.StructureBlockInfo> blocks = new ArrayList<>();
        for (StructureTemplate.StructureBlockInfo info : readBlockInfos(registryAccess, nbtPath)) {
            if (!info.state().isAir()) {
                blocks.add(info);
            }
        }
        return blocks;
    }

    private static List<StructureTemplate.StructureBlockInfo> readBlockInfos(RegistryAccess registryAccess, Path nbtPath) throws IOException {
        CompoundTag tag = NbtIo.readCompressed(nbtPath, net.minecraft.nbt.NbtAccounter.unlimitedHeap());
        HolderGetter<Block> lookup = registryAccess.lookupOrThrow(Registries.BLOCK);
        List<BlockState> palette = readPaletteStates(tag, lookup);
        ListTag blocksTag = tag.getList("blocks").orElse(new ListTag());
        List<StructureTemplate.StructureBlockInfo> result = new ArrayList<>(blocksTag.size());

        for (int i = 0; i < blocksTag.size(); i++) {
            CompoundTag blockTag = blocksTag.getCompound(i).orElse(new CompoundTag());
            ListTag posTag = blockTag.getList("pos").orElse(new ListTag());
            if (posTag.size() < 3) {
                continue;
            }
            int stateIndex = blockTag.getInt("state").orElse(0);
            BlockState state = stateIndex >= 0 && stateIndex < palette.size()
                    ? palette.get(stateIndex)
                    : net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
            BlockPos pos = new BlockPos(
                    posTag.getInt(0).orElse(0),
                    posTag.getInt(1).orElse(0),
                    posTag.getInt(2).orElse(0)
            );
            CompoundTag nbt = blockTag.contains("nbt") ? blockTag.getCompound("nbt").orElse(null) : null;
            result.add(new StructureTemplate.StructureBlockInfo(pos, state, nbt));
        }
        return result;
    }

    private static List<BlockState> readPaletteStates(CompoundTag tag, HolderGetter<Block> lookup) {
        ListTag paletteTag;
        var palettesTag = tag.getList("palettes");
        if (palettesTag.isPresent() && !palettesTag.get().isEmpty()) {
            paletteTag = palettesTag.get().getListOrEmpty(0);
        } else {
            paletteTag = tag.getList("palette").orElse(new ListTag());
        }

        List<BlockState> palette = new ArrayList<>(paletteTag.size());
        for (int i = 0; i < paletteTag.size(); i++) {
            CompoundTag stateTag = paletteTag.getCompound(i).orElse(null);
            if (stateTag != null && !stateTag.isEmpty()) {
                palette.add(NbtUtils.readBlockState(lookup, stateTag));
            } else {
                palette.add(BlockStateParserSupport.parse(
                        (HolderLookup<Block>) lookup,
                        paletteTag.getString(i).orElse("minecraft:air")
                ));
            }
        }
        return palette;
    }

    public static Rotation toRotation(int rotationDegrees) {
        return switch (rotationDegrees) {
            case 90 -> Rotation.CLOCKWISE_90;
            case 180 -> Rotation.CLOCKWISE_180;
            case 270 -> Rotation.COUNTERCLOCKWISE_90;
            default -> Rotation.NONE;
        };
    }

    public static StructurePlaceSettings createPlaceSettings(int rotationDegrees) {
        return createPlaceSettings(rotationDegrees, Mirror.NONE);
    }

    public static StructurePlaceSettings createPlaceSettings(int rotationDegrees, Mirror mirror) {
        StructurePlaceSettings settings = new StructurePlaceSettings();
        settings.setRotation(toRotation(rotationDegrees));
        settings.setMirror(mirror);
        return settings;
    }

    public static Mirror mirrorFromOrdinal(int ordinal) {
        Mirror[] values = Mirror.values();
        if (ordinal < 0 || ordinal >= values.length) {
            return Mirror.NONE;
        }
        return values[ordinal];
    }

    public static Path resolveTemplatePath(Path configDir, String category, String templateName) {
        return configDir.resolve("templates-structure").resolve(category).resolve(templateName + ".nbt");
    }

    public static boolean templateExists(Path configDir, String category, String templateName) {
        return Files.exists(resolveTemplatePath(configDir, category, templateName));
    }
}
