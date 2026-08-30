package com.ogatamizuki.economy.fabric;

import com.ogatamizuki.economy.EconomyCommonConfigPushPayload;
import com.ogatamizuki.economy.EconomyRuntimeConfig;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class EconomyConfigScreenFabric extends Screen {
    private final Screen parent;
    private boolean enableBalanceHud;
    private boolean enableActionRewards;
    private boolean enableEtfUpdates;
    private int rewardChatAggregateSeconds;
    private EditBox aggregateBox;

    public EconomyConfigScreenFabric(Screen parent) {
        super(Component.translatable("economy.configuration.title"));
        this.parent = parent;
        this.enableBalanceHud = EconomyRuntimeConfig.enableBalanceHud;
        this.enableActionRewards = EconomyRuntimeConfig.enableActionRewards;
        this.enableEtfUpdates = EconomyRuntimeConfig.enableEtfUpdates;
        this.rewardChatAggregateSeconds = EconomyRuntimeConfig.rewardChatAggregateSeconds;
    }

    @Override
    protected void init() {
        super.init();
        int centerX = this.width / 2;
        int startY = this.height / 2 - 70;

        addRenderableWidget(Checkbox.builder(
                Component.translatable("economy.configuration.enableBalanceHud.name"), this.font)
                .pos(centerX - 120, startY)
                .selected(enableBalanceHud)
                .onValueChange((cb, value) -> this.enableBalanceHud = value)
                .build());

        addRenderableWidget(Checkbox.builder(
                Component.translatable("economy.configuration.enableActionRewards.name"), this.font)
                .pos(centerX - 120, startY + 28)
                .selected(enableActionRewards)
                .onValueChange((cb, value) -> this.enableActionRewards = value)
                .build());

        addRenderableWidget(Checkbox.builder(
                Component.translatable("economy.configuration.enableEtfUpdates.name"), this.font)
                .pos(centerX - 120, startY + 56)
                .selected(enableEtfUpdates)
                .onValueChange((cb, value) -> this.enableEtfUpdates = value)
                .build());

        this.aggregateBox = new EditBox(
                this.font,
                centerX + 40,
                startY + 84,
                60,
                20,
                Component.translatable("economy.configuration.rewardChatAggregateSeconds.name")
        );
        this.aggregateBox.setValue(String.valueOf(rewardChatAggregateSeconds));
        this.aggregateBox.setResponder(text -> {
            if (text.isEmpty()) {
                return;
            }
            try {
                this.rewardChatAggregateSeconds = Integer.parseInt(text);
            } catch (NumberFormatException ignored) {
            }
        });
        addRenderableWidget(this.aggregateBox);

        addRenderableWidget(Button.builder(
                Component.translatable("economy.configuration.push_to_server"),
                btn -> pushToServer()
        ).bounds(centerX - 110, startY + 112, 220, 20).build());

        addRenderableWidget(Button.builder(
                Component.translatable("gui.done"),
                btn -> saveLocalAndClose()
        ).bounds(centerX - 50, startY + 138, 100, 20).build());
    }

    private void applyLocal() {
        EconomyRuntimeConfig.enableBalanceHud = enableBalanceHud;
        EconomyRuntimeConfig.enableActionRewards = enableActionRewards;
        EconomyRuntimeConfig.enableEtfUpdates = enableEtfUpdates;
        EconomyRuntimeConfig.rewardChatAggregateSeconds = Math.max(0, Math.min(30, rewardChatAggregateSeconds));
    }

    private void saveLocalAndClose() {
        applyLocal();
        EconomyFabricConfig config = new EconomyFabricConfig();
        config.syncFromRuntime();
        config.save();
        this.minecraft.setScreen(parent);
    }

    private void pushToServer() {
        applyLocal();
        Minecraft mc = this.minecraft;
        if (mc == null || mc.player == null) {
            return;
        }
        if (!ClientPlayNetworking.canSend(EconomyCommonConfigPushPayload.TYPE)) {
            mc.player.sendSystemMessage(
                    Component.translatable("economy.configuration.push_need_connection")
                            .withStyle(ChatFormatting.RED));
            return;
        }
        ClientPlayNetworking.send(EconomyCommonConfigPushPayload.fromLocalConfig());
        mc.player.sendSystemMessage(
                Component.translatable("economy.configuration.push_sent").withStyle(ChatFormatting.YELLOW));
        saveLocalAndClose();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        this.extractTransparentBackground(graphics);
        graphics.centeredText(this.font, this.title, this.width / 2, 20, 0xFFFFFFFF);
        graphics.centeredText(
                this.font,
                Component.translatable("economy.configuration.push_hint"),
                this.width / 2,
                36,
                0xFFAAAAAA
        );
        int centerX = this.width / 2;
        int startY = this.height / 2 - 70;
        graphics.text(
                this.font,
                Component.translatable("economy.configuration.rewardChatAggregateSeconds.name"),
                centerX - 120,
                startY + 90,
                0xFFFFFFFF,
                false
        );
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose() {
        saveLocalAndClose();
    }
}
