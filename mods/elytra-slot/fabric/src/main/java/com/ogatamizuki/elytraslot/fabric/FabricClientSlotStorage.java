package com.ogatamizuki.elytraslot.fabric;

import com.ogatamizuki.elytraslot.SlotPositions;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Client-side slot storage, separate from server maps (fixes integrated-server duplication). */
final class FabricClientSlotStorage {
    private static final Map<UUID, ItemStack> ELYTRA = new ConcurrentHashMap<>();
    private static final Map<UUID, ItemStack> FIREWORK = new ConcurrentHashMap<>();
    private static final Map<UUID, SlotPositions> POSITIONS = new ConcurrentHashMap<>();

    private FabricClientSlotStorage() {}

    static ItemStack getElytra(Player player) {
        return ELYTRA.getOrDefault(player.getUUID(), ItemStack.EMPTY);
    }

    static void setElytra(Player player, ItemStack stack) {
        if (stack.isEmpty()) {
            ELYTRA.remove(player.getUUID());
        } else {
            ELYTRA.put(player.getUUID(), stack.copy());
        }
    }

    static ItemStack getFirework(Player player) {
        return FIREWORK.getOrDefault(player.getUUID(), ItemStack.EMPTY);
    }

    static void setFirework(Player player, ItemStack stack) {
        if (stack.isEmpty()) {
            FIREWORK.remove(player.getUUID());
        } else {
            FIREWORK.put(player.getUUID(), stack.copy());
        }
    }

    static SlotPositions getPositions(Player player) {
        return POSITIONS.getOrDefault(player.getUUID(), SlotPositions.DEFAULT);
    }

    static void setPositions(Player player, SlotPositions pos) {
        POSITIONS.put(player.getUUID(), pos);
    }
}
