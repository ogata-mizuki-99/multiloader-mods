package com.ogatamizuki.radialteleport.client;

import com.ogatamizuki.radialteleport.RadialTeleportCommon;
import com.ogatamizuki.radialteleport.WaypointActionPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;

public class WaypointSaveScreen extends Screen {
    private static final int NAME_BOX_WIDTH = 220;

    private EditBox nameBox;

    protected WaypointSaveScreen() {
        super(Component.translatable("radial_teleport.screen.waypoint_save.title"));
    }

    public static void open() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            RadialTeleportClientHooks.onWaypointScreenOpened();
            mc.setScreen(new WaypointSaveScreen());
        }
    }

    @Override
    protected void init() {
        super.init();

        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            this.onClose();
            return;
        }

        int centerX = this.width / 2;
        int centerY = this.height / 2;

        String defaultName = (int) player.getX() + ", " + (int) player.getY() + ", " + (int) player.getZ();
        this.nameBox = new EditBox(this.font, centerX - NAME_BOX_WIDTH / 2, centerY - 4, NAME_BOX_WIDTH, 20,
                Component.translatable("radial_teleport.screen.waypoint_save.name"));
        this.nameBox.setMaxLength(32);
        this.nameBox.setValue(defaultName);
        this.addRenderableWidget(this.nameBox);
        this.setInitialFocus(this.nameBox);

        this.addRenderableWidget(Button.builder(Component.translatable("radial_teleport.screen.waypoint_save.save"),
                        button -> save())
                .bounds(centerX - 105, centerY + 36, 100, 20)
                .build());

        this.addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), button -> this.onClose())
                .bounds(centerX + 5, centerY + 36, 100, 20)
                .build());
    }

    private void save() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.getConnection() == null) {
            return;
        }

        String name = this.nameBox.getValue().trim();
        if (name.isEmpty()) {
            return;
        }

        RadialTeleportCommon.sendToServer.accept(WaypointActionPayload.save(name));
        this.onClose();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.extractTransparentBackground(guiGraphics);

        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) {
            int centerX = this.width / 2;
            int centerY = this.height / 2;

            guiGraphics.centeredText(this.font, this.title, centerX, centerY - 58, 0xFFFFFF);

            String dimension = player.level().dimension().identifier().toString();
            String coords = (int) player.getX() + ", " + (int) player.getY() + ", " + (int) player.getZ();
            guiGraphics.centeredText(this.font,
                    Component.translatable("radial_teleport.screen.waypoint_save.location", dimension, coords),
                    centerX, centerY - 38, 0xA0A0A0);

            guiGraphics.centeredText(this.font,
                    Component.translatable("radial_teleport.screen.waypoint_save.name_hint"),
                    centerX, centerY - 22, 0xC0C0C0);
        }

        super.extractRenderState(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
