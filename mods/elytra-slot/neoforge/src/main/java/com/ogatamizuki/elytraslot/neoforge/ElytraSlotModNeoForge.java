package com.ogatamizuki.elytraslot.neoforge;

import com.ogatamizuki.elytraslot.*;
import com.ogatamizuki.elytraslot.network.ActionPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

@Mod(ElytraSlotCommon.MODID)
public class ElytraSlotModNeoForge {
    // Attachment type registration
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, ElytraSlotCommon.MODID);

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<ItemStack>> ELYTRA_SLOT =
            ATTACHMENT_TYPES.register("elytra_item", () -> AttachmentType.builder(() -> ItemStack.EMPTY)
                    .serialize(ItemStack.OPTIONAL_CODEC.fieldOf("item"))
                    .build());

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<ItemStack>> FIREWORK_SLOT =
            ATTACHMENT_TYPES.register("firework_item", () -> AttachmentType.builder(() -> ItemStack.EMPTY)
                    .serialize(ItemStack.OPTIONAL_CODEC.fieldOf("item"))
                    .build());

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<SlotPositions>> SLOT_POSITIONS =
            ATTACHMENT_TYPES.register("slot_positions", () -> AttachmentType.builder(() -> SlotPositions.DEFAULT)
                    .serialize(SlotPositions.CODEC.fieldOf("positions"))
                    .build());

    public ElytraSlotModNeoForge(IEventBus modEventBus, net.neoforged.fml.ModContainer modContainer) {
        ElytraSlotCommon.LOGGER.info("Elytra Slot Mod (NeoForge) Initializing...");

        ElytraSlotCommon.getElytraItem = player -> player.getData(ELYTRA_SLOT);
        ElytraSlotCommon.setElytraItem = (player, stack) -> player.setData(ELYTRA_SLOT, stack);
        ElytraSlotCommon.getFireworkItem = player -> player.getData(FIREWORK_SLOT);
        ElytraSlotCommon.setFireworkItem = (player, stack) -> player.setData(FIREWORK_SLOT, stack);
        ElytraSlotCommon.getSlotPositions = player -> player.getData(SLOT_POSITIONS);
        ElytraSlotCommon.setSlotPositions = (player, pos) -> player.setData(SLOT_POSITIONS, pos);
        ElytraSlotCommon.sendToPlayer = PacketDistributor::sendToPlayer;
        ElytraSlotCommon.sendToTracking = (player, payload) ->
                PacketDistributor.sendToPlayersTrackingEntity(player, payload);
        ElytraSlotCommon.sendToTrackingAndSelf = PacketDistributor::sendToPlayersTrackingEntityAndSelf;

        ElytraSlotNeoForgeConfig.init();
        modContainer.registerConfig(net.neoforged.fml.config.ModConfig.Type.CLIENT, ElytraSlotNeoForgeConfig.SPEC, "elytra_slot-client.toml");

        ATTACHMENT_TYPES.register(modEventBus);
        modEventBus.addListener(this::registerPayloads);

        NeoForge.EVENT_BUS.register(this);

        if (net.neoforged.fml.loading.FMLEnvironment.getDist() == net.neoforged.api.distmarker.Dist.CLIENT) {
            com.ogatamizuki.elytraslot.neoforge.client.ElytraSlotModNeoForgeClient.init(modContainer);
        }
    }

    private void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");

        // Register Sync Payload for Elytra
        registrar.playToClient(
                ElytraSlotSyncPayload.TYPE,
                ElytraSlotSyncPayload.STREAM_CODEC,
                (payload, context) -> {
                    context.enqueueWork(() -> {
                        net.minecraft.world.entity.player.Player targetPlayer = context.player().level()
                                .getPlayerByUUID(payload.playerUuid());
                        if (targetPlayer != null) {
                            targetPlayer.setData(ELYTRA_SLOT, payload.elytraItem());
                        }
                    });
                });

        // Register Sync Payload for Firework
        registrar.playToClient(
                FireworkSlotSyncPayload.TYPE,
                FireworkSlotSyncPayload.STREAM_CODEC,
                (payload, context) -> {
                    context.enqueueWork(() -> {
                        net.minecraft.world.entity.player.Player targetPlayer = context.player().level()
                                .getPlayerByUUID(payload.playerUuid());
                        if (targetPlayer != null) {
                            targetPlayer.setData(FIREWORK_SLOT, payload.fireworkItem());
                        }
                    });
                });

        // Register Action Payload (Client to Server)
        registrar.playToServer(
                com.ogatamizuki.elytraslot.network.ActionPayload.TYPE,
                com.ogatamizuki.elytraslot.network.ActionPayload.STREAM_CODEC,
                (payload, context) -> {
                    context.enqueueWork(() -> {
                        net.minecraft.world.entity.player.Player player = context.player();
                        if (player instanceof ServerPlayer serverPlayer) {
                            handleServerAction(serverPlayer, payload.actionId());
                        }
                    });
                });

        // Register Slot Position Sync Payload (Bidirectional)
        registrar.playBidirectional(
                com.ogatamizuki.elytraslot.network.SlotPosSyncPayload.TYPE,
                com.ogatamizuki.elytraslot.network.SlotPosSyncPayload.STREAM_CODEC,
                (payload, context) -> {
                    context.enqueueWork(() -> {
                        // Client-side payload handler (no-op or local state sync)
                    });
                },
                (payload, context) -> {
                    context.enqueueWork(() -> {
                        net.minecraft.world.entity.player.Player player = context.player();
                        if (player instanceof ServerPlayer serverPlayer) {
                            SlotPositions current = serverPlayer.getData(SLOT_POSITIONS);
                            serverPlayer.setData(SLOT_POSITIONS, new SlotPositions(
                                     payload.elytraX(), payload.elytraY(),
                                    payload.fireworkX(), payload.fireworkY(),
                                    current.creativeElytraX(), current.creativeElytraY(),
                                    current.creativeFireworkX(), current.creativeFireworkY()
                            ));
                            updatePlayerContainerSlotPositions(serverPlayer, payload.elytraX(), payload.elytraY(), payload.fireworkX(), payload.fireworkY());
                        }
                    });
                });
    }

    public static void updatePlayerContainerSlotPositions(net.minecraft.world.entity.player.Player player, int ex, int ey, int fx, int fy) {
        try {
            java.lang.reflect.Field xField = net.minecraft.world.inventory.Slot.class.getDeclaredField("x");
            java.lang.reflect.Field yField = net.minecraft.world.inventory.Slot.class.getDeclaredField("y");
            xField.setAccessible(true);
            yField.setAccessible(true);

            for (net.minecraft.world.inventory.Slot slot : player.containerMenu.slots) {
                if (slot instanceof ElytraSlot) {
                    xField.setInt(slot, ex);
                    yField.setInt(slot, ey);
                } else if (slot instanceof FireworkSlot) {
                    xField.setInt(slot, fx);
                    yField.setInt(slot, fy);
                }
            }
        } catch (Exception e) {
            ElytraSlotCommon.LOGGER.error("Failed to update slot positions via reflection", e);
        }
    }

    private void handleServerAction(ServerPlayer player, int actionId) {
        if (actionId == ActionPayload.ACTION_SWAP_ELYTRA) {
            ElytraSlotCommon.performQuickSwapElytra(player);
        } else if (actionId == ActionPayload.ACTION_FIREWORK_BOOST) {
            ElytraSlotCommon.performFireworkBoost(player);
        }
    }

    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ItemStack elytra = player.getData(ELYTRA_SLOT);
            if (!elytra.isEmpty()) {
                ElytraSlotCommon.syncSlot(player, elytra);
            }
            ItemStack firework = player.getData(FIREWORK_SLOT);
            if (!firework.isEmpty()) {
                ElytraSlotCommon.syncFireworkSlot(player, firework);
            }
            SlotPositions positions = player.getData(SLOT_POSITIONS);
            ElytraSlotCommon.syncPositions(player, positions);
        }
    }

    @SubscribeEvent
    public void onStartTracking(PlayerEvent.StartTracking event) {
        if (event.getTarget() instanceof ServerPlayer targetPlayer && event.getEntity() instanceof ServerPlayer trackingPlayer) {
            ItemStack elytra = targetPlayer.getData(ELYTRA_SLOT);
            if (!elytra.isEmpty()) {
                PacketDistributor.sendToPlayer(trackingPlayer, new ElytraSlotSyncPayload(targetPlayer.getUUID(), elytra));
            }
            ItemStack firework = targetPlayer.getData(FIREWORK_SLOT);
            if (!firework.isEmpty()) {
                PacketDistributor.sendToPlayer(trackingPlayer, new FireworkSlotSyncPayload(targetPlayer.getUUID(), firework));
            }
        }
    }

    @SubscribeEvent
    public void onPlayerClone(PlayerEvent.Clone event) {
        boolean keepInv = event.getEntity().level().getServer() != null && event.getEntity().level().getServer().getGameRules().get(net.minecraft.world.level.gamerules.GameRules.KEEP_INVENTORY);
        if (!event.isWasDeath() || keepInv) {
            // Only keep if keepInventory is on or cloned not from death
            if (!event.isWasDeath()) {
                event.getEntity().setData(ELYTRA_SLOT, event.getOriginal().getData(ELYTRA_SLOT));
                event.getEntity().setData(FIREWORK_SLOT, event.getOriginal().getData(FIREWORK_SLOT));
                event.getEntity().setData(SLOT_POSITIONS, event.getOriginal().getData(SLOT_POSITIONS));
            }
        } else {
            event.getEntity().setData(ELYTRA_SLOT, event.getOriginal().getData(ELYTRA_SLOT));
            event.getEntity().setData(FIREWORK_SLOT, event.getOriginal().getData(FIREWORK_SLOT));
            event.getEntity().setData(SLOT_POSITIONS, event.getOriginal().getData(SLOT_POSITIONS));
        }
    }
}
