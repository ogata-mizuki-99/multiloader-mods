package com.ogatamizuki.lookalike.client;

import com.ogatamizuki.lookalike.LookalikeAttachments.ScanEntry;
import com.ogatamizuki.lookalike.NetworkPayloads.DisguiseRequestPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public final class LookalikeRadialSession {
    public record MenuEntry(UUID uuid, Component label, boolean resetEntry) {}

    private static boolean active;
    private static boolean selectionApplied;
    private static List<MenuEntry> menuEntries = List.of();
    private static int hoveredIndex = -1;

    private LookalikeRadialSession() {
    }

    public static void begin(Minecraft mc) {
        active = true;
        selectionApplied = false;
        hoveredIndex = -1;
        LookalikeMouseCapture.captureForRadialMenu(mc);
        rebuildEntries(mc);
    }

    public static void closeWithoutSelecting(Minecraft mc) {
        if (!active) {
            return;
        }
        clear(mc);
    }

    public static void cancel(Minecraft mc) {
        closeWithoutSelecting(mc);
    }

    static void refreshHoverForInput(Minecraft mc) {
        if (!active || mc.player == null) {
            return;
        }
        rebuildEntries(mc);
        updateHoveredIndex(mc);
    }

    public static void confirmSelection(Minecraft mc) {
        if (!active || selectionApplied) {
            return;
        }
        applyHoveredSelection(mc);
        clear(mc);
    }

    public static boolean isActive() {
        return active;
    }

    public static List<MenuEntry> getMenuEntries() {
        return menuEntries;
    }

    public static int getHoveredIndex() {
        return hoveredIndex;
    }

    public static boolean isMouseOverCenter(Minecraft mc) {
        double dx = scaledMouseX(mc) - menuCenterX(mc);
        double dy = scaledMouseY(mc) - menuCenterY(mc);
        return Math.hypot(dx, dy) < LookalikeRadialLayout.INNER_RADIUS;
    }

    public static void tick(Minecraft mc) {
        if (!active) {
            return;
        }
        rebuildEntries(mc);
        updateHoveredIndex(mc);
    }

    private static void rebuildEntries(Minecraft mc) {
        if (mc.getConnection() == null || mc.player == null) {
            menuEntries = List.of();
            return;
        }

        List<MenuEntry> entries = new ArrayList<>();
        entries.add(new MenuEntry(
                mc.player.getUUID(),
                Component.translatable("lookalike.radial.reset"),
                true
        ));

        for (ScanEntry entry : LookalikeModClient.getScanHistory()) {
            entries.add(new MenuEntry(entry.uuid(), Component.literal(entry.name()), false));
        }

        menuEntries = Collections.unmodifiableList(entries);
        if (hoveredIndex >= menuEntries.size()) {
            hoveredIndex = -1;
        }
    }

    private static void updateHoveredIndex(Minecraft mc) {
        if (isMouseOverCenter(mc)) {
            hoveredIndex = -1;
            return;
        }

        int centerX = menuCenterX(mc);
        int centerY = menuCenterY(mc);
        hoveredIndex = resolveHoveredIndex(
                (int) scaledMouseX(mc),
                (int) scaledMouseY(mc),
                centerX,
                centerY,
                menuEntries.size()
        );
    }

    static int resolveHoveredIndex(int mouseX, int mouseY, int centerX, int centerY, int entryCount) {
        if (entryCount <= 0) {
            return -1;
        }

        double dx = mouseX - centerX;
        double dy = mouseY - centerY;
        double distance = Math.hypot(dx, dy);

        if (distance < LookalikeRadialLayout.INNER_RADIUS
                || distance > LookalikeRadialLayout.OUTER_RADIUS + LookalikeRadialLayout.HOVER_MARGIN) {
            return -1;
        }

        float angleDegrees = (float) Math.toDegrees(Math.atan2(dy, dx));
        float sliceAngle = 360.0F / entryCount;
        float normalized = LookalikeRadialLayout.normalizeDegrees(angleDegrees + 90.0F);
        int index = (int) (normalized / sliceAngle);
        if (index >= entryCount) {
            index = entryCount - 1;
        }
        return index;
    }

    static int menuCenterX(Minecraft mc) {
        return mc.getWindow().getGuiScaledWidth() / 2;
    }

    static int menuCenterY(Minecraft mc) {
        return mc.getWindow().getGuiScaledHeight() / 2 + LookalikeRadialLayout.CENTER_Y_OFFSET;
    }

    static double scaledMouseX(Minecraft mc) {
        return mc.mouseHandler.xpos() * (double) mc.getWindow().getGuiScaledWidth() / (double) mc.getWindow().getScreenWidth();
    }

    static double scaledMouseY(Minecraft mc) {
        return mc.mouseHandler.ypos() * (double) mc.getWindow().getGuiScaledHeight() / (double) mc.getWindow().getScreenHeight();
    }

    private static void applyHoveredSelection(Minecraft mc) {
        if (selectionApplied || mc.player == null) {
            return;
        }

        if (hoveredIndex >= 0 && hoveredIndex < menuEntries.size() && mc.getConnection() != null) {
            MenuEntry selected = menuEntries.get(hoveredIndex);
            mc.getConnection().send(new DisguiseRequestPayload(selected.uuid().toString()));
            selectionApplied = true;
            mc.player.stopUsingItem();
        }
    }

    private static void clear(Minecraft mc) {
        if (active) {
            LookalikeMouseCapture.restore(mc);
        }
        active = false;
        menuEntries = List.of();
        hoveredIndex = -1;
    }
}
