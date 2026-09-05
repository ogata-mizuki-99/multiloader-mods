package com.ogatamizuki.elytraslot.fabric;

import com.ogatamizuki.elytraslot.*;
import com.ogatamizuki.elytraslot.network.ActionPayload;
import com.ogatamizuki.elytraslot.network.SlotPosSyncPayload;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ElytraSlotModFabric implements ModInitializer {
    private static final Map<UUID, ItemStack> ELYTRA_DATA = new ConcurrentHashMap<>();
    private static final Map<UUID, ItemStack> FIREWORK_DATA = new ConcurrentHashMap<>();
    private static final Map<UUID, SlotPositions> POSITIONS_DATA = new ConcurrentHashMap<>();

    public static ItemStack getElytra(Player player) {
        return ELYTRA_DATA.getOrDefault(player.getUUID(), ItemStack.EMPTY);
    }

    public static void setElytra(Player player, ItemStack stack) {
        if (stack.isEmpty()) {
            ELYTRA_DATA.remove(player.getUUID());
        } else {
            ELYTRA_DATA.put(player.getUUID(), stack.copy());
        }
    }

    public static ItemStack getFirework(Player player) {
        return FIREWORK_DATA.getOrDefault(player.getUUID(), ItemStack.EMPTY);
    }

    public static void setFirework(Player player, ItemStack stack) {
        if (stack.isEmpty()) {
            FIREWORK_DATA.remove(player.getUUID());
        } else {
            FIREWORK_DATA.put(player.getUUID(), stack.copy());
        }
    }

    public static SlotPositions getPositions(Player player) {
        return POSITIONS_DATA.getOrDefault(player.getUUID(), SlotPositions.DEFAULT);
    }

    public static void setPositions(Player player, SlotPositions pos) {
        POSITIONS_DATA.put(player.getUUID(), pos);
    }

    @Override
    public void onInitialize() {
        ElytraSlotCommon.LOGGER.info("Elytra Slot Mod (Fabric) Initializing...");

        ElytraSlotCommon.getElytraItem = ElytraSlotModFabric::getElytra;
        ElytraSlotCommon.setElytraItem = ElytraSlotModFabric::setElytra;
        ElytraSlotCommon.getFireworkItem = ElytraSlotModFabric::getFirework;
        ElytraSlotCommon.setFireworkItem = ElytraSlotModFabric::setFirework;
        ElytraSlotCommon.getSlotPositions = ElytraSlotModFabric::getPositions;
        ElytraSlotCommon.setSlotPositions = ElytraSlotModFabric::setPositions;

        ElytraSlotCommon.sendToPlayer = (player, payload) -> ServerPlayNetworking.send(player, payload);
        ElytraSlotCommon.sendToTracking = (player, payload) -> {
            for (ServerPlayer tracking : PlayerLookup.tracking(player)) {
                if (tracking.getUUID().equals(player.getUUID())) {
                    continue;
                }
                ServerPlayNetworking.send(tracking, payload);
            }
        };
        ElytraSlotCommon.sendToTrackingAndSelf = (player, payload) -> {
            ServerPlayNetworking.send(player, payload);
            ElytraSlotCommon.sendToTracking.accept(player, payload);
        };

        // Elytra flight provider for Fabric
        net.fabricmc.fabric.api.entity.event.v1.EntityElytraEvents.CUSTOM.register((entity, tickElytra) -> {
            if (entity instanceof Player player) {
                if (player.isShiftKeyDown()) {
                    return false;
                }
                ItemStack elytra = ElytraSlotCommon.getElytra(player);
                if (elytra.is(Items.ELYTRA) && elytra.getDamageValue() < elytra.getMaxDamage() - 1) {
                    if (tickElytra) {
                        int nextRoll = player.getFallFlyingTicks() + 1;
                        if (nextRoll % 10 == 0) {
                            if ((nextRoll / 10) % 2 == 0) {
                                elytra.hurtAndBreak(1, player, EquipmentSlot.CHEST);
                                if (player instanceof ServerPlayer sp) {
                                    ElytraSlotCommon.syncSlot(sp, elytra);
                                }
                            }
                            player.gameEvent(net.minecraft.world.level.gameevent.GameEvent.ELYTRA_GLIDE);
                        }
                    }
                    return true;
                }
            }
            return false;
        });

        // Register Payloads
        PayloadTypeRegistry.clientboundPlay().register(ElytraSlotSyncPayload.TYPE, ElytraSlotSyncPayload.STREAM_CODEC);
        PayloadTypeRegistry.clientboundPlay().register(FireworkSlotSyncPayload.TYPE, FireworkSlotSyncPayload.STREAM_CODEC);
        PayloadTypeRegistry.clientboundPlay().register(SlotPosSyncPayload.TYPE, SlotPosSyncPayload.STREAM_CODEC);
        PayloadTypeRegistry.serverboundPlay().register(ActionPayload.TYPE, ActionPayload.STREAM_CODEC);
        PayloadTypeRegistry.serverboundPlay().register(SlotPosSyncPayload.TYPE, SlotPosSyncPayload.STREAM_CODEC);

        // Server Packet Receivers
        ServerPlayNetworking.registerGlobalReceiver(ActionPayload.TYPE, (payload, context) -> {
            context.server().execute(() -> {
                ServerPlayer player = context.player();
                handleServerAction(player, payload.actionId());
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(SlotPosSyncPayload.TYPE, (payload, context) -> {
            context.server().execute(() -> {
                ServerPlayer player = context.player();
                SlotPositions current = ElytraSlotCommon.getPositions(player);
                SlotPositions updated = new SlotPositions(
                        payload.elytraX(), payload.elytraY(),
                        payload.fireworkX(), payload.fireworkY(),
                        current.creativeElytraX(), current.creativeElytraY(),
                        current.creativeFireworkX(), current.creativeFireworkY()
                );
                ElytraSlotCommon.setPositions(player, updated);
                ElytraSlotCommon.updatePlayerContainerSlotPositions(player, payload.elytraX(), payload.elytraY(), payload.fireworkX(), payload.fireworkY());
            });
        });

        // Lifecycle / Sync Events
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayer player = handler.getPlayer();
            syncPlayerState(player);
        });

        ServerPlayerEvents.COPY_FROM.register((oldPlayer, newPlayer, alive) -> {
            ItemStack elytra = ELYTRA_DATA.getOrDefault(oldPlayer.getUUID(), ItemStack.EMPTY);
            ItemStack firework = FIREWORK_DATA.getOrDefault(oldPlayer.getUUID(), ItemStack.EMPTY);
            SlotPositions pos = POSITIONS_DATA.getOrDefault(oldPlayer.getUUID(), SlotPositions.DEFAULT);

            boolean keepInv = newPlayer.level().getServer() != null && newPlayer.level().getServer().getGameRules().get(net.minecraft.world.level.gamerules.GameRules.KEEP_INVENTORY);
            if (alive || keepInv) {
                ELYTRA_DATA.put(newPlayer.getUUID(), elytra);
                FIREWORK_DATA.put(newPlayer.getUUID(), firework);
            }
            POSITIONS_DATA.put(newPlayer.getUUID(), pos);
        });
    }

    private static void syncPlayerState(ServerPlayer player) {
        ItemStack elytra = ElytraSlotCommon.getElytra(player);
        if (!elytra.isEmpty()) {
            ElytraSlotCommon.syncSlot(player, elytra);
        }
        ItemStack firework = ElytraSlotCommon.getFirework(player);
        if (!firework.isEmpty()) {
            ElytraSlotCommon.syncFireworkSlot(player, firework);
        }
        SlotPositions pos = ElytraSlotCommon.getPositions(player);
        ElytraSlotCommon.syncPositions(player, pos);
    }

    private static void handleServerAction(ServerPlayer player, int actionId) {
        if (actionId == ActionPayload.ACTION_SWAP_ELYTRA) {
            ElytraSlotCommon.performQuickSwapElytra(player);
        } else if (actionId == ActionPayload.ACTION_FIREWORK_BOOST) {
            ElytraSlotCommon.performFireworkBoost(player);
        }
    }
}
