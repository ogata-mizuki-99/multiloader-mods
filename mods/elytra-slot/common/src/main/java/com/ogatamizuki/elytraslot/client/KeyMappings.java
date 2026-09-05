package com.ogatamizuki.elytraslot.client;

import com.ogatamizuki.elytraslot.Config;
import com.ogatamizuki.elytraslot.ElytraSlotCommon;
import com.ogatamizuki.elytraslot.network.ActionPayload;
import com.ogatamizuki.elytraslot.network.SlotPosSyncPayload;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

public class KeyMappings {
    public static final KeyMapping.Category CATEGORY = new KeyMapping.Category(
            Identifier.fromNamespaceAndPath("elytra_slot", "main")
    );

    public static final KeyMapping QUICK_SWAP_KEY = new KeyMapping(
            "key.elytra_slot.quick_swap",
            GLFW.GLFW_KEY_G,
            CATEGORY
    );

    /** Glide boost via firework slot. Default: R (rebindable in Controls). */
    public static final KeyMapping FIREWORK_BOOST_KEY = new KeyMapping(
            "key.elytra_slot.firework_boost",
            GLFW.GLFW_KEY_R,
            CATEGORY
    );

    private static int fireworkCooldown = 0;
    private static int warningCooldown = 0;
    private static boolean hasSyncedPos = false;

    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            hasSyncedPos = false;
            return;
        }

        // Send slot coordinates on first login to sync server-side click boundaries
        if (!hasSyncedPos && mc.getConnection() != null) {
            ElytraSlotCommon.sendToServer.accept(new SlotPosSyncPayload(
                    Config.ELYTRA_SLOT_X.get(),
                    Config.ELYTRA_SLOT_Y.get(),
                    Config.FIREWORK_SLOT_X.get(),
                    Config.FIREWORK_SLOT_Y.get()
            ));
            hasSyncedPos = true;
        }

        if (mc.gui.screen() != null) {
            return;
        }

        while (QUICK_SWAP_KEY.consumeClick()) {
            if (mc.getConnection() != null) {
                ElytraSlotCommon.sendToServer.accept(new ActionPayload(ActionPayload.ACTION_SWAP_ELYTRA));
            }
        }

        if (fireworkCooldown > 0) {
            fireworkCooldown--;
        }
        if (warningCooldown > 0) {
            warningCooldown--;
        }

        // Dedicated boost key (default R; rebindable) while gliding
        if (mc.player.isFallFlying()) {
            if (FIREWORK_BOOST_KEY.isDown() && fireworkCooldown == 0) {
                if (mc.getConnection() != null) {
                    ElytraSlotCommon.sendToServer.accept(new ActionPayload(ActionPayload.ACTION_FIREWORK_BOOST));
                }
                fireworkCooldown = 20; // 1 second cooldown
            }

            // Elytra Durability Warning
            ItemStack elytra = ElytraSlotCommon.getElytra(mc.player);
            if (!elytra.isEmpty() && elytra.isDamageableItem()) {
                int maxDamage = elytra.getMaxDamage();
                int damage = elytra.getDamageValue();
                double pct = (double) (maxDamage - damage) / maxDamage;
                if (pct <= Config.WARNING_THRESHOLD.get() && warningCooldown == 0) {
                    mc.player.sendSystemMessage(
                            Component.literal("§c?? Elytra Durability Low! (" + (int) (pct * 100) + "%)")
                    );
                    mc.player.playSound(
                            net.minecraft.sounds.SoundEvents.NOTE_BLOCK_BASS.value(),
                            1.0f,
                            1.0f
                    );
                    warningCooldown = 100; // 5 seconds cooldown
                }
            }
        }
    }
}
