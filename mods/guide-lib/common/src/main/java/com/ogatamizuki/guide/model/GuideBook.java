package com.ogatamizuki.guide.model;

import com.ogatamizuki.guide.GuideIcons;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

public record GuideBook(
        Identifier id,
        String modNamespace,
        String nameKey,
        String descriptionKey,
        @Nullable Identifier iconId,
        int sortPriority,
        Identifier themeId,
        int listColumns,
        boolean codexVisible,
        boolean devOnly,
        List<GuideCategory> categories,
        Map<String, GuideEntry> entries
) {
    public ItemStack icon() {
        return GuideIcons.resolve(iconId);
    }
    public GuideEntry getEntry(String entryId) {
        return entries.get(entryId);
    }
}
