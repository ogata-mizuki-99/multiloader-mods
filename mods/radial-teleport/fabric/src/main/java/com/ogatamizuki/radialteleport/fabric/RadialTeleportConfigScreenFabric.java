package com.ogatamizuki.radialteleport.fabric;

import com.ogatamizuki.radialteleport.Config;
import com.ogatamizuki.radialteleport.RadialTeleportCommonConfigPushPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class RadialTeleportConfigScreenFabric extends Screen {
    private final Screen parent;
    private boolean enableCraftingRecipe;
    private boolean enableWaypoints;
    private int maxWaypointsPerPlayer;
    private int teleportCooldownTicks;

    private EditBox maxWaypointsBox;
    private EditBox cooldownBox;

    public RadialTeleportConfigScreenFabric(Screen parent) {
        super(Component.translatable("radial_teleport.configuration.title"));
        this.parent = parent;
        this.enableCraftingRecipe = Config.isEnableCraftingRecipe();
        this.enableWaypoints = Config.isEnableWaypoints();
        this.maxWaypointsPerPlayer = Config.getMaxWaypointsPerPlayer();
        this.teleportCooldownTicks = Config.getTeleportCooldownTicks();
    }

    @Override
    protected void init() {
        super.init();
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        addRenderableWidget(Checkbox.builder(
                Component.translatable("radial_teleport.config.enable_crafting_recipe"), this.font)
                .pos(centerX - 120, centerY - 60)
                .selected(enableCraftingRecipe)
                .onValueChange((cb, val) -> {
                    this.enableCraftingRecipe = val;
                    Config.setEnableCraftingRecipe(val);
                })
                .build());

        addRenderableWidget(Checkbox.builder(
                Component.translatable("radial_teleport.config.enable_waypoints"), this.font)
                .pos(centerX - 120, centerY - 35)
                .selected(enableWaypoints)
                .onValueChange((cb, val) -> {
                    this.enableWaypoints = val;
                    Config.setEnableWaypoints(val);
                })
                .build());

        this.maxWaypointsBox = new EditBox(this.font, centerX + 20, centerY - 10, 60, 18, Component.literal("Max Waypoints"));
        this.maxWaypointsBox.setValue(String.valueOf(this.maxWaypointsPerPlayer));
        this.maxWaypointsBox.setResponder(val -> {
            try {
                int parsed = Integer.parseInt(val.trim());
                this.maxWaypointsPerPlayer = Math.max(1, Math.min(32, parsed));
                Config.setMaxWaypointsPerPlayer(this.maxWaypointsPerPlayer);
            } catch (NumberFormatException ignored) {}
        });
        addRenderableWidget(this.maxWaypointsBox);

        this.cooldownBox = new EditBox(this.font, centerX + 20, centerY + 15, 60, 18, Component.literal("Cooldown Ticks"));
        this.cooldownBox.setValue(String.valueOf(this.teleportCooldownTicks));
        this.cooldownBox.setResponder(val -> {
            try {
                int parsed = Integer.parseInt(val.trim());
                this.teleportCooldownTicks = Math.max(0, Math.min(72000, parsed));
                Config.setTeleportCooldownTicks(this.teleportCooldownTicks);
            } catch (NumberFormatException ignored) {}
        });
        addRenderableWidget(this.cooldownBox);

        addRenderableWidget(Button.builder(
                Component.translatable("radial_teleport.configuration.push_to_server"),
                button -> pushToServer()
        ).bounds(centerX - 110, centerY + 45, 220, 20).build());

        addRenderableWidget(Button.builder(
                Component.translatable("gui.done"),
                button -> this.minecraft.gui.setScreen(parent)
        ).bounds(centerX - 50, centerY + 72, 100, 20).build());
    }

    private void pushToServer() {
        Minecraft mc = this.minecraft;
        if (mc == null || mc.player == null) {
            return;
        }
        if (mc.getConnection() == null) {
            mc.player.sendSystemMessage(
                    Component.translatable("radial_teleport.configuration.push_need_connection")
                            .withStyle(ChatFormatting.RED));
            return;
        }
        ClientPlayNetworking.send(new RadialTeleportCommonConfigPushPayload(
                this.enableCraftingRecipe,
                this.enableWaypoints,
                this.maxWaypointsPerPlayer,
                this.teleportCooldownTicks
        ));
        mc.player.sendSystemMessage(
                Component.translatable("radial_teleport.configuration.push_sent").withStyle(ChatFormatting.YELLOW));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.extractTransparentBackground(guiGraphics);
        guiGraphics.centeredText(this.font, this.title, this.width / 2, this.height / 2 - 80, 0xFFFFFFFF);
        guiGraphics.text(this.font, Component.translatable("radial_teleport.config.max_waypoints_per_player"), this.width / 2 - 120, this.height / 2 - 5, 0xFFE0E0E0);
        guiGraphics.text(this.font, Component.translatable("radial_teleport.config.teleport_cooldown_ticks"), this.width / 2 - 120, this.height / 2 + 20, 0xFFE0E0E0);
        super.extractRenderState(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose() {
        this.minecraft.gui.setScreen(parent);
    }
}
