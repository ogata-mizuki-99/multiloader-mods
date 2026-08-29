package com.ogatamizuki.instantstructure.client;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public final class BuilderPlacementHudOverlay {
    private static final int HINT_COLOR = 0xFFFFD080;
    private static final int NOTE_COLOR = 0xFFC0C0C0;
    private static final int STATUS_COLOR = 0xFF55FF55;

    private BuilderPlacementHudOverlay() {
    }

    private record HudLine(String text, int color) {
    }

    public static void render(GuiGraphicsExtractor guiGraphics, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.gui.hud.isHidden() || mc.gui.screen() != null) {
            return;
        }
        if (!ClientPlacementRegistry.active) {
            return;
        }

        List<HudLine> lines = new ArrayList<>();
        if (ClientPlacementRegistry.tentativelyConfirmed) {
            lines.add(new HudLine(Component.translatable("instant_structure.hud.builder.status_tentative").getString(), STATUS_COLOR));
            BlockPos anchor = ClientPlacementRegistry.lockedAnchor;
            if (anchor != null) {
                lines.add(new HudLine(
                        Component.translatable(
                                "instant_structure.hud.builder.anchor",
                                anchor.getX(), anchor.getY(), anchor.getZ()
                        ).getString(),
                        0xFFFFFFFF
                ));
            }
            lines.add(new HudLine(Component.translatable("instant_structure.hud.builder.final_confirm").getString(), HINT_COLOR));
            lines.add(new HudLine(Component.translatable("instant_structure.hud.builder.cancel_tentative").getString(), HINT_COLOR));
        } else {
            lines.add(new HudLine(Component.translatable("instant_structure.hud.builder.tentative_confirm").getString(), HINT_COLOR));
            lines.add(new HudLine(Component.translatable("instant_structure.hud.builder.rotate").getString(), HINT_COLOR));
            lines.add(new HudLine(Component.translatable("instant_structure.hud.builder.adjust_y").getString(), HINT_COLOR));
            if (ClientPlacementRegistry.isSimplifiedGhostMode()) {
                lines.add(new HudLine(
                        Component.translatable("instant_structure.hud.builder.simplified_preview").getString(),
                        NOTE_COLOR
                ));
            }
        }
        lines.add(new HudLine(Component.translatable("instant_structure.hud.builder.cancel_all").getString(), HINT_COLOR));
        lines.add(new HudLine(Component.translatable("instant_structure.hud.builder.place_hint").getString(), NOTE_COLOR));
        lines.add(new HudLine(Component.translatable("instant_structure.hud.builder.chest_hint").getString(), NOTE_COLOR));

        Font font = mc.font;
        int x = 8;
        int y = 8;
        int lineHeight = font.lineHeight + 2;
        int panelWidth = 0;
        for (HudLine line : lines) {
            panelWidth = Math.max(panelWidth, font.width(line.text()));
        }

        guiGraphics.fill(x - 4, y - 4, x + panelWidth + 8, y + lines.size() * lineHeight + 2, 0xA0000000);
        for (int i = 0; i < lines.size(); i++) {
            HudLine line = lines.get(i);
            guiGraphics.text(font, line.text(), x, y + i * lineHeight, line.color(), true);
        }
    }
}
