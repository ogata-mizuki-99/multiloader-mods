package com.ogatamizuki.radialteleport.client;

import com.ogatamizuki.radialteleport.RadialTeleportCommon;
import com.ogatamizuki.radialteleport.WaypointActionPayload;
import com.ogatamizuki.radialteleport.WaypointListEntry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class WaypointEditScreen extends Screen {
    private static final int PANEL_WIDTH = 340;
    private static final int PANEL_MARGIN_H = 48;
    private static final int PANEL_MARGIN_V = 32;
    private static final int ROW_HEIGHT = 28;
    private static final int HEADER_HEIGHT = 34;
    private static final int HEADER_SCROLL_LINE = 10;
    private static final int PANEL_BOTTOM_PAD = 8;
    private static final int MAX_VIEWPORT_ROWS = 8;
    private static final int MIN_VIEWPORT_ROWS = 3;
    private static final int OUTER_SECTION_GAP = 12;
    private static final int CONTROL_HEIGHT = 20;
    private static final int NAME_BOX_WIDTH = 148;
    private static final int APPLY_WIDTH = 72;
    private static final int DONE_WIDTH = 60;
    private static final int BOTTOM_CONTROL_GAP = 8;

    private List<WaypointListEntry> entries = List.of();
    private int selectedIndex = -1;
    private int scrollOffset;
    private int viewportRows = MAX_VIEWPORT_ROWS;
    private EditBox nameBox;
    private int listTop;
    private int listLeft;
    private int listRight;
    private int listBottom;
    private int panelTop;
    private int panelBottom;
    private int bottomRowY;
    private int headerHeight = HEADER_HEIGHT;

    protected WaypointEditScreen() {
        super(Component.translatable("radial_teleport.screen.waypoint_edit.title"));
    }

    public static void open() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && mc.getConnection() != null) {
            RadialTeleportCommon.sendToServer.accept(WaypointActionPayload.openEdit());
        }
    }

    public static void applyList(List<WaypointListEntry> waypoints) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.gui.screen() instanceof WaypointEditScreen screen) {
            screen.updateEntries(waypoints);
        } else if (mc.player != null) {
            RadialTeleportClientHooks.onWaypointScreenOpened();
            WaypointEditScreen screen = new WaypointEditScreen();
            screen.updateEntries(waypoints);
            mc.gui.setScreen(screen);
        }
    }

    public void updateEntries(List<WaypointListEntry> waypoints) {
        this.entries = new ArrayList<>(waypoints);
        if (this.selectedIndex < 0 && !this.entries.isEmpty()) {
            this.selectedIndex = 0;
        } else if (this.selectedIndex >= this.entries.size()) {
            this.selectedIndex = this.entries.isEmpty() ? -1 : this.entries.size() - 1;
        }
        clampScrollOffset();
        ensureSelectedVisible();
        if (this.nameBox != null) {
            syncNameBox();
        }
        this.rebuildRowButtons();
    }

    @Override
    protected void init() {
        super.init();
        layoutPanel();
        rebuildRowButtons();
        syncNameBox();
    }

    private void layoutPanel() {
        int centerX = this.width / 2;
        int panelWidth = Math.min(PANEL_WIDTH, Math.max(240, this.width - PANEL_MARGIN_H * 2));
        this.listLeft = centerX - panelWidth / 2;
        this.listRight = centerX + panelWidth / 2;

        if (entries.isEmpty()) {
            this.viewportRows = 0;
            int panelHeight = 108;
            this.panelTop = (this.height - panelHeight) / 2;
            this.panelBottom = panelTop + panelHeight;
            this.listTop = panelTop + HEADER_HEIGHT;
            this.listBottom = listTop;
            this.bottomRowY = panelBottom - 28;
            return;
        }

        int outerHeight = OUTER_SECTION_GAP + CONTROL_HEIGHT;
        int availablePanelHeight = this.height - PANEL_MARGIN_V * 2 - outerHeight;

        this.headerHeight = HEADER_HEIGHT;
        int maxRowsByScreen = (availablePanelHeight - headerHeight - PANEL_BOTTOM_PAD) / ROW_HEIGHT;
        this.viewportRows = Math.min(MAX_VIEWPORT_ROWS, Math.max(MIN_VIEWPORT_ROWS, maxRowsByScreen));

        if (entries.size() > viewportRows) {
            this.headerHeight = HEADER_HEIGHT + HEADER_SCROLL_LINE;
            maxRowsByScreen = (availablePanelHeight - headerHeight - PANEL_BOTTOM_PAD) / ROW_HEIGHT;
            this.viewportRows = Math.min(MAX_VIEWPORT_ROWS, Math.max(MIN_VIEWPORT_ROWS, maxRowsByScreen));
        }

        int listViewportHeight = viewportRows * ROW_HEIGHT;
        int panelHeight = headerHeight + listViewportHeight + PANEL_BOTTOM_PAD;
        int totalHeight = panelHeight + outerHeight;

        this.panelTop = Math.max(PANEL_MARGIN_V, (this.height - totalHeight) / 2);
        this.panelBottom = panelTop + panelHeight;
        this.listTop = panelTop + headerHeight;
        this.listBottom = listTop + listViewportHeight;
        this.bottomRowY = panelBottom + OUTER_SECTION_GAP;
    }

    private boolean showsScrollHint() {
        return maxScrollOffset() > 0;
    }

    private int maxScrollOffset() {
        return Math.max(0, entries.size() - viewportRows);
    }

    private void clampScrollOffset() {
        this.scrollOffset = Math.max(0, Math.min(scrollOffset, maxScrollOffset()));
    }

    private void ensureSelectedVisible() {
        if (selectedIndex < 0) {
            return;
        }
        if (selectedIndex < scrollOffset) {
            scrollOffset = selectedIndex;
        } else if (selectedIndex >= scrollOffset + viewportRows) {
            scrollOffset = selectedIndex - viewportRows + 1;
        }
        clampScrollOffset();
    }

    private void initBottomControls() {
        int nameBoxX = listLeft + 4;
        int applyX = nameBoxX + NAME_BOX_WIDTH + BOTTOM_CONTROL_GAP;
        int doneX = applyX + APPLY_WIDTH + BOTTOM_CONTROL_GAP;

        this.nameBox = new EditBox(this.font, nameBoxX, bottomRowY, NAME_BOX_WIDTH, CONTROL_HEIGHT,
                Component.translatable("radial_teleport.screen.waypoint_edit.rename"));
        this.nameBox.setMaxLength(32);
        this.addRenderableWidget(this.nameBox);

        this.addRenderableWidget(Button.builder(Component.translatable("radial_teleport.screen.waypoint_edit.apply"),
                        button -> applyRename())
                .bounds(applyX, bottomRowY, APPLY_WIDTH, CONTROL_HEIGHT)
                .build());

        this.addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> this.onClose())
                .bounds(doneX, bottomRowY, DONE_WIDTH, CONTROL_HEIGHT)
                .build());
    }

    private void initEmptyDoneButton() {
        int centerX = this.width / 2;
        this.addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> this.onClose())
                .bounds(centerX - 50, bottomRowY, 100, CONTROL_HEIGHT)
                .build());
    }

    private void rebuildRowButtons() {
        this.clearWidgets();
        layoutPanel();
        if (!entries.isEmpty()) {
            initBottomControls();
        } else {
            initEmptyDoneButton();
        }

        for (int viewportIndex = 0; viewportIndex < viewportRows; viewportIndex++) {
            int entryIndex = scrollOffset + viewportIndex;
            if (entryIndex >= entries.size()) {
                break;
            }

            WaypointListEntry entry = entries.get(entryIndex);
            int rowY = listTop + viewportIndex * ROW_HEIGHT;

            this.addRenderableWidget(Button.builder(Component.literal("▲"), button -> move(entry, true))
                    .bounds(listRight - 74, rowY + 4, 20, 20)
                    .build());
            this.addRenderableWidget(Button.builder(Component.literal("▼"), button -> move(entry, false))
                    .bounds(listRight - 50, rowY + 4, 20, 20)
                    .build());
            this.addRenderableWidget(Button.builder(Component.literal("×"), button -> delete(entry))
                    .bounds(listRight - 26, rowY + 4, 20, 20)
                    .build());
        }
    }

    private void select(int index) {
        this.selectedIndex = index;
        ensureSelectedVisible();
        syncNameBox();
    }

    private void syncNameBox() {
        if (this.nameBox == null) {
            return;
        }
        if (selectedIndex >= 0 && selectedIndex < entries.size()) {
            this.nameBox.setValue(entries.get(selectedIndex).name());
            this.nameBox.setEditable(true);
        } else {
            this.nameBox.setValue("");
            this.nameBox.setEditable(false);
        }
    }

    private void applyRename() {
        if (selectedIndex < 0 || selectedIndex >= entries.size()) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.getConnection() == null) {
            return;
        }

        String newName = this.nameBox.getValue().trim();
        if (newName.isEmpty()) {
            return;
        }

        WaypointListEntry entry = entries.get(selectedIndex);
        if (newName.equals(entry.name())) {
            return;
        }

        RadialTeleportCommon.sendToServer.accept(WaypointActionPayload.rename(entry.destinationId(), newName));
    }

    private void move(WaypointListEntry entry, boolean up) {
        RadialTeleportCommon.sendToServer.accept(WaypointActionPayload.move(entry.destinationId(), up));
    }

    private void delete(WaypointListEntry entry) {
        RadialTeleportCommon.sendToServer.accept(WaypointActionPayload.delete(entry.destinationId()));
    }

    private boolean isMouseOverList(double mouseX, double mouseY) {
        return mouseX >= listLeft && mouseX < listRight && mouseY >= listTop && mouseY < listBottom;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT && !entries.isEmpty()) {
            double mouseX = event.x();
            double mouseY = event.y();
            for (int viewportIndex = 0; viewportIndex < viewportRows; viewportIndex++) {
                int entryIndex = scrollOffset + viewportIndex;
                if (entryIndex >= entries.size()) {
                    break;
                }

                int rowY = listTop + viewportIndex * ROW_HEIGHT;
                if (mouseX >= listLeft + 4 && mouseX < listRight - 82
                        && mouseY >= rowY && mouseY < rowY + ROW_HEIGHT - 4) {
                    select(entryIndex);
                    return true;
                }
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (!entries.isEmpty() && maxScrollOffset() > 0 && isMouseOverList(mouseX, mouseY)) {
            int nextOffset = scrollOffset - (int) Math.signum(scrollY);
            if (nextOffset != scrollOffset) {
                scrollOffset = Math.max(0, Math.min(nextOffset, maxScrollOffset()));
                rebuildRowButtons();
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.extractTransparentBackground(guiGraphics);

        int centerX = this.width / 2;
        drawPanel(guiGraphics, listLeft, panelTop, listRight, panelBottom);
        guiGraphics.centeredText(this.font, this.title, centerX, panelTop + 8, 0xFFFFFF);

        if (entries.isEmpty()) {
            guiGraphics.centeredText(this.font,
                    Component.translatable("radial_teleport.screen.waypoint_edit.empty"),
                    centerX, panelTop + 52, 0xFFC0C0C0);
            super.extractRenderState(guiGraphics, mouseX, mouseY, partialTick);
            return;
        }

        guiGraphics.centeredText(this.font,
                Component.translatable("radial_teleport.screen.waypoint_edit.hint"),
                centerX, panelTop + 18, 0xFF909090);

        if (showsScrollHint()) {
            guiGraphics.centeredText(this.font,
                    Component.translatable("radial_teleport.screen.waypoint_edit.scroll_hint"),
                    centerX, panelTop + 28, 0xFF707070);
        }

        guiGraphics.enableScissor(listLeft + 4, listTop, listRight - 4, listBottom);
        try {
            for (int viewportIndex = 0; viewportIndex < viewportRows; viewportIndex++) {
                int entryIndex = scrollOffset + viewportIndex;
                if (entryIndex >= entries.size()) {
                    break;
                }

                WaypointListEntry entry = entries.get(entryIndex);
                int rowY = listTop + viewportIndex * ROW_HEIGHT;
                int background = entryIndex == selectedIndex ? 0x80404080 : 0x40202020;
                guiGraphics.fill(listLeft + 4, rowY, listRight - 82, rowY + ROW_HEIGHT - 4, background);

                guiGraphics.text(this.font, Component.literal(entry.name()), listLeft + 10, rowY + 4, 0xFFFFD080, false);

                String coords = shortDimension(entry.dimensionId()) + "  "
                        + entry.x() + ", " + entry.y() + ", " + entry.z();
                guiGraphics.text(this.font, coords, listLeft + 10, rowY + 16, 0xFFAAAAAA, false);
            }
        } finally {
            guiGraphics.disableScissor();
        }

        super.extractRenderState(guiGraphics, mouseX, mouseY, partialTick);
    }

    private static void drawPanel(GuiGraphicsExtractor guiGraphics, int left, int top, int right, int bottom) {
        guiGraphics.fill(left, top, right, bottom, 0xE010141C);
        guiGraphics.fill(left, top, right, top + 1, 0xFF56CFE1);
        guiGraphics.fill(left, bottom - 1, right, bottom, 0xFF56CFE1);
        guiGraphics.fill(left, top, left + 1, bottom, 0xFF56CFE1);
        guiGraphics.fill(right - 1, top, right, bottom, 0xFF56CFE1);
    }

    private static String shortDimension(String dimensionId) {
        int slash = dimensionId.indexOf(':');
        return slash >= 0 ? dimensionId.substring(slash + 1) : dimensionId;
    }

    @Override
    public void onClose() {
        super.onClose();
        RadialTeleportClientHooks.onWaypointScreenClosed();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
