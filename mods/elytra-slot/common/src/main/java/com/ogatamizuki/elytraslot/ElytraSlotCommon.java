package com.ogatamizuki.elytraslot;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.function.BiConsumer;
import java.util.function.Function;

public final class ElytraSlotCommon {
    public static final String MODID = "elytra_slot";
    public static final Logger LOGGER = LogManager.getLogger(ElytraSlotCommon.class);

    // Platform accessors
    public static Function<Player, ItemStack> getElytraItem = player -> ItemStack.EMPTY;
    public static BiConsumer<Player, ItemStack> setElytraItem = (player, stack) -> {};

    public static Function<Player, ItemStack> getFireworkItem = player -> ItemStack.EMPTY;
    public static BiConsumer<Player, ItemStack> setFireworkItem = (player, stack) -> {};

    public static Function<Player, SlotPositions> getSlotPositions = player -> SlotPositions.DEFAULT;
    public static BiConsumer<Player, SlotPositions> setSlotPositions = (player, pos) -> {};

    public static BiConsumer<ServerPlayer, CustomPacketPayload> sendToPlayer = (player, payload) -> {};
    public static Consumer2<ServerPlayer, CustomPacketPayload> sendToTracking = (player, payload) -> {};
    public static Consumer2<ServerPlayer, CustomPacketPayload> sendToTrackingAndSelf = (player, payload) -> {};
    public static java.util.function.Consumer<CustomPacketPayload> sendToServer = payload -> {};

    @FunctionalInterface
    public interface Consumer2<T, U> {
        void accept(T t, U u);
    }

