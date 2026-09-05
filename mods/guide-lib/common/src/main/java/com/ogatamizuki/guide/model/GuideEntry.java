package com.ogatamizuki.guide.model;

import com.ogatamizuki.guide.GuideIcons;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.List;

public record GuideEntry(
        String id,
        String nameKey,
        @Nullable Identifier iconId,
        List<GuidePage> pages
) {
    public ItemStack icon() {
        return GuideIcons.resolve(iconId);
    }
}
