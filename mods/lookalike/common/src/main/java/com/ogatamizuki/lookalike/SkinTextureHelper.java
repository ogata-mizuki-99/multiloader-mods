package com.ogatamizuki.lookalike;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.properties.Property;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;

public final class SkinTextureHelper {
    private SkinTextureHelper() {
    }

    static Property toUnsignedDisguiseProperty(Property source, UUID wearerUuid, String wearerName) {
        return new Property("textures", rewriteTextureValue(source.value(), wearerUuid, wearerName));
    }

    public static String rewriteTextureValue(String encoded, UUID wearerUuid, String wearerName) {
        try {
            String json = new String(Base64.getDecoder().decode(encoded), StandardCharsets.UTF_8);
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            if (!root.has("textures")) {
                return encoded;
            }

            JsonObject rewritten = new JsonObject();
            if (root.has("timestamp")) {
                rewritten.add("timestamp", root.get("timestamp"));
            }
            rewritten.addProperty("profileId", wearerUuid.toString().replace("-", ""));
            rewritten.addProperty("profileName", wearerName);
            rewritten.add("textures", root.getAsJsonObject("textures").deepCopy());
            return Base64.getEncoder().encodeToString(rewritten.toString().getBytes(StandardCharsets.UTF_8));
        } catch (RuntimeException e) {
            return encoded;
        }
    }
}
