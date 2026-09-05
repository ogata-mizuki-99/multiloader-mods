package com.ogatamizuki.radialteleport.client;

import com.mojang.blaze3d.platform.NativeImage;
import com.ogatamizuki.radialteleport.RadialTeleportCommon;
import com.ogatamizuki.radialteleport.TeleportDestination;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

import java.util.List;
import java.util.Objects;

final class RadialMenuTextureCache {
    private static final Identifier BASE_TEXTURE_ID = Identifier.fromNamespaceAndPath(
            RadialTeleportCommon.MODID, "radial_menu/base");
    private static final Identifier HOVER_TEXTURE_ID = Identifier.fromNamespaceAndPath(
            RadialTeleportCommon.MODID, "radial_menu/hover");

    private static DynamicTexture baseTexture;
    private static DynamicTexture hoverTexture;
    private static NativeImage baseImage;
    private static NativeImage hoverImage;
    private static boolean baseDirty = true;
    private static boolean hoverDirty = true;
    private static int cachedHoverIndex = Integer.MIN_VALUE;
    private static List<TeleportDestination> cachedDestinations = List.of();

    private RadialMenuTextureCache() {
    }

    static void markDestinationsDirty() {
        baseDirty = true;
        hoverDirty = true;
    }

    static void markHoverDirty() {
        hoverDirty = true;
    }

    static void update(Minecraft mc, List<TeleportDestination> destinations, int hoveredIndex) {
        ensureAllocated(mc);

        if (baseDirty || !sameDestinations(destinations, cachedDestinations)) {
            RadialSliceRaster.bakeBase(baseImage, destinations);
            baseTexture.upload();
            cachedDestinations = destinations;
            baseDirty = false;
            hoverDirty = true;
        }

        if (hoverDirty || cachedHoverIndex != hoveredIndex) {
            RadialSliceRaster.bakeHover(hoverImage, destinations, hoveredIndex);
            hoverTexture.upload();
            cachedHoverIndex = hoveredIndex;
            hoverDirty = false;
        }
    }

    static void blit(GuiGraphicsExtractor graphics, int centerX, int centerY) {
        if (baseTexture == null || hoverTexture == null) {
            return;
        }

        int half = RadialMenuLayout.TEXTURE_SIZE / 2;
        int drawX = centerX - half;
        int drawY = centerY - half;
        int size = RadialMenuLayout.TEXTURE_SIZE;

        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                BASE_TEXTURE_ID,
                drawX,
                drawY,
                0.0F,
                0.0F,
                size,
                size,
                size,
                size
        );
        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                HOVER_TEXTURE_ID,
                drawX,
                drawY,
                0.0F,
                0.0F,
                size,
                size,
                size,
                size
        );
    }

    static void release(Minecraft mc) {
        if (mc != null) {
            mc.getTextureManager().release(BASE_TEXTURE_ID);
            mc.getTextureManager().release(HOVER_TEXTURE_ID);
        }

        closeTexture(baseTexture, baseImage);
        closeTexture(hoverTexture, hoverImage);
        baseTexture = null;
        hoverTexture = null;
        baseImage = null;
        hoverImage = null;
        baseDirty = true;
        hoverDirty = true;
        cachedHoverIndex = Integer.MIN_VALUE;
        cachedDestinations = List.of();
    }

    private static void ensureAllocated(Minecraft mc) {
        if (baseTexture != null && hoverTexture != null) {
            return;
        }

        int size = RadialMenuLayout.TEXTURE_SIZE;
        baseImage = new NativeImage(size, size, true);
        hoverImage = new NativeImage(size, size, true);
        baseTexture = new DynamicTexture(() -> BASE_TEXTURE_ID.toString(), baseImage);
        hoverTexture = new DynamicTexture(() -> HOVER_TEXTURE_ID.toString(), hoverImage);
        mc.getTextureManager().register(BASE_TEXTURE_ID, baseTexture);
        mc.getTextureManager().register(HOVER_TEXTURE_ID, hoverTexture);
        baseDirty = true;
        hoverDirty = true;
    }

    private static void closeTexture(DynamicTexture texture, NativeImage image) {
        if (texture != null) {
            texture.close();
        } else if (image != null) {
            image.close();
        }
    }

    private static boolean sameDestinations(List<TeleportDestination> left, List<TeleportDestination> right) {
        if (left.size() != right.size()) {
            return false;
        }
        for (int i = 0; i < left.size(); i++) {
            TeleportDestination a = left.get(i);
            TeleportDestination b = right.get(i);
            if (!Objects.equals(a.id(), b.id())
                    || !Objects.equals(a.displayName(), b.displayName())
                    || a.kind() != b.kind()) {
                return false;
            }
        }
        return true;
    }
}
