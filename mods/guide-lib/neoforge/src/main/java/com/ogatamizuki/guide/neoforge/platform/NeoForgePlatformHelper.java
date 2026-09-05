package com.ogatamizuki.guide.neoforge.platform;

import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.ogatamizuki.guide.GuideLibCommon;
import com.ogatamizuki.guide.platform.IPlatformHelper;
import com.ogatamizuki.guide.platform.Platform;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModList;
import net.neoforged.fml.jarcontents.JarContents;
import net.neoforged.fml.jarcontents.JarResource;
import net.neoforged.fml.loading.FMLEnvironment;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NeoForgePlatformHelper implements IPlatformHelper {
    @Override
    public Platform getPlatform() {
        return Platform.NEOFORGE;
    }

    @Override
    public boolean isModLoaded(String modId) {
        return ModList.get().isLoaded(modId);
    }

    @Override
    public boolean isDevelopmentEnvironment() {
        return !FMLEnvironment.isProduction();
    }

    @Override
    public boolean isPhysicalClient() {
        return FMLEnvironment.getDist() == Dist.CLIENT;
    }

    @Override
    public Map<Identifier, JsonElement> scanModJarResources(
            Pattern pattern,
            BiFunction<String, Matcher, Identifier> idFactory
    ) {
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
                    GuideLibCommon.LOGGER.error("Failed to load guide resource {} from mod jar", id, e);
                }
            });
        }
        return results;
    }

    private static JsonElement readJson(JarResource resource) throws IOException {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(resource.open(), StandardCharsets.UTF_8))) {
            return JsonParser.parseReader(reader);
        }
    }
}
