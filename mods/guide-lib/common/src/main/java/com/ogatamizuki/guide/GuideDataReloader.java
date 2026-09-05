package com.ogatamizuki.guide;

import com.ogatamizuki.guide.model.GuideBook;
import com.ogatamizuki.guide.model.GuideManualDefinition;
import com.ogatamizuki.guide.model.GuideTheme;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;

import java.util.Map;

/**
 * Applies guide datapack loads to registries. Skips empty reload results when cached data
 * already exists so multiplayer connect reloads cannot wipe locally loaded guides.
 */
public final class GuideDataReloader {
    private GuideDataReloader() {}

    public static void reloadFrom(ResourceManager resourceManager) {
        applyBooks(GuideBookLoader.loadBooks(resourceManager));
        applyThemes(GuideThemeLoader.loadThemes(resourceManager));
        applyManuals(GuideManualLoader.loadManuals(resourceManager));
    }

    public static void applyBooks(Map<Identifier, GuideBook> books) {
        if (shouldKeepExisting(books, GuideBookRegistry.isEmpty())) {
            GuideLibCommon.LOGGER.warn(
                    "Guide book reload returned no books; keeping {} previously loaded book(s)",
                    GuideBookRegistry.getAllBooks().size()
            );
            return;
        }
        GuideBookRegistry.setBooks(books);
        GuideLibCommon.LOGGER.info("Loaded {} guide book(s)", books.size());
    }

    public static void applyThemes(Map<Identifier, GuideTheme> themes) {
        if (shouldKeepExisting(themes, GuideThemeRegistry.isEmpty())) {
            GuideLibCommon.LOGGER.warn(
                    "Guide theme reload returned no themes; keeping previously loaded theme data"
            );
            return;
        }
        GuideThemeRegistry.setThemes(themes);
        GuideLibCommon.LOGGER.info("Loaded {} guide theme(s)", themes.size());
    }

    public static void applyManuals(Map<Identifier, GuideManualDefinition> manuals) {
        if (shouldKeepExisting(manuals, GuideManualRegistry.isEmpty())) {
            GuideLibCommon.LOGGER.warn(
                    "Guide manual reload returned no manuals; keeping previously loaded manual data"
            );
            return;
        }
        GuideManualRegistry.setManuals(manuals);
        GuideLibCommon.LOGGER.info("Loaded {} guide manual(s)", manuals.size());
    }

    private static boolean shouldKeepExisting(Map<?, ?> loaded, boolean registryEmpty) {
        return loaded.isEmpty() && !registryEmpty;
    }
}
