package com.ogatamizuki.lookalike;

import com.google.common.collect.LinkedHashMultimap;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * 謎の影用の暗色スキンテクスチャ（Mojang CDN 上の単色黒スキン）。
 * 旧ハッシュ {@code 1a4af718…} は Steve 既定スキンだったため差し替え済み。
 */
public final class ShadowSkinTextures {
    private static final String SHADOW_TEXTURE_JSON = """
            {"timestamp":0,"profileId":"00000000000000000000000000000000","profileName":"Shadow",\
            "textures":{"SKIN":{"url":"http://textures.minecraft.net/texture/3679804e0de3024340eae02e04b2b7c4"}}}""";

    private static final String SHADOW_TEXTURE_VALUE = Base64.getEncoder()
            .encodeToString(SHADOW_TEXTURE_JSON.trim().getBytes(StandardCharsets.UTF_8));

    private ShadowSkinTextures() {
    }

    public static String textureValue() {
        return SHADOW_TEXTURE_VALUE;
    }

    public static PropertyMap textureProperties() {
        LinkedHashMultimap<String, Property> properties = LinkedHashMultimap.create();
        properties.put("textures", new Property("textures", SHADOW_TEXTURE_VALUE));
        return new PropertyMap(properties);
    }
}
