package com.ogatamizuki.guide;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.ogatamizuki.guide.model.GuideTheme;
import com.ogatamizuki.guide.platform.Services;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class GuideThemeLoader extends SimplePreparableReloadListener<Map<Identifier, GuideTheme>> {
    public static final Identifier LISTENER_ID = GuideLibCommon.id("guide_themes");

    private static Map<Identifier, JsonObject> cachedModJarThemes;

    @Override
    protected Map<Identifier, GuideTheme> prepare(ResourceManager resourceManager, ProfilerFiller profiler) {
        return loadThemes(resourceManager);
    }

    @Override
    protected void apply(Map<Identifier, GuideTheme> loaded, ResourceManager resourceManager, ProfilerFiller profiler) {
        GuideDataReloader.applyThemes(loaded);
    }

    public static Map<Identifier, GuideTheme> loadThemes(ResourceManager resourceManager) {
        Map<Identifier, JsonObject> rawThemes = new LinkedHashMap<>();
        if (Services.PLATFORM.isPhysicalClient()) {
            rawThemes.putAll(loadThemeJsonFromModJars());
        }
        rawThemes.putAll(loadThemeJsonFromResourceManager(resourceManager));

        Map<Identifier, GuideTheme> resolved = new LinkedHashMap<>();
        for (Map.Entry<Identifier, JsonObject> entry : rawThemes.entrySet()) {
            try {
                resolved.put(entry.getKey(), resolveTheme(entry.getKey(), entry.getValue(), rawThemes, resolved));
            } catch (JsonParseException e) {
                GuideLibCommon.LOGGER.error("Failed to resolve guide theme {}", entry.getKey(), e);
            }
        }
        return resolved;
    }

    public static Map<Identifier, JsonObject> loadThemeJsonFromModJars() {
        if (cachedModJarThemes == null) {
            Map<Identifier, JsonObject> loaded = new LinkedHashMap<>();
            GuideModResourceScanner.scanGuideThemeJson().forEach((id, json) -> {
                if (json.isJsonObject()) {
                    loaded.put(id, json.getAsJsonObject());
                }
            });
            cachedModJarThemes = Collections.unmodifiableMap(loaded);
            GuideLibCommon.LOGGER.info("Indexed {} guide theme(s) from mod jars", cachedModJarThemes.size());
        }
        return cachedModJarThemes;
    }

    public static void invalidateModJarCache() {
        cachedModJarThemes = null;
    }

    private static Map<Identifier, JsonObject> loadThemeJsonFromResourceManager(ResourceManager resourceManager) {
        Map<Identifier, JsonObject> rawThemes = new LinkedHashMap<>();

        for (Map.Entry<Identifier, List<Resource>> entry : resourceManager.listResourceStacks(
                "guide/themes",
                path -> path.getPath().endsWith(".json")
        ).entrySet()) {
            Identifier resourceId = entry.getKey();
            String resourcePath = resourceId.getPath();
            if (!resourcePath.startsWith("guide/themes/") || !resourcePath.endsWith(".json")) {
                continue;
            }
            String themeLocalId = resourcePath.substring("guide/themes/".length(), resourcePath.length() - ".json".length());
            Identifier themeId = Identifier.fromNamespaceAndPath(resourceId.getNamespace(), themeLocalId);

            for (Resource resource : entry.getValue()) {
                try (BufferedReader reader = resource.openAsReader()) {
                    JsonElement parsed = com.google.gson.JsonParser.parseReader(reader);
                    if (parsed.isJsonObject()) {
                        rawThemes.put(themeId, parsed.getAsJsonObject());
                    }
                } catch (IOException | JsonParseException e) {
                    GuideLibCommon.LOGGER.error("Failed to load guide theme {}", themeId, e);
                }
            }
        }

        return rawThemes;
    }

    private static GuideTheme resolveTheme(
            Identifier themeId,
            JsonObject root,
            Map<Identifier, JsonObject> rawThemes,
            Map<Identifier, GuideTheme> resolved
    ) {
        if (resolved.containsKey(themeId)) {
            return resolved.get(themeId);
        }

        GuideTheme base = GuideTheme.bookBuiltin();
        if (root.has("extends")) {
            Identifier parentId = Identifier.parse(root.get("extends").getAsString());
            if (parentId.equals(themeId)) {
                base = GuideTheme.TABLET_ID.equals(themeId)
                        ? GuideTheme.tabletBuiltin()
                        : GuideTheme.bookBuiltin();
            } else if (resolved.containsKey(parentId)) {
                base = resolved.get(parentId);
            } else if (rawThemes.containsKey(parentId)) {
                base = resolveTheme(parentId, rawThemes.get(parentId), rawThemes, resolved);
            } else if (GuideTheme.BOOK_ID.equals(parentId)) {
                base = GuideTheme.bookBuiltin();
            } else if (GuideTheme.TABLET_ID.equals(parentId)) {
                base = GuideTheme.tabletBuiltin();
            } else {
                base = GuideThemeRegistry.resolve(parentId);
            }
        } else if (GuideTheme.TABLET_ID.equals(themeId)) {
            base = GuideTheme.tabletBuiltin();
        }

        GuideTheme.Builder builder = GuideTheme.Builder.from(base).id(themeId);
        if (root.has("colors") && root.get("colors").isJsonObject()) {
            JsonObject colors = root.getAsJsonObject("colors");
            builder.colorTitle(parseColor(colors, "title", base.colorTitle()));
            builder.colorSubtitle(parseColor(colors, "subtitle", base.colorSubtitle()));
            builder.colorBody(parseColor(colors, "body", base.colorBody()));
            builder.colorMuted(parseColor(colors, "muted", base.colorMuted()));
            builder.colorError(parseColor(colors, "error", base.colorError()));
            builder.colorAccent(parseColor(colors, "accent", base.colorAccent()));
            builder.colorLink(parseColor(colors, "link", base.colorLink()));
            builder.colorLinkHover(parseColor(colors, "link_hover", base.colorLinkHover()));
            builder.colorPanelBg(parseColor(colors, "panel_bg", base.colorPanelBg()));
            builder.colorInnerBg(parseColor(colors, "inner_bg", base.colorInnerBg()));
            builder.colorDimOverlay(parseColor(colors, "dim_overlay", base.colorDimOverlay()));
            builder.colorSlotInner(parseColor(colors, "slot_inner", base.colorSlotInner()));
        }

        if (root.has("frame")) {
            builder.frameStyle(parseFrame(root.get("frame").getAsString(), base.frameStyle()));
        }

        if (root.has("sounds") && root.get("sounds").isJsonObject()) {
            JsonObject sounds = root.getAsJsonObject("sounds");
            if (sounds.has("open")) {
                builder.openSound(parseSound(sounds.get("open").getAsString(), base.openSound()));
            }
            if (sounds.has("page_turn")) {
                builder.pageTurnSound(parseSound(sounds.get("page_turn").getAsString(), base.pageTurnSound()));
            }
        }

        GuideTheme theme = builder.build();
        resolved.put(themeId, theme);
        return theme;
    }

    private static int parseColor(JsonObject colors, String field, int fallback) {
        if (!colors.has(field)) {
            return fallback;
        }
        String raw = colors.get(field).getAsString();
        if (raw.startsWith("#")) {
            raw = raw.substring(1);
        }
        if (raw.length() == 6) {
            return (int) Long.parseLong(raw, 16) | 0xFF000000;
        }
        if (raw.length() == 8) {
            return (int) Long.parseLong(raw, 16);
        }
        throw new JsonParseException("Invalid color value: " + raw);
    }

    private static GuideTheme.FrameStyle parseFrame(String raw, GuideTheme.FrameStyle fallback) {
        return switch (raw) {
            case "minecraft_bevel" -> GuideTheme.FrameStyle.MINECRAFT_BEVEL;
            case "paper" -> GuideTheme.FrameStyle.PAPER;
            case "tablet" -> GuideTheme.FrameStyle.TABLET;
            default -> fallback;
        };
    }

    private static Identifier parseSound(String raw, Identifier fallback) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return Identifier.parse(raw);
    }
}
