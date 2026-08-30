package com.ogatamizuki.sleep.fabric;

import com.ogatamizuki.sleep.SleepCommon;
import com.ogatamizuki.sleep.SleepCommonConfigPushPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class SleepConfigScreenFabric extends Screen {
    private final Screen parent;

    private boolean allowDaySleep;
    private boolean healWhileSleeping;
    private boolean onePlayerSkip;

    public SleepConfigScreenFabric(Screen parent) {
        super(Component.translatable("good_sleep.configuration.title"));
        this.parent = parent;
        this.allowDaySleep = SleepCommon.allowDaySleep;
        this.healWhileSleeping = SleepCommon.healWhileSleeping;
        this.onePlayerSkip = SleepCommon.onePlayerSkip;
    }

    @Override
    protected void init() {
        super.init();
        int centerX = this.width / 2;
        int startY = this.height / 2 - 50;

        addRenderableWidget(Checkbox.builder(
                Component.translatable("good_sleep.configuration.allowDaySleep.name"), this.font)
                .pos(centerX - 120, startY)
                .selected(allowDaySleep)
                .onValueChange((cb, value) -> this.allowDaySleep = value)
                .build());

        addRenderableWidget(Checkbox.builder(
                Component.translatable("good_sleep.configuration.healWhileSleeping.name"), this.font)
                .pos(centerX - 120, startY + 28)
                .selected(healWhileSleeping)
                .onValueChange((cb, value) -> this.healWhileSleeping = value)
                .build());

        addRenderableWidget(Checkbox.builder(
                Component.translatable("good_sleep.configuration.onePlayerSkip.name"), this.font)
                .pos(centerX - 120, startY + 56)
                .selected(onePlayerSkip)
                .onValueChange((cb, value) -> this.onePlayerSkip = value)
                .build());

        addRenderableWidget(Button.builder(
                Component.translatable("good_sleep.configuration.push_to_server"),
                btn -> pushToServer()
        ).bounds(centerX - 110, startY + 92, 220, 20).build());

        addRenderableWidget(Button.builder(
                Component.translatable("gui.done"),
                btn -> this.minecraft.setScreen(parent)
        ).bounds(centerX - 50, startY + 118, 100, 20).build());
    }

    private void pushToServer() {
        Minecraft mc = this.minecraft;
        if (mc == null || mc.player == null) return;
        SleepCommon.allowDaySleep = this.allowDaySleep;
        SleepCommon.healWhileSleeping = this.healWhileSleeping;
        SleepCommon.onePlayerSkip = this.onePlayerSkip;
        if (ClientPlayNetworking.canSend(SleepCommonConfigPushPayload.TYPE)) {
            ClientPlayNetworking.send(SleepCommonConfigPushPayload.fromLocalConfig());
            mc.player.sendSystemMessage(
                    Component.translatable("good_sleep.configuration.push_sent")
                            .withStyle(ChatFormatting.YELLOW));
        }
        this.minecraft.setScreen(parent);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.extractTransparentBackground(guiGraphics);
        guiGraphics.centeredText(this.font, this.title, this.width / 2, this.height / 2 - 75, 0xFFFFFFFF);
        super.extractRenderState(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(parent);
    }
}