    private ElytraSlotCommon() {}

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MODID, path);
    }

    public static ItemStack getElytra(Player player) {
        return getElytraItem.apply(player);
    }

    public static void setElytra(Player player, ItemStack stack) {
        setElytraItem.accept(player, stack);
    }

    public static ItemStack getFirework(Player player) {
        return getFireworkItem.apply(player);
    }

    public static void setFirework(Player player, ItemStack stack) {
        setFireworkItem.accept(player, stack);
    }

    public static SlotPositions getPositions(Player player) {
        return getSlotPositions.apply(player);
    }

    public static void setPositions(Player player, SlotPositions positions) {
        setSlotPositions.accept(player, positions);
    }

    /** Sync after container slot changes; local player is updated via container packets. */
    public static void syncSlotToTracking(ServerPlayer player, ItemStack stack) {
        sendToTracking.accept(player, new ElytraSlotSyncPayload(player.getUUID(), stack));
    }

    /** Sync for join, HUD, key actions — includes the owning player. */
    public static void syncSlot(ServerPlayer player, ItemStack stack) {
        sendToTrackingAndSelf.accept(player, new ElytraSlotSyncPayload(player.getUUID(), stack));
    }

    public static void syncElytraSlot(ServerPlayer player, ItemStack stack) {
        syncSlot(player, stack);
    }

    /** Sync after container slot changes; local player is updated via container packets. */
    public static void syncFireworkSlotToTracking(ServerPlayer player, ItemStack stack) {
        sendToTracking.accept(player, new FireworkSlotSyncPayload(player.getUUID(), stack));
    }

    public static void syncFireworkSlot(ServerPlayer player, ItemStack stack) {
        sendToTrackingAndSelf.accept(player, new FireworkSlotSyncPayload(player.getUUID(), stack));
    }

    public static void syncPositions(ServerPlayer player, SlotPositions positions) {
        sendToPlayer.accept(player, new com.ogatamizuki.elytraslot.network.SlotPosSyncPayload(
                positions.elytraX(), positions.elytraY(),
                positions.fireworkX(), positions.fireworkY()
        ));
    }

    /**
     * G key: equip from hotbar / main inventory / offhand, or return to inventory (drop if full).
     * Does not swap with the chest armor slot.
     */
    public static void performQuickSwapElytra(ServerPlayer player) {
        ItemStack slotElytra = getElytra(player);

        if (!slotElytra.isEmpty()) {
            setElytra(player, ItemStack.EMPTY);
            ItemStack toStore = slotElytra.copy();
            if (!player.getInventory().add(toStore)) {
                player.drop(toStore, false);
            }
            syncElytraSlot(player, ItemStack.EMPTY);
            player.inventoryMenu.broadcastChanges();
            return;
        }

        Integer foundSlot = findInventoryElytraSlot(player);
        if (foundSlot == null) {
            return;
        }

        Inventory inventory = player.getInventory();
        ItemStack stack = inventory.getItem(foundSlot);
        if (!stack.is(Items.ELYTRA)) {
            return;
        }

        ItemStack toSlot = stack.copyWithCount(1);
        stack.shrink(1);
        inventory.setItem(foundSlot, stack.isEmpty() ? ItemStack.EMPTY : stack);
        setElytra(player, toSlot);
        syncElytraSlot(player, toSlot);
        player.inventoryMenu.broadcastChanges();
    }

    /**
     * 花火スロットからロケット花火を消費して滑空ブーストする。
     * ItemStack#use(MAIN_HAND) は成功時にメインハンドを上書きするため使わない。
     */
    public static void performFireworkBoost(ServerPlayer player) {
        if (!player.isFallFlying()) {
            return;
        }
        ItemStack fireworkStack = getFirework(player);
        if (fireworkStack.isEmpty() || !fireworkStack.is(Items.FIREWORK_ROCKET)) {
            return;
        }

        ItemStack rocket = fireworkStack.copyWithCount(1);
        player.level().addFreshEntity(new FireworkRocketEntity(player.level(), rocket, player));
        player.level().playSound(
                null,
                player.getX(), player.getY(), player.getZ(),
                SoundEvents.FIREWORK_ROCKET_LAUNCH,
                SoundSource.PLAYERS,
                1.0F,
                1.0F
        );

        if (!player.isCreative()) {
            fireworkStack.shrink(1);
            ItemStack remaining = fireworkStack.isEmpty() ? ItemStack.EMPTY : fireworkStack;
            setFirework(player, remaining);
            syncFireworkSlot(player, remaining);
        }
    }

    private static Integer findInventoryElytraSlot(Player player) {
        Inventory inventory = player.getInventory();
        for (int i = 0; i < 36; i++) {
            if (inventory.getItem(i).is(Items.ELYTRA)) {
                return i;
            }
        }
        if (inventory.getItem(40).is(Items.ELYTRA)) {
            return 40;
        }
        return null;
    }

    public static boolean hasCustomSlots(AbstractContainerMenu menu) {
        for (Slot slot : menu.slots) {
            if (slot instanceof ElytraSlot || slot instanceof FireworkSlot) {
                return true;
            }
        }
        return false;
    }

    public static int[] resolveSlotPositions(Player player) {
        return resolveSurvivalSlotPositions(player);
    }

    public static int[] resolveSurvivalSlotPositions(Player player) {
        SlotPositions pos = getPositions(player);
        int ex = pos.elytraX();
        int ey = pos.elytraY();
        int fx = pos.fireworkX();
        int fy = pos.fireworkY();

        if (ex == 0 && ey == 0) {
            ex = 77;
            ey = 44;
        }
        if (fx == 0 && fy == 0) {
            fx = 77;
            fy = 26;
        }
        return new int[]{ex, ey, fx, fy};
    }

    public static int[] resolveCreativeSlotPositions(Player player) {
        SlotPositions pos = getPositions(player);
        int ex = pos.creativeElytraX();
        int ey = pos.creativeElytraY();
        int fx = pos.creativeFireworkX();
        int fy = pos.creativeFireworkY();

        if (ex == 0 && ey == 0) {
            ex = 127;
            ey = 20;
        }
        if (fx == 0 && fy == 0) {
            fx = 127;
            fy = 2;
        }
        return new int[]{ex, ey, fx, fy};
    }

    public static void updatePlayerContainerSlotPositions(Player player, int ex, int ey, int fx, int fy) {
        try {
            java.lang.reflect.Field xField = Slot.class.getDeclaredField("x");
            java.lang.reflect.Field yField = Slot.class.getDeclaredField("y");
            xField.setAccessible(true);
            yField.setAccessible(true);

            for (Slot slot : player.containerMenu.slots) {
                if (slot instanceof ElytraSlot) {
                    xField.setInt(slot, ex);
                    yField.setInt(slot, ey);
                } else if (slot instanceof FireworkSlot) {
                    xField.setInt(slot, fx);
                    yField.setInt(slot, fy);
                }
            }
        } catch (Exception e) {
            LOGGER.error("Failed to update slot coordinates dynamically", e);
        }
    }
}
