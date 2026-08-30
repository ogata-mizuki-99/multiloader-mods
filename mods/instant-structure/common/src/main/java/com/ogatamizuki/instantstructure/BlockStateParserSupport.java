package com.ogatamizuki.instantstructure;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public final class BlockStateParserSupport {
    private BlockStateParserSupport() {
    }

    public static BlockState parse(HolderLookup<Block> lookup, String stateString) {
        try {
            return BlockStateParser.parseForBlock(lookup, new StringReader(stateString), false).blockState();
        } catch (CommandSyntaxException e) {
            return net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
        }
    }

    public static String serialize(BlockState state) {
        return BlockStateParser.serialize(state);
    }
}
