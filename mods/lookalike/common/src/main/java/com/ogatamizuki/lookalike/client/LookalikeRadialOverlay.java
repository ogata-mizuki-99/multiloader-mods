package com.ogatamizuki.lookalike.client;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.PlayerFaceExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.PlayerSkin;

import java.util.List;

public final class LookalikeRadialOverlay {
    private LookalikeRadialOverlay() {
    }

    public static void render(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        if (!LookalikeRadialSession.isActive()) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.gui.hud.isHidden()) {
            return;
        }

        List<LookalikeRadialSession.MenuEntry> menuEntries = LookalikeRadialSession.getMenuEntries();
        int centerX = LookalikeRadialSession.menuCenterX(mc);
        int centerY = LookalikeRadialSession.menuCenterY(mc);

        if (menuEntries.isEmpty()) {
            graphics.centeredText(
                    mc.font,
                    Component.translatable("lookalike.radial.no_targets"),
                    centerX,
                    centerY,
                    0xFFFFFF
            );
            return;
        }

        int hoveredIndex = LookalikeRadialSession.getHoveredIndex();
        LookalikeRadialSliceDrawer.drawMenu(graphics, centerX, centerY, menuEntries, hoveredIndex);

        var selfUuid = mc.player.getUUID();
        for (int i = 0; i < menuEntries.size(); i++) {
            LookalikeRadialSession.MenuEntry entry = menuEntries.get(i);
            int labelX = resolveLabelX(centerX, menuEntries.size(), i);
            int labelY = resolveLabelY(centerY, menuEntries.size(), i);

            boolean hovered = i == hoveredIndex;
            int size = hovered ? Math.max(LookalikeRadialLayout.FACE_SIZE + 4, 18) : LookalikeRadialLayout.FACE_SIZE;
            int textColor = hovered ? 0x56CFE1 : 0xFFFFFF;

            PlayerSkin skin = LookalikeClientSkins.resolveIcon(
                    entry.uuid(),
                    entry.name(),
                    entry.resetEntry(),
                    selfUuid
            );
            int faceX = labelX - size / 2;
            int faceY = labelY - size - 4;
            graphics.fill(faceX - 1, faceY - 1, faceX + size + 1, faceY + size + 1, 0xFF101820);
            PlayerFaceExtractor.extractRenderState(graphics, skin, faceX, faceY, size);
            graphics.centeredText(mc.font, entry.label(), labelX, labelY + 2, textColor);
        }
    }

    private static int resolveLabelX(int centerX, int sliceCount, int index) {
        if (usesTopAnchor(sliceCount)) {
            return centerX;
        }
        double angle = Math.toRadians((360.0 / sliceCount) * index - 90 + (180.0 / sliceCount));
        return centerX + (int) (Math.cos(angle) * LookalikeRadialLayout.LABEL_RADIUS);
    }

    private static int resolveLabelY(int centerY, int sliceCount, int index) {
        if (usesTopAnchor(sliceCount)) {
            return centerY - LookalikeRadialLayout.LABEL_RADIUS;
        }
        double angle = Math.toRadians((360.0 / sliceCount) * index - 90 + (180.0 / sliceCount));
        return centerY + (int) (Math.sin(angle) * LookalikeRadialLayout.LABEL_RADIUS);
    }

    private static boolean usesTopAnchor(int sliceCount) {
        return sliceCount <= 1;
    }
}
