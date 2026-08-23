package com.ogatamizuki.lookalike.client;

import com.ogatamizuki.lookalike.LookalikeAttachments.ScanEntry;
import com.ogatamizuki.lookalike.NetworkPayloads.ScanHistoryActionPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.PlayerFaceExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ScanHistoryEditScreen extends Screen {
    private static final int PANEL_WIDTH = 380;
    private static final int PANEL_MARGIN = 32;
    private static final int ROW_HEIGHT = 28;
    private static final int TITLE_HEIGHT = 18;
    private static final int HINT_PADDING = 8;
    private static final int PANEL_BOTTOM_PAD = 8;
    private static final int FACE_SIZE = 16;

    private List<ScanEntry> entries = List.of();
    private List<FormattedCharSequence> hintLines = List.of();
    private int listLeft;
    private int listRight;
    private int listTop;
    private int panelTop;
    private int panelBottom;
    private int headerHeight;

    protected ScanHistoryEditScreen() {
        super(Component.translatable("lookalike.screen.scan_history.title"));
    }

    public static void open() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }
        LookalikeModClient.onScanScreenOpened();
        ScanHistoryEditScreen screen = new ScanHistoryEditScreen();
        screen.entries = new ArrayList<>(LookalikeModClient.getScanHistory());
        mc.setScreen(screen);
    }

    public void updateEntries(List<ScanEntry> nextEntries) {
        this.entries = new ArrayList<>(nextEntries);
        this.rebuildRowButtons();
    }

    @Override
    protected void init() {
        super.init();
        rebuildRowButtons();
    }

    private void layoutPanel() {
        int centerX = this.width / 2;
        int panelWidth = Math.min(PANEL_WIDTH, Math.max(280, this.width - PANEL_MARGIN * 2));
        int textWidth = panelWidth - 16;
        this.hintLines = this.font.split(Component.translatable("lookalike.screen.scan_history.hint"), textWidth);
        this.headerHeight = TITLE_HEIGHT + HINT_PADDING + this.hintLines.size() * (this.font.lineHeight + 2) + 8;

        this.listLeft = centerX - panelWidth / 2;
        this.listRight = centerX + panelWidth / 2;

        int rowCount = Math.max(1, entries.size());
        int panelHeight = headerHeight + rowCount * ROW_HEIGHT + PANEL_BOTTOM_PAD + 36;
        this.panelTop = (this.height - panelHeight) / 2;
        this.panelBottom = panelTop + panelHeight;
        this.listTop = panelTop + headerHeight;
    }

    private void rebuildRowButtons() {
        this.clearWidgets();
        layoutPanel();

        int centerX = this.width / 2;
        for (int i = 0; i < entries.size(); i++) {
            int rowY = listTop + i * ROW_HEIGHT;
            int finalI = i;
            this.addRenderableWidget(Button.builder(
                            Component.translatable("lookalike.screen.scan_history.delete"),
                            button -> delete(entries.get(finalI)))
                    .bounds(listRight - 72, rowY + 2, 64, 20)
                    .build());
        }

        this.addRenderableWidget(Button.builder(
                        Component.translatable("gui.done"),
                        button -> this.onClose())
                .bounds(centerX - 40, panelBottom - 28, 80, 20)
                .build());
    }

    private void delete(ScanEntry entry) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.getConnection() != null) {
            mc.getConnection().send(ScanHistoryActionPayload.delete(entry.uuid().toString()));
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.extractTransparentBackground(guiGraphics);

        int centerX = this.width / 2;
        drawPanel(guiGraphics, listLeft, panelTop, listRight, panelBottom);
        guiGraphics.centeredText(this.font, this.title, centerX, panelTop + 6, 0xFFFFFF);

        if (entries.isEmpty()) {
            int hintY = panelTop + TITLE_HEIGHT + HINT_PADDING + 4;
            for (FormattedCharSequence line : hintLines) {
                guiGraphics.centeredText(this.font, line, centerX, hintY, 0xFF909090);
                hintY += this.font.lineHeight + 2;
            }
            guiGraphics.centeredText(this.font,
                    Component.translatable("lookalike.screen.scan_history.empty"),
                    centerX, listTop + 12, 0xFFC0C0C0);
            super.extractRenderState(guiGraphics, mouseX, mouseY, partialTick);
            return;
        }

        int hintY = panelTop + TITLE_HEIGHT + HINT_PADDING;
        for (FormattedCharSequence line : hintLines) {
            guiGraphics.centeredText(this.font, line, centerX, hintY, 0xFF909090);
            hintY += this.font.lineHeight + 2;
        }

        Minecraft mc = Minecraft.getInstance();
        UUID selfUuid = mc.player != null ? mc.player.getUUID() : null;
        for (int i = 0; i < entries.size(); i++) {
            ScanEntry entry = entries.get(i);
            int rowY = listTop + i * ROW_HEIGHT;
            guiGraphics.fill(listLeft + 4, rowY, listRight - 76, rowY + ROW_HEIGHT - 4, 0x40202020);

            var skin = selfUuid != null
                    ? LookalikeClientSkins.resolveIcon(entry.uuid(), entry.name(), false, selfUuid)
                    : null;
            int faceX = listLeft + 8;
            int faceY = rowY + 4;
            if (skin != null) {
                guiGraphics.fill(faceX - 1, faceY - 1, faceX + FACE_SIZE + 1, faceY + FACE_SIZE + 1, 0xFF101820);
                PlayerFaceExtractor.extractRenderState(guiGraphics, skin, faceX, faceY, FACE_SIZE);
            } else {
                guiGraphics.fill(faceX, faceY, faceX + FACE_SIZE, faceY + FACE_SIZE, 0xFF546E7A);
            }

            guiGraphics.text(this.font, entry.name(), listLeft + 30, rowY + 8, 0xFFF4F7FB, false);
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

    @Override
    public void onClose() {
        super.onClose();
        LookalikeModClient.onScanScreenClosed();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
