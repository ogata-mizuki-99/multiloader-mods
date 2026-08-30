package com.ogatamizuki.guide.platform;

import com.google.gson.JsonElement;
import net.minecraft.resources.Identifier;

import java.util.Map;
import java.util.function.BiFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public interface IPlatformHelper {
    Platform getPlatform();

    boolean isModLoaded(String modId);

    boolean isDevelopmentEnvironment();

    boolean isPhysicalClient();

    Map<Identifier, JsonElement> scanModJarResources(
            Pattern pattern,
            BiFunction<String, Matcher, Identifier> idFactory
    );
}
