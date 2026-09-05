package com.ogatamizuki.elytraslot.neoforge;

import com.ogatamizuki.elytraslot.ElytraSlotCommon;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.NeoForgeMod;

/**
 * NeoForge 26.2+ grants gliding via the GLIDING_FLIGHT attribute instead of
 * intercepting {@code getItemBySlot} / {@code canGlide}.
 */
public final class ElytraSlotNeoForgeGliding {
    public static final Identifier ELYTRA_GLIDE_MODIFIER_ID =
            ElytraSlotCommon.id("custom_elytra_glide");

    private ElytraSlotNeoForgeGliding() {}

    public static void apply(ServerPlayer player) {
        AttributeInstance attr = player.getAttribute(NeoForgeMod.GLIDING_FLIGHT);
        if (attr != null && attr.getModifier(ELYTRA_GLIDE_MODIFIER_ID) == null) {
            attr.addPermanentModifier(new AttributeModifier(
                    ELYTRA_GLIDE_MODIFIER_ID, 1.0, AttributeModifier.Operation.ADD_VALUE
            ));
        }
    }

    public static void remove(ServerPlayer player) {
        AttributeInstance attr = player.getAttribute(NeoForgeMod.GLIDING_FLIGHT);
        if (attr != null) {
            attr.removeModifier(ELYTRA_GLIDE_MODIFIER_ID);
        }
    }

    public static void onElytraChanged(ServerPlayer player, ItemStack previous, ItemStack current) {
        boolean had = !previous.isEmpty() && previous.is(Items.ELYTRA);
        boolean has = !current.isEmpty() && current.is(Items.ELYTRA);
        if (!had && has) {
            apply(player);
        } else if (had && !has) {
            remove(player);
        }
    }

    /**
     * Shift cancels custom-slot glide (chest elytra keeps vanilla behavior).
     * Re-applies the attribute when sneak ends so jump-to-glide still works.
     */
    public static void tickSneakCancel(ServerPlayer player) {
        ItemStack custom = ElytraSlotCommon.getElytra(player);
        boolean hasCustom = custom.is(Items.ELYTRA) && custom.getDamageValue() < custom.getMaxDamage() - 1;
        if (!hasCustom || player.getItemBySlot(EquipmentSlot.CHEST).is(Items.ELYTRA)) {
            return;
        }
        if (player.isShiftKeyDown()) {
            remove(player);
            if (player.isFallFlying()) {
                player.stopFallFlying();
            }
        } else {
            apply(player);
        }
    }
}
