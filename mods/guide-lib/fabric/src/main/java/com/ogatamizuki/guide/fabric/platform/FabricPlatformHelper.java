package com.ogatamizuki.guide.fabric.platform;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.ogatamizuki.guide.GuideLibCommon;
import com.ogatamizuki.guide.platform.IPlatformHelper;
import com.ogatamizuki.guide.platform.Platform;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.resources.Identifier;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FabricPlatformHelper implements IPlatformHelper {
    @Override
    public Platform getPlatform() {
        return Platform.FABRIC;
    }

    @Override
    public boolean isModLoaded(String modId) {
        return FabricLoader.getInstance().isModLoaded(modId);
    }

    @Override
    public boolean isDevelopmentEnvironment() {
        return FabricLoader.getInstance().isDevelopmentEnvironment();
    }

    @Override
    public boolean isPhysicalClient() {
        return FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT;
    }

    @Override
    public Map<Identifier, JsonElement> scanModJarResources(
            Pattern pattern,
            BiFunction<String, Matcher, Identifier> idFactory
    ) {
        Map<Identifier, JsonElement> results = new LinkedHashMap<>();
        for (ModContainer container : FabricLoader.getInstance().getAllMods()) {
            container.findPath("data").ifPresent(dataPath -> {
                try {
                    Files.walkFileTree(dataPath, new SimpleFileVisitor<Path>() {
                        @Override
                        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                            String relPath = "data/" + dataPath.relativize(file).toString().replace('\\', '/');
                            Matcher matcher = pattern.matcher(relPath);
                            if (matcher.matches()) {
                                Identifier id = idFactory.apply(relPath, matcher);
                                if (!id.getNamespace().equals("minecraft") && !id.getNamespace().equals("fabric")) {
                                    try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                                        results.putIfAbsent(id, JsonParser.parseReader(reader));
                                    } catch (Exception e) {
                                        GuideLibCommon.LOGGER.error("Failed to load guide resource {} from mod jar", id, e);
                                    }
                                }
                            }
                            return FileVisitResult.CONTINUE;
                        }
                    });
                } catch (IOException ignored) {}
            });
        }
        return results;
    }
}
