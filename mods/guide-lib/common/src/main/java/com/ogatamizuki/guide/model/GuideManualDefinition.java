package com.ogatamizuki.guide.model;

import net.minecraft.resources.Identifier;

public record GuideManualDefinition(
        Identifier id,
        Identifier itemId,
        Identifier opensBook,
        Identifier themeId
) {}
