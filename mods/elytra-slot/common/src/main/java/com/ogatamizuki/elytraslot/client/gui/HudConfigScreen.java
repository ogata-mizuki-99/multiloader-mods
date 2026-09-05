package com.ogatamizuki.elytraslot.client.gui;

import com.ogatamizuki.elytraslot.Config;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.KeyEvent;
import org.lwjgl.glfw.GLFW;

public class HudConfigScreen extends Screen {
    private final Screen parent;

    // Temporary offset positions
    private int elytraHudX;
    private int elytraHudY;
    private int fireworkHudX;
    private int fireworkHudY;

    // Dragging state
    private int selectedHud = 0; // 0: None, 1: Elytra HUD, 2: Firework HUD
    private boolean isDragging = false;
    private int dragOffsetX = 0;
    private int dragOffsetY = 0;

    public HudConfigScreen(Screen parent) {
        super(Component.translatable("elytra_slot.screen.hud_config.title"));
        this.parent = parent;
        this.elytraHudX = Config.ELYTRA_HUD_X.get();
        this.elytraHudY = Config.ELYTRA_HUD_Y.get();
        this.fireworkHudX = Config.FIREWORK_HUD_X.get();
        this.fireworkHudY = Config.FIREWORK_HUD_Y.get();
    }

    @Override
    protected void init() {
        super.init();

        int centerX = this.width / 2;

        addRenderableWidget(Button.builder(Component.translatable("elytra_slot.screen.config.save"), btn -> {
            Config.ELYTRA_HUD_X.set(this.elytraHudX);
            Config.ELYTRA_HUD_Y.set(this.elytraHudY);
            Config.FIREWORK_HUD_X.set(this.fireworkHudX);
            Config.FIREWORK_HUD_Y.set(this.fireworkHudY);
            Config.ELYTRA_HUD_X.save();
            Config.ELYTRA_HUD_Y.save();
            Config.FIREWORK_HUD_X.save();
            Config.FIREWORK_HUD_Y.save();
            this.minecraft.gui.setScreen(parent);
        }).bounds(centerX - 115, this.height - 60, 70, 20).build());

        addRenderableWidget(Button.builder(Component.translatable("elytra_slot.screen.config.default"), btn -> {
            this.elytraHudX = -150;
            this.elytraHudY = -20;
            this.fireworkHudX = -170;
            this.fireworkHudY = -20;
            this.selectedHud = 0;
        }).bounds(centerX - 35, this.height - 60, 70, 20).build());

        addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), btn -> {
            this.minecraft.gui.setScreen(parent);
        }).bounds(centerX + 45, this.height - 60, 70, 20).build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.extractTransparentBackground(guiGraphics);

        int centerX = this.width / 2;
        int bottom = this.height;

        // 1. Draw Simulated Hotbar (182x22)
        int hX = centerX - 91;
        int hY = bottom - 22;
        guiGraphics.fill(hX, hY, hX + 182, hY + 22, 0x80333333); // Translucent background
        // Draw borders
        guiGraphics.fill(hX, hY, hX + 182, hY + 1, 0xFF777777);
        guiGraphics.fill(hX, hY + 21, hX + 182, hY + 22, 0xFF777777);
        guiGraphics.fill(hX, hY, hX + 1, hY + 22, 0xFF777777);
        guiGraphics.fill(hX + 181, hY, hX + 182, hY + 22, 0xFF777777);
        // Label it
        guiGraphics.centeredText(this.font, Component.translatable("elytra_slot.screen.config.hotbar"), centerX, hY + 7,
                0xAAFFFFFF);

        // 2. Draw Elytra HUD Preview
        int ex = centerX + elytraHudX;
        int ey = bottom + elytraHudY;
        if (selectedHud == 1) {
            guiGraphics.fill(ex - 2, ey - 2, ex + 18, ey + 18, 0x6000FF00); // Selected outline
        } else {
            guiGraphics.fill(ex - 2, ey - 2, ex + 18, ey + 18, 0x30FFFFFF);
        }
        guiGraphics.fakeItem(new ItemStack(Items.ELYTRA), ex, ey);
        guiGraphics.pose().pushMatrix();
        guiGraphics.pose().scale(0.75f, 0.75f);
        guiGraphics.text(this.font, "100%", (int) ((ex + 14) / 0.75f), (int) ((ey + 10) / 0.75f), 0xFFFFFFFF, true);
        guiGraphics.pose().popMatrix();

        // 3. Draw Firework HUD Preview
        int fx = centerX + fireworkHudX;
        int fy = bottom + fireworkHudY;
        if (selectedHud == 2) {
            guiGraphics.fill(fx - 2, fy - 2, fx + 18, fy + 18, 0x6000FF00);
        } else {
            guiGraphics.fill(fx - 2, fy - 2, fx + 18, fy + 18, 0x30FFFFFF);
        }
        guiGraphics.fakeItem(new ItemStack(Items.FIREWORK_ROCKET), fx, fy);
        guiGraphics.pose().pushMatrix();
        guiGraphics.pose().scale(0.85f, 0.85f);
        guiGraphics.text(this.font, "64", (int) ((fx + 12) / 0.85f), (int) ((fy + 10) / 0.85f), 0xFFFFFFFF, true);
        guiGraphics.pose().popMatrix();

        // 4. Draw instructions
        guiGraphics.centeredText(this.font, this.title, centerX, bottom - 110, 0xFFFFFFFF);
        guiGraphics.centeredText(this.font, Component.translatable("elytra_slot.screen.config.hud_instructions"),
                centerX, bottom - 87, 0xAAAAAAFF);

        super.extractRenderState(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mouseX = event.x();
        double mouseY = event.y();

        int centerX = this.width / 2;
        int bottom = this.height;

        // Check Elytra HUD
        int ex = centerX + elytraHudX;
        int ey = bottom + elytraHudY;
        if (mouseX >= ex - 2 && mouseX < ex + 18 && mouseY >= ey - 2 && mouseY < ey + 18) {
            selectedHud = 1;
            isDragging = true;
            dragOffsetX = (int) mouseX - ex;
            dragOffsetY = (int) mouseY - ey;
            return true;
        }

        // Check Firework HUD
        int fx = centerX + fireworkHudX;
        int fy = bottom + fireworkHudY;
        if (mouseX >= fx - 2 && mouseX < fx + 18 && mouseY >= fy - 2 && mouseY < fy + 18) {
            selectedHud = 2;
            isDragging = true;
            dragOffsetX = (int) mouseX - fx;
            dragOffsetY = (int) mouseY - fy;
            return true;
        }

        selectedHud = 0;
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (isDragging) {
            double mouseX = event.x();
            double mouseY = event.y();
            int centerX = this.width / 2;
            int bottom = this.height;

            int targetX = (int) mouseX - centerX - dragOffsetX;
            int targetY = (int) mouseY - bottom - dragOffsetY;

            // Clamp offsets to keep HUD on screen
            targetX = Math.max(-centerX, Math.min(centerX - 16, targetX));
            targetY = Math.max(-bottom, Math.min(-16, targetY));

            if (selectedHud == 1) {
                elytraHudX = targetX;
                elytraHudY = targetY;
            } else if (selectedHud == 2) {
                fireworkHudX = targetX;
                fireworkHudY = targetY;
            }
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        isDragging = false;
        return super.mouseReleased(event);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (selectedHud != 0) {
            int moveX = 0;
            int moveY = 0;
            int keyCode = event.key();

            if (keyCode == GLFW.GLFW_KEY_UP) {
                moveY = -1;
            } else if (keyCode == GLFW.GLFW_KEY_DOWN) {
                moveY = 1;
            } else if (keyCode == GLFW.GLFW_KEY_LEFT) {
                moveX = -1;
            } else if (keyCode == GLFW.GLFW_KEY_RIGHT) {
                moveX = 1;
            }

            if (moveX != 0 || moveY != 0) {
                if (selectedHud == 1) {
                    elytraHudX = Math.max(-this.width / 2, Math.min(this.width / 2 - 16, elytraHudX + moveX));
                    elytraHudY = Math.max(-this.height, Math.min(-16, elytraHudY + moveY));
                } else if (selectedHud == 2) {
                    fireworkHudX = Math.max(-this.width / 2, Math.min(this.width / 2 - 16, fireworkHudX + moveX));
                    fireworkHudY = Math.max(-this.height, Math.min(-16, fireworkHudY + moveY));
                }
                return true;
            }
        }
        return super.keyPressed(event);
    }
}
