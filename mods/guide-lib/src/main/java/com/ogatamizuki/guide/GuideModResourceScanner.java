package com.ogatamizuki.guide;

import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import net.minecraft.resources.Identifier;
import net.neoforged.fml.ModList;
import net.neoforged.fml.jarcontents.JarContents;
import net.neoforged.fml.jarcontents.JarResource;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
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
    private static final Pattern GUIDE_BOOK = Pattern.compile("^data/([a-z0-9_.-]+)/guide/([a-z0-9_.-]+)\\.json$");
    private static final Pattern GUIDE_THEME = Pattern.compile("^data/([a-z0-9_.-]+)/guide/themes/([a-z0-9_.-]+)\\.json$");
    private static final Pattern GUIDE_MANUAL = Pattern.compile("^data/([a-z0-9_.-]+)/guide/manuals/([a-z0-9_.-]+)\\.json$");
    private static final Pattern RECIPE = Pattern.compile("^data/([a-z0-9_.-]+)/recipe/([a-z0-9_.-]+)\\.json$");

    private GuideModResourceScanner() {}

    public static Map<Identifier, JsonElement> scanRecipeJson() {
        return scanJson(RECIPE, GuideModResourceScanner::recipeIdFor);
    }

    public static Map<Identifier, JsonElement> scanGuideThemeJson() {
        return scanJson(GUIDE_THEME, GuideModResourceScanner::themeIdFor);
    }

    public static Map<Identifier, JsonElement> scanGuideManualJson() {
        return scanJson(GUIDE_MANUAL, GuideModResourceScanner::manualIdFor);
    }

    public static <T> Map<Identifier, T> scanGuideBooks(
            BiFunction<Identifier, com.google.gson.JsonObject, T> parser,
            java.util.function.Function<T, Identifier> idExtractor
    ) {
        Map<Identifier, T> results = new LinkedHashMap<>();
        scanJson(GUIDE_BOOK, GuideModResourceScanner::bookResourceIdFor).forEach((resourceId, json) -> {
            if (!json.isJsonObject()) {
                return;
            }
            try {
                T value = parser.apply(resourceId, json.getAsJsonObject());
                if (value != null) {
                    results.putIfAbsent(idExtractor.apply(value), value);
                }
            } catch (JsonParseException e) {
                GuideLibMod.LOGGER.error("Failed to parse guide book {}", resourceId, e);
            }
        });
        return results;
    }

    private static Map<Identifier, JsonElement> scanJson(Pattern pattern, BiFunction<String, Matcher, Identifier> idFactory) {
        Map<Identifier, JsonElement> results = new LinkedHashMap<>();
        for (var modFileInfo : ModList.get().getModFiles()) {
            JarContents contents = modFileInfo.getFile().getContents();
            contents.visitContent("data", (path, resource) -> {
                Matcher matcher = pattern.matcher(path);
                if (!matcher.matches()) {
                    return;
                }
                Identifier id = idFactory.apply(path, matcher);
                if (id.getNamespace().equals("minecraft") || id.getNamespace().equals("neoforge")) {
                    return;
                }
                try {
                    results.putIfAbsent(id, readJson(resource));
                } catch (IOException | JsonParseException e) {
                    GuideLibMod.LOGGER.error("Failed to load guide resource {} from mod jar", id, e);
                }
            });
        }
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

    private static JsonElement readJson(JarResource resource) throws IOException {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(resource.open(), StandardCharsets.UTF_8))) {
            return com.google.gson.JsonParser.parseReader(reader);
        }
    }
}
