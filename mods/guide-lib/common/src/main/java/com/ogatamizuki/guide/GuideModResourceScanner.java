package com.ogatamizuki.guide;

import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.ogatamizuki.guide.platform.Services;
import net.minecraft.resources.Identifier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Loads guide JSON directly from mod JARs when the client ResourceManager does not expose datapack
 * resources yet (Dedicated Server connect without integrated server reload).
 */
public final class GuideModResourceScanner {
    private static final Logger LOGGER = LogManager.getLogger(GuideModResourceScanner.class);
    private static final Pattern GUIDE_BOOK = Pattern.compile("^data/([a-z0-9_.-]+)/guide/([a-z0-9_.-]+)\\.json$");
    private static final Pattern GUIDE_THEME = Pattern.compile("^data/([a-z0-9_.-]+)/guide/themes/([a-z0-9_.-]+)\\.json$");
    private static final Pattern GUIDE_MANUAL = Pattern.compile("^data/([a-z0-9_.-]+)/guide/manuals/([a-z0-9_.-]+)\\.json$");
    private static final Pattern RECIPE = Pattern.compile("^data/([a-z0-9_.-]+)/recipe/([a-z0-9_.-]+)\\.json$");

    private GuideModResourceScanner() {}

    public static Map<Identifier, JsonElement> scanRecipeJson() {
        return Services.PLATFORM.scanModJarResources(RECIPE, GuideModResourceScanner::recipeIdFor);
    }

    public static Map<Identifier, JsonElement> scanGuideThemeJson() {
        return Services.PLATFORM.scanModJarResources(GUIDE_THEME, GuideModResourceScanner::themeIdFor);
    }

    public static Map<Identifier, JsonElement> scanGuideManualJson() {
        return Services.PLATFORM.scanModJarResources(GUIDE_MANUAL, GuideModResourceScanner::manualIdFor);
    }

    public static <T> Map<Identifier, T> scanGuideBooks(
            BiFunction<Identifier, com.google.gson.JsonObject, T> parser,
            java.util.function.Function<T, Identifier> idExtractor
    ) {
        Map<Identifier, T> results = new LinkedHashMap<>();
        Services.PLATFORM.scanModJarResources(GUIDE_BOOK, GuideModResourceScanner::bookResourceIdFor).forEach((resourceId, json) -> {
            if (!json.isJsonObject()) {
                return;
            }
            try {
                T value = parser.apply(resourceId, json.getAsJsonObject());
                if (value != null) {
                    results.putIfAbsent(idExtractor.apply(value), value);
                }
            } catch (JsonParseException e) {
                LOGGER.error("Failed to parse guide book {}", resourceId, e);
            }
        });
        return results;
    }

    private static Identifier bookResourceIdFor(String path, Matcher matcher) {
        String namespace = matcher.group(1);
        String localId = matcher.group(2);
        return Identifier.fromNamespaceAndPath(namespace, "guide/" + localId + ".json");
    }

    private static Identifier themeIdFor(String path, Matcher matcher) {
        return Identifier.fromNamespaceAndPath(matcher.group(1), matcher.group(2));
    }

    private static Identifier manualIdFor(String path, Matcher matcher) {
        return Identifier.fromNamespaceAndPath(matcher.group(1), matcher.group(2));
    }

    private static Identifier recipeIdFor(String path, Matcher matcher) {
        return Identifier.fromNamespaceAndPath(matcher.group(1), matcher.group(2));
    }
}
