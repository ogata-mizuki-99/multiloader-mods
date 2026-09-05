package com.ogatamizuki.instantstructure.client;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public final class SelectionHudOverlay {
    private static final int HINT_COLOR = 0xFFFFD080;

    private SelectionHudOverlay() {
    }

    private record HudLine(String text, int color) {
    }

    public static void render(GuiGraphicsExtractor guiGraphics, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.gui.hud.isHidden() || mc.gui.screen() != null) {
            return;
        }
        if (!ClientSelectionRegistry.isHoldingMarker(mc)) {
            return;
        }

        List<HudLine> lines = buildLines(mc);
        if (lines.isEmpty()) {
            return;
        }

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

    private static List<HudLine> buildLines(Minecraft mc) {
        List<HudLine> lines = new ArrayList<>();

        if (ClientSelectionRegistry.hasStart && ClientSelectionRegistry.pos1 != null) {
            BlockPos pos1 = ClientSelectionRegistry.pos1;
            BlockPos pos2 = ClientSelectionRegistry.resolvePreviewEnd(mc);
            if (pos2 != null) {
                lines.add(new HudLine(SelectionDisplayTexts.point1(pos1), 0xFFFFFFFF));
                if (ClientSelectionRegistry.hasBoth && ClientSelectionRegistry.pos2 != null) {
                    lines.add(new HudLine(SelectionDisplayTexts.point2(ClientSelectionRegistry.pos2), 0xFFFFFFFF));
                } else {
                    lines.add(new HudLine(SelectionDisplayTexts.point2Preview(pos2), 0xFFFFFFFF));
                }
                SelectionBounds bounds = SelectionBounds.from(pos1, pos2);
                lines.add(new HudLine(SelectionDisplayTexts.size(bounds), 0xFFFFFFFF));

                if (ClientSelectionRegistry.confirmed) {
                    lines.add(new HudLine(
                            Component.translatable("instant_structure.hud.selection_confirmed").getString(),
                            0xFF55FF55
                    ));
                }
            }
        }

        appendHints(lines);
        return lines;
    }

    private static void appendHints(List<HudLine> lines) {
        if (!ClientSelectionRegistry.hasStart) {
            lines.add(hint("instant_structure.hud.hint_set_start"));
            return;
        }

        if (!ClientSelectionRegistry.hasBoth) {
            lines.add(hint("instant_structure.hud.hint_set_end"));
        } else if (!ClientSelectionRegistry.confirmed) {
            lines.add(hint("instant_structure.hud.hint_confirm"));
        }

        lines.add(hint("instant_structure.hud.hint_undo"));
        lines.add(hint("instant_structure.hud.hint_adjust_y"));
    }

    private static HudLine hint(String key) {
        return new HudLine(Component.translatable(key).getString(), HINT_COLOR);
    }
}
