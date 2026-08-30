package com.ogatamizuki.guide;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.ogatamizuki.guide.client.page.GuidePageExpander;
import com.ogatamizuki.guide.model.GuideBook;
import com.ogatamizuki.guide.model.GuideCategory;
import com.ogatamizuki.guide.model.GuideEntry;
import com.ogatamizuki.guide.model.GuidePage;
import com.ogatamizuki.guide.model.GuideTheme;
import com.ogatamizuki.guide.platform.Services;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import javax.annotation.Nullable;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class GuideBookLoader extends SimplePreparableReloadListener<Map<Identifier, GuideBook>> {
    public static final Identifier LISTENER_ID = GuideLibCommon.id("guide_books");

    private static Map<Identifier, GuideBook> cachedModJarBooks;

    @Override
    protected Map<Identifier, GuideBook> prepare(ResourceManager resourceManager, ProfilerFiller profiler) {
        return loadBooks(resourceManager);
    }

    @Override
    protected void apply(Map<Identifier, GuideBook> books, ResourceManager resourceManager, ProfilerFiller profiler) {
        GuideDataReloader.applyBooks(books);
    }

    public static Map<Identifier, GuideBook> loadBooks(ResourceManager resourceManager) {
        Map<Identifier, GuideBook> books = new LinkedHashMap<>();
        if (Services.PLATFORM.isPhysicalClient()) {
            books.putAll(loadBooksFromModJars());
        }
        books.putAll(loadBooksFromResourceManager(resourceManager));
        return books;
    }

    public static Map<Identifier, GuideBook> loadBooksFromModJars() {
        if (cachedModJarBooks == null) {
            Map<Identifier, GuideBook> loaded = GuideModResourceScanner.scanGuideBooks(
                    GuideBookLoader::parseBook,
                    GuideBook::id
            );
            cachedModJarBooks = Collections.unmodifiableMap(loaded);
            GuideLibCommon.LOGGER.info("Indexed {} guide book(s) from mod jars", cachedModJarBooks.size());
        }
        return cachedModJarBooks;
    }

    public static void invalidateModJarCache() {
        cachedModJarBooks = null;
    }

    private static Map<Identifier, GuideBook> loadBooksFromResourceManager(ResourceManager resourceManager) {
        Map<Identifier, GuideBook> books = new LinkedHashMap<>();

        for (Map.Entry<Identifier, List<Resource>> entry : resourceManager.listResourceStacks("guide", path -> path.getPath().endsWith(".json")).entrySet()) {
            Identifier resourceId = entry.getKey();
            for (Resource resource : entry.getValue()) {
                try (BufferedReader reader = resource.openAsReader()) {
                    JsonElement parsed = com.google.gson.JsonParser.parseReader(reader);
                    if (!parsed.isJsonObject()) {
                        continue;
                    }
                    GuideBook book = parseBook(resourceId, parsed.getAsJsonObject());
                    if (book != null) {
                        books.put(book.id(), book);
                    }
                } catch (IOException | JsonParseException e) {
                    GuideLibCommon.LOGGER.error("Failed to load guide book {}", resourceId, e);
                }
            }
        }

        return books;
    }

    private static GuideBook parseBook(Identifier resourceId, JsonObject root) {
        String namespace = resourceId.getNamespace();
        String bookPath = resourceId.getPath();
        if (!bookPath.startsWith("guide/") || !bookPath.endsWith(".json") || bookPath.indexOf('/', "guide/".length()) >= 0) {
            return null;
        }
        String bookLocalId = bookPath.substring("guide/".length(), bookPath.length() - ".json".length());
        Identifier bookId = Identifier.fromNamespaceAndPath(namespace, bookLocalId);

        String nameKey = requiredString(root, "name", bookId);
        String descriptionKey = optionalString(root, "description", "");
        Identifier iconId = parseIconId(root, "icon");
        int sortPriority = root.has("sort_priority") ? root.get("sort_priority").getAsInt() : 100;
        Identifier themeId = root.has("theme")
                ? Identifier.parse(root.get("theme").getAsString())
                : GuideTheme.BOOK_ID;
        int listColumns = root.has("list_columns")
                ? Math.clamp(root.get("list_columns").getAsInt(), 1, 2)
                : 1;
        boolean codexVisible = !root.has("codex_visible") || root.get("codex_visible").getAsBoolean();
        boolean devOnly = root.has("dev_only") && root.get("dev_only").getAsBoolean();

        Map<String, GuideEntry> entries = parseEntries(root, bookId);
        List<GuideCategory> categories = parseCategories(root, bookId, entries.keySet());
        validateBook(bookId, categories, entries);

        return new GuideBook(bookId, namespace, nameKey, descriptionKey, iconId, sortPriority, themeId, listColumns, codexVisible, devOnly, categories, entries);
    }

    private static Map<String, GuideEntry> parseEntries(JsonObject root, Identifier bookId) {
        Map<String, GuideEntry> entries = new LinkedHashMap<>();
        if (!root.has("entries") || !root.get("entries").isJsonObject()) {
            return entries;
        }

        JsonObject entriesObject = root.getAsJsonObject("entries");
        for (Map.Entry<String, JsonElement> entry : entriesObject.entrySet()) {
            if (!entry.getValue().isJsonObject()) {
                continue;
            }
            JsonObject entryObject = entry.getValue().getAsJsonObject();
            String entryId = entry.getKey();
            String nameKey = requiredString(entryObject, "name", bookId);
            Identifier iconId = parseIconId(entryObject, "icon");
            List<GuidePage> pages = parsePages(entryObject, bookId);
            entries.put(entryId, new GuideEntry(entryId, nameKey, iconId, pages));
        }
        return entries;
    }

    private static List<GuideCategory> parseCategories(JsonObject root, Identifier bookId, java.util.Set<String> knownEntryIds) {
        List<GuideCategory> categories = new ArrayList<>();
        if (!root.has("categories") || !root.get("categories").isJsonArray()) {
            if (!knownEntryIds.isEmpty()) {
                categories.add(new GuideCategory("default", "", new ArrayList<>(knownEntryIds)));
            }
            return categories;
        }

        JsonArray categoriesArray = root.getAsJsonArray("categories");
        for (JsonElement element : categoriesArray) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject categoryObject = element.getAsJsonObject();
            String categoryId = requiredString(categoryObject, "id", bookId);
            String nameKey = optionalString(categoryObject, "name", "");
            List<String> entryIds = new ArrayList<>();
            if (categoryObject.has("entries") && categoryObject.get("entries").isJsonArray()) {
                for (JsonElement entryElement : categoryObject.getAsJsonArray("entries")) {
                    if (entryElement.isJsonPrimitive()) {
                        entryIds.add(entryElement.getAsString());
                    }
                }
            }
            categories.add(new GuideCategory(categoryId, nameKey, entryIds));
        }
        return categories;
    }

    private static List<GuidePage> parsePages(JsonObject entryObject, Identifier bookId) {
        List<GuidePage> pages = new ArrayList<>();
        if (!entryObject.has("pages") || !entryObject.get("pages").isJsonArray()) {
            return pages;
        }

        JsonArray pagesArray = entryObject.getAsJsonArray("pages");
        for (JsonElement pageElement : pagesArray) {
            if (!pageElement.isJsonObject()) {
                continue;
            }
            JsonObject pageObject = pageElement.getAsJsonObject();
            String type = requiredString(pageObject, "type", bookId);
            if (!GuidePageExpander.isKnownType(type)) {
                GuideLibCommon.LOGGER.warn("Unknown guide page type '{}' in book {}", type, bookId);
            }
            pages.add(new GuidePage(type, pageObject));
        }
        return pages;
    }

    private static void validateBook(Identifier bookId, List<GuideCategory> categories, Map<String, GuideEntry> entries) {
        java.util.Set<String> referenced = new java.util.HashSet<>();
        for (GuideCategory category : categories) {
            for (String entryId : category.entryIds()) {
                if (!entries.containsKey(entryId)) {
                    GuideLibCommon.LOGGER.warn("Guide book {} category {} references unknown entry '{}'", bookId, category.id(), entryId);
                }
                if (!referenced.add(entryId)) {
                    GuideLibCommon.LOGGER.warn("Guide book {} references entry '{}' more than once in categories", bookId, entryId);
                }
            }
        }
    }

    @Nullable
    private static Identifier parseIconId(JsonObject object, String field) {
        if (!object.has(field)) {
            return null;
        }
        try {
            return Identifier.parse(object.get(field).getAsString());
        } catch (Exception e) {
            GuideLibCommon.LOGGER.warn("Invalid item id in guide json field {}: {}", field, object.get(field));
            return null;
        }
    }

    private static String requiredString(JsonObject object, String field, Identifier context) {
        if (!object.has(field)) {
            throw new JsonParseException("Missing field '" + field + "' in guide book " + context);
        }
        return object.get(field).getAsString();
    }

    private static String optionalString(JsonObject object, String field, String fallback) {
        if (!object.has(field)) {
            return fallback;
        }
        return object.get(field).getAsString();
    }
}
