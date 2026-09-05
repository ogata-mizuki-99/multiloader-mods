package com.ogatamizuki.economy;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class PlacedBlocksSavedData extends SavedData {
    public record PlacedBlockKey(ResourceKey<Level> dimension, BlockPos pos) {
        public static final Codec<PlacedBlockKey> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ResourceKey.codec(Registries.DIMENSION).fieldOf("dimension").forGetter(PlacedBlockKey::dimension),
                BlockPos.CODEC.fieldOf("pos").forGetter(PlacedBlockKey::pos)
        ).apply(instance, PlacedBlockKey::new));
    }

    private static final Codec<PlacedBlocksSavedData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            PlacedBlockKey.CODEC.listOf().fieldOf("placedBlocks").forGetter(data -> List.copyOf(data.placedBlocks))
    ).apply(instance, placedBlocks -> {
        PlacedBlocksSavedData data = new PlacedBlocksSavedData();
        data.placedBlocks.addAll(placedBlocks);
        return data;
    }));

    private static final SavedDataType<PlacedBlocksSavedData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(EconomyCommon.MODID, "placed_blocks"),
            PlacedBlocksSavedData::new,
            CODEC,
            null
    );

    private final Set<PlacedBlockKey> placedBlocks = new HashSet<>();

    public PlacedBlocksSavedData() {
    }

    public static PlacedBlocksSavedData get(MinecraftServer server) {
        ServerLevel level = server.overworld();
        return level.getDataStorage().computeIfAbsent(TYPE);
    }

    public boolean isPlaced(ResourceKey<Level> dimension, BlockPos pos) {
        return placedBlocks.contains(new PlacedBlockKey(dimension, pos));
    }

    public void addPlaced(ResourceKey<Level> dimension, BlockPos pos) {
        if (placedBlocks.add(new PlacedBlockKey(dimension, pos))) {
            setDirty();
        }
    }

    public void removePlaced(ResourceKey<Level> dimension, BlockPos pos) {
        if (placedBlocks.remove(new PlacedBlockKey(dimension, pos))) {
            setDirty();
        }
    }
}
