package com.ogatamizuki.guide;

import com.ogatamizuki.guide.model.GuideTheme;
import net.minecraft.resources.Identifier;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class GuideThemeRegistry {
    private static Map<Identifier, GuideTheme> themes = Map.of();

    private GuideThemeRegistry() {}

    public static void setThemes(Map<Identifier, GuideTheme> loadedThemes) {
        themes = Collections.unmodifiableMap(new LinkedHashMap<>(loadedThemes));
    }

    public static boolean isEmpty() {
        return themes.isEmpty();
    }

    public static GuideTheme resolve(Identifier themeId) {
        if (themeId == null) {
            return GuideTheme.bookBuiltin();
        }
        GuideTheme loaded = themes.get(themeId);
        if (loaded != null) {
            return loaded;
        }
        if (GuideTheme.BOOK_ID.equals(themeId)) {
            return GuideTheme.bookBuiltin();
        }
        if (GuideTheme.TABLET_ID.equals(themeId)) {
            return GuideTheme.tabletBuiltin();
        }
        GuideLibCommon.LOGGER.warn("Unknown guide theme {}, falling back to book", themeId);
        return GuideTheme.bookBuiltin();
    }
}
