package com.ogatamizuki.radialteleport.client;

import com.ogatamizuki.radialteleport.TeleportDestination;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.Mth;

import java.util.List;

public final class RadialTeleportOverlay {
    private RadialTeleportOverlay() {
    }

    public static void render(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        if (!RadialTeleportSession.isActive()) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.gui.hud.isHidden()) {
            return;
        }

        List<TeleportDestination> destinations = RadialTeleportSession.getDestinations();
        if (destinations.isEmpty()) {
            return;
        }

        int centerX = menuCenterX(mc);
        int centerY = menuCenterY(mc);
        int hoveredIndex = RadialTeleportSession.getHoveredIndex();

        RadialSliceRaster.drawMenu(graphics, centerX, centerY, destinations, hoveredIndex);

        for (int i = 0; i < RadialTeleportLabelCache.entries().size(); i++) {
            renderSliceLabel(graphics, mc, RadialTeleportLabelCache.entries().get(i), centerX, centerY, i == hoveredIndex);
        }
    }

    public static float getMouseAngleDegrees() {
        if (!isMouseInSliceRing()) {
            return Float.NaN;
        }

        Minecraft mc = Minecraft.getInstance();
        double mouseX = mc.mouseHandler.xpos() * mc.getWindow().getGuiScaledWidth() / mc.getWindow().getScreenWidth();
        double mouseY = mc.mouseHandler.ypos() * mc.getWindow().getGuiScaledHeight() / mc.getWindow().getScreenHeight();
        int centerX = menuCenterX(mc);
        int centerY = menuCenterY(mc);

        double dx = mouseX - centerX;
        double dy = mouseY - centerY;
        return (float) Math.toDegrees(Math.atan2(dy, dx));
    }

    public static boolean isMouseOverCenter() {
        return getMouseDistanceFromCenter() < RadialMenuLayout.INNER_RADIUS;
    }

    private static double getMouseDistanceFromCenter() {
        Minecraft mc = Minecraft.getInstance();
        double mouseX = mc.mouseHandler.xpos() * mc.getWindow().getGuiScaledWidth() / mc.getWindow().getScreenWidth();
        double mouseY = mc.mouseHandler.ypos() * mc.getWindow().getGuiScaledHeight() / mc.getWindow().getScreenHeight();
        int centerX = menuCenterX(mc);
        int centerY = menuCenterY(mc);
        double dx = mouseX - centerX;
        double dy = mouseY - centerY;
        return Math.hypot(dx, dy);
    }

    private static boolean isMouseInSliceRing() {
        double distance = getMouseDistanceFromCenter();
        return distance >= RadialMenuLayout.INNER_RADIUS
                && distance <= RadialMenuLayout.OUTER_RADIUS + RadialMenuLayout.HOVER_MARGIN;
    }

    public static int resolveHoveredSlice(int sliceCount, float angleDegrees) {
        if (sliceCount <= 0 || Float.isNaN(angleDegrees)) {
            return -1;
        }

        float sliceAngle = 360.0F / sliceCount;
        float normalized = Mth.wrapDegrees(angleDegrees + 90.0F);
        if (normalized < 0.0F) {
            normalized += 360.0F;
        }

        int index = (int) (normalized / sliceAngle);
        if (index >= sliceCount) {
            index = sliceCount - 1;
        }
        return index;
    }

    private static int menuCenterX(Minecraft mc) {
        return mc.getWindow().getGuiScaledWidth() / 2;
    }

    private static int menuCenterY(Minecraft mc) {
        return mc.getWindow().getGuiScaledHeight() / 2 + RadialMenuLayout.CENTER_Y_OFFSET;
    }

    private static void renderSliceLabel(
            GuiGraphicsExtractor graphics,
            Minecraft mc,
            RadialTeleportLabelCache.LabelEntry entry,
            int centerX,
            int centerY,
            boolean hovered
    ) {
        int labelX = resolveLabelX(entry, centerX);
        int labelY = resolveLabelY(entry, centerY);
        Font font = mc.font;

        if (hovered) {
            renderHoveredFullLabel(graphics, font, entry, labelX, labelY);
            return;
        }

        if (entry.destination().kind() == TeleportDestination.KIND_SPAWN) {
            if (entry.truncatedName() != null) {
                graphics.centeredText(font, entry.truncatedName(), labelX, labelY, RadialMenuLayout.LABEL_COLOR);
            }
            return;
        }

        if (entry.destination().kind() == TeleportDestination.KIND_WAYPOINT) {
            if (entry.truncatedName() != null) {
                graphics.centeredText(font, entry.truncatedName(), labelX, labelY, RadialMenuLayout.WAYPOINT_LABEL_COLOR);
            }
            return;
        }

        if (entry.playerUuid() == null || entry.truncatedName() == null) {
            return;
        }

        int faceSize = entry.faceSize();
        int faceX = labelX - faceSize / 2;
        int faceY = labelY - faceSize - 4;
        RadialTeleportFaceRenderer.drawPlayerFace(graphics, entry.playerUuid(), faceX, faceY, faceSize);
        graphics.centeredText(font, entry.truncatedName(), labelX, labelY + 2, RadialMenuLayout.LABEL_COLOR);
    }

    private static void renderHoveredFullLabel(
            GuiGraphicsExtractor graphics,
            Font font,
            RadialTeleportLabelCache.LabelEntry entry,
            int labelX,
            int labelY
    ) {
        String fullName = entry.displayName();
        int textColor = switch (entry.destination().kind()) {
            case TeleportDestination.KIND_WAYPOINT -> RadialMenuLayout.WAYPOINT_LABEL_HOVER;
            case TeleportDestination.KIND_SPAWN -> RadialMenuLayout.LABEL_HOVER;
            default -> RadialMenuLayout.LABEL_HOVER;
        };

        int textTop = labelY;
        if (entry.destination().kind() == TeleportDestination.KIND_PLAYER && entry.playerUuid() != null) {
            int faceSize = Math.max(entry.faceSize(), 12);
            int faceX = labelX - faceSize / 2;
            int faceY = labelY - faceSize - 4;
            RadialTeleportFaceRenderer.drawPlayerFace(graphics, entry.playerUuid(), faceX, faceY, faceSize);
            textTop = labelY + 2;
        }

        int textWidth = font.width(fullName);
        int textHeight = font.lineHeight;
        int textLeft = labelX - textWidth / 2;
        int paddingX = 5;
        int paddingY = 3;
        int boxLeft = textLeft - paddingX;
        int boxTop = textTop - paddingY;
        int boxRight = textLeft + textWidth + paddingX;
        int boxBottom = textTop + textHeight + paddingY;

        graphics.fill(boxLeft, boxTop, boxRight, boxBottom, 0xF010141C);
        graphics.fill(boxLeft, boxTop, boxRight, boxTop + 1, 0xFF56CFE1);
        graphics.fill(boxLeft, boxBottom - 1, boxRight, boxBottom, 0xFF56CFE1);
        graphics.fill(boxLeft, boxTop, boxLeft + 1, boxBottom, 0xFF56CFE1);
        graphics.fill(boxRight - 1, boxTop, boxRight, boxBottom, 0xFF56CFE1);
        graphics.text(font, fullName, textLeft, textTop, textColor, false);
    }

    private static int resolveLabelX(RadialTeleportLabelCache.LabelEntry entry, int centerX) {
        if (usesTopAnchor(entry)) {
            return centerX;
        }
        float midRadians = (float) Math.toRadians(entry.midDegrees());
        return centerX + (int) (Math.cos(midRadians) * RadialMenuLayout.LABEL_RADIUS);
    }

    private static int resolveLabelY(RadialTeleportLabelCache.LabelEntry entry, int centerY) {
        if (usesTopAnchor(entry)) {
            return centerY - RadialMenuLayout.LABEL_RADIUS;
        }
        float midRadians = (float) Math.toRadians(entry.midDegrees());
        return centerY + (int) (Math.sin(midRadians) * RadialMenuLayout.LABEL_RADIUS);
    }

    /** Full-ring (single destination) menus place the label at the top; multi-slice menus use slice mid-angle. */
    private static boolean usesTopAnchor(RadialTeleportLabelCache.LabelEntry entry) {
        return entry.sliceAngle() >= 359.9F;
    }
}
