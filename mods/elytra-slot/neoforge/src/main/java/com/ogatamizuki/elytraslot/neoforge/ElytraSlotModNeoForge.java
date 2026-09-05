package com.ogatamizuki.elytraslot.neoforge;

import com.ogatamizuki.elytraslot.*;
import com.ogatamizuki.elytraslot.network.ActionPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

@Mod(ElytraSlotCommon.MODID)
public class ElytraSlotModNeoForge {
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
        ElytraSlotCommon.setElytraItem = (player, stack) -> {
            ItemStack previous = player.getData(ELYTRA_SLOT);
            player.setData(ELYTRA_SLOT, stack);
            if (player instanceof ServerPlayer serverPlayer && !player.level().isClientSide()) {
                ElytraSlotNeoForgeGliding.onElytraChanged(serverPlayer, previous, stack);
            }
        };
        ElytraSlotCommon.getFireworkItem = player -> player.getData(FIREWORK_SLOT);
        ElytraSlotCommon.setFireworkItem = (player, stack) -> player.setData(FIREWORK_SLOT, stack);
        ElytraSlotCommon.getSlotPositions = player -> player.getData(SLOT_POSITIONS);
        ElytraSlotCommon.setSlotPositions = (player, pos) -> player.setData(SLOT_POSITIONS, pos);
        ElytraSlotCommon.sendToPlayer = PacketDistributor::sendToPlayer;
        ElytraSlotCommon.sendToTracking = (player, payload) ->
                PacketDistributor.sendToPlayersTrackingEntity(player, payload);
        ElytraSlotCommon.sendToTrackingAndSelf = PacketDistributor::sendToPlayersTrackingEntityAndSelf;
        ElytraSlotCommon.onElytraEquipped = ElytraSlotNeoForgeGliding::apply;
        ElytraSlotCommon.onElytraUnequipped = ElytraSlotNeoForgeGliding::remove;

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

        registrar.playToServer(
                ActionPayload.TYPE,
                ActionPayload.STREAM_CODEC,
                (payload, context) -> {
                    context.enqueueWork(() -> {
                        net.minecraft.world.entity.player.Player player = context.player();
                        if (player instanceof ServerPlayer serverPlayer) {
                            handleServerAction(serverPlayer, payload.actionId());
                        }
                    });
                });

        registrar.playToServer(
                com.ogatamizuki.elytraslot.network.SlotPosSyncPayload.TYPE,
                com.ogatamizuki.elytraslot.network.SlotPosSyncPayload.STREAM_CODEC,
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
                            ElytraSlotCommon.updatePlayerContainerSlotPositions(
                                    serverPlayer, payload.elytraX(), payload.elytraY(), payload.fireworkX(), payload.fireworkY());
                        }
                    });
                });
    }

    private void handleServerAction(ServerPlayer player, int actionId) {
        if (actionId == ActionPayload.ACTION_SWAP_ELYTRA) {
            ElytraSlotCommon.performQuickSwapElytra(player);
        } else if (actionId == ActionPayload.ACTION_FIREWORK_BOOST) {
            ElytraSlotCommon.performFireworkBoost(player);
        }
    }

    public static void syncAttachmentSlotsToPlayer(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, new ElytraSlotSyncPayload(player.getUUID(), player.getData(ELYTRA_SLOT)));
        PacketDistributor.sendToPlayer(player, new FireworkSlotSyncPayload(player.getUUID(), player.getData(FIREWORK_SLOT)));
    }

    @SubscribeEvent
    public void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        net.minecraft.world.entity.player.Player player = event.getEntity();
        ItemStack held = event.getItemStack();

        if (!held.is(Items.ELYTRA)) {
            return;
        }

        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);

        if (!player.level().isClientSide() && player instanceof ServerPlayer serverPlayer) {
            ItemStack currentInSlot = serverPlayer.getData(ELYTRA_SLOT);
            serverPlayer.setItemInHand(event.getHand(), currentInSlot.copy());
            ElytraSlotCommon.setElytra(serverPlayer, held.copy());
            ElytraSlotCommon.syncSlot(serverPlayer, held);
        }
    }

    @SubscribeEvent
    public void onLivingDrops(LivingDropsEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (player.level().getGameRules().get(net.minecraft.world.level.gamerules.GameRules.KEEP_INVENTORY)) {
            return;
        }

        ItemStack elytraStack = player.getData(ELYTRA_SLOT);
        if (!elytraStack.isEmpty()) {
            player.drop(elytraStack.copy(), true, false);
            ElytraSlotCommon.setElytra(player, ItemStack.EMPTY);
            ElytraSlotCommon.syncSlot(player, ItemStack.EMPTY);
        }

        ItemStack fireworkStack = player.getData(FIREWORK_SLOT);
        if (!fireworkStack.isEmpty()) {
            player.drop(fireworkStack.copy(), true, false);
            ElytraSlotCommon.setFirework(player, ItemStack.EMPTY);
            ElytraSlotCommon.syncFireworkSlot(player, ItemStack.EMPTY);
        }
    }

    @SubscribeEvent
    public void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player && !player.level().isClientSide()) {
            ElytraSlotNeoForgeGliding.tickSneakCancel(player);
        }
    }

    @SubscribeEvent
    public void onPlayerClone(PlayerEvent.Clone event) {
        if (!(event.getOriginal() instanceof ServerPlayer oldPlayer) || !(event.getEntity() instanceof ServerPlayer newPlayer)) {
            return;
        }

        if (!event.isWasDeath() || oldPlayer.level().getGameRules().get(net.minecraft.world.level.gamerules.GameRules.KEEP_INVENTORY)) {
            ItemStack elytraStack = oldPlayer.getData(ELYTRA_SLOT);
            newPlayer.setData(ELYTRA_SLOT, elytraStack.copy());

            ItemStack fireworkStack = oldPlayer.getData(FIREWORK_SLOT);
            newPlayer.setData(FIREWORK_SLOT, fireworkStack.copy());

            if (!elytraStack.isEmpty()) {
                ElytraSlotNeoForgeGliding.apply(newPlayer);
            }
        }

        newPlayer.setData(SLOT_POSITIONS, oldPlayer.getData(SLOT_POSITIONS));
    }

    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            syncAttachmentSlotsToPlayer(player);
            if (!player.getData(ELYTRA_SLOT).isEmpty()) {
                ElytraSlotNeoForgeGliding.apply(player);
            }
        }
    }

    @SubscribeEvent
    public void onPlayerChangeGameMode(PlayerEvent.PlayerChangeGameModeEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            syncAttachmentSlotsToPlayer(player);
        }
    }

    @SubscribeEvent
    public void onStartTracking(PlayerEvent.StartTracking event) {
        if (event.getTarget() instanceof ServerPlayer targetPlayer && event.getEntity() instanceof ServerPlayer tracker) {
            ItemStack elytraStack = targetPlayer.getData(ELYTRA_SLOT);
            if (!elytraStack.isEmpty()) {
                PacketDistributor.sendToPlayer(tracker, new ElytraSlotSyncPayload(targetPlayer.getUUID(), elytraStack));
            }
            ItemStack fireworkStack = targetPlayer.getData(FIREWORK_SLOT);
            if (!fireworkStack.isEmpty()) {
                PacketDistributor.sendToPlayer(tracker, new FireworkSlotSyncPayload(targetPlayer.getUUID(), fireworkStack));
            }
        }
    }
}
