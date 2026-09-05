package com.ogatamizuki.instantstructure;

import net.minecraft.core.BlockPos;

import java.util.Collections;
import java.util.Map;

public record TemplateMetadata(
        String name,
        String description,
        String author,
        BlockPos offset,
        Map<String, BlockPos> specialPositions
) {
    public TemplateMetadata {
        specialPositions = specialPositions == null ? Map.of() : Collections.unmodifiableMap(specialPositions);
    }
}
