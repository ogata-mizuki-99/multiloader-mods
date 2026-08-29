package com.ogatamizuki.elytraslot;

import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.minecraft.server.level.ServerPlayer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(ElytraSlotMod.MODID)
public class ElytraSlotMod {
    public static final String MODID = "elytra_slot";
    public static final Logger LOGGER = LogManager.getLogger(ElytraSlotMod.class);

    /** Identifier used as the ID of the GLIDING_FLIGHT attribute modifier added when elytra is in the dedicated slot. */
    public static final Identifier ELYTRA_GLIDE_MODIFIER_ID =
            Identifier.fromNamespaceAndPath(MODID, "custom_elytra_glide");

    // Attachment type registration
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, MODID);

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

    public ElytraSlotMod(IEventBus modEventBus, net.neoforged.fml.ModContainer modContainer) {
        LOGGER.info("Elytra Slot Mod Initializing...");

        modContainer.registerConfig(net.neoforged.fml.config.ModConfig.Type.CLIENT, Config.SPEC, "elytra_slot-client.toml");

        ATTACHMENT_TYPES.register(modEventBus);
        modEventBus.addListener(this::registerPayloads);

        NeoForge.EVENT_BUS.register(this);

        if (net.neoforged.fml.loading.FMLEnvironment.getDist() == net.neoforged.api.distmarker.Dist.CLIENT) {
            com.ogatamizuki.elytraslot.client.KeyMappings.init();
            com.ogatamizuki.elytraslot.client.ElytraSlotModClient.init(modContainer);
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

        // Register Slot Position Sync Payload (Client to Server)
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
            LOGGER.error("Failed to update slot coordinates dynamically", e);
        }
    }

    public static boolean hasCustomSlots(net.minecraft.world.inventory.AbstractContainerMenu menu) {
        for (net.minecraft.world.inventory.Slot slot : menu.slots) {
            if (slot instanceof ElytraSlot || slot instanceof FireworkSlot) {
                return true;
            }
        }
        return false;
    }

    /** Survival/container coordinates. Never calls isCreative() (unsafe during player construction). */
    public static int[] resolveSlotPositions(net.minecraft.world.entity.player.Player player) {
        return resolveSurvivalSlotPositions(player);
    }

    public static int[] resolveSurvivalSlotPositions(net.minecraft.world.entity.player.Player player) {
        if (player.level().isClientSide()) {
            return new int[]{
                    Config.ELYTRA_SLOT_X.get(),
                    Config.ELYTRA_SLOT_Y.get(),
                    Config.FIREWORK_SLOT_X.get(),
                    Config.FIREWORK_SLOT_Y.get()
            };
        }
        return player.getData(SLOT_POSITIONS).forSurvival();
    }

    public static int[] resolveCreativeSlotPositions(net.minecraft.world.entity.player.Player player) {
        if (player.level().isClientSide()) {
            return new int[]{
                    Config.CREATIVE_ELYTRA_SLOT_X.get(),
                    Config.CREATIVE_ELYTRA_SLOT_Y.get(),
                    Config.CREATIVE_FIREWORK_SLOT_X.get(),
                    Config.CREATIVE_FIREWORK_SLOT_Y.get()
            };
        }
        return player.getData(SLOT_POSITIONS).forCreative();
    }

    public static void addCustomSlotsIfAbsent(net.minecraft.world.inventory.InventoryMenu menu, net.minecraft.world.entity.player.Player owner) {
        CustomInventorySlots.addCustomSlotsIfAbsent(menu, owner);
    }

    private void handleServerAction(ServerPlayer player, int actionId) {
        if (actionId == 1) {
            // Quick Swap: toggle dedicated slot ↔ inventory/hotbar (no need to hold the elytra)
            ItemStack elytraInSlot = player.getData(ELYTRA_SLOT);
            if (!elytraInSlot.isEmpty()) {
                unequipElytraToInventory(player, elytraInSlot);
            } else {
                equipElytraFromInventory(player);
            }
        } else if (actionId == 2) {
            // Firework Use
            if (player.isFallFlying()) {
                ItemStack firework = player.getData(FIREWORK_SLOT);
                boolean consumed = false;

                if (firework.is(net.minecraft.world.item.Items.FIREWORK_ROCKET)) {
                    firework.shrink(1);
                    player.setData(FIREWORK_SLOT, firework);
                    syncFireworkSlot(player, firework);
                    consumed = true;
                } else {
                    // Try inventory
                    for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                        ItemStack invStack = player.getInventory().getItem(i);
                        if (invStack.is(net.minecraft.world.item.Items.FIREWORK_ROCKET)) {
                            invStack.shrink(1);
                            player.getInventory().setItem(i, invStack);
                            consumed = true;
                            break;
                        }
                    }
                }

                if (consumed) {
                    // Spawn firework rocket entity to accelerate player
                    net.minecraft.world.entity.projectile.FireworkRocketEntity entity =
                            new net.minecraft.world.entity.projectile.FireworkRocketEntity(
                                    player.level(),
                                    new ItemStack(net.minecraft.world.item.Items.FIREWORK_ROCKET),
                                    player
                            );
                    player.level().addFreshEntity(entity);
                }
            }
        }
    }

    // -------------------------------------------------------------------------
    // GLIDING_FLIGHT attribute management
    // -------------------------------------------------------------------------

    /**
     * Add the GLIDING_FLIGHT attribute modifier to the player so that
     * {@code LivingEntity#canGlide(isNeoForge=true)} returns {@code true}.
     * Must be called whenever an elytra is placed into the dedicated slot.
     */
    public static void applyGlidingAttribute(ServerPlayer player) {
        AttributeInstance attr = player.getAttribute(NeoForgeMod.GLIDING_FLIGHT);
        if (attr != null && attr.getModifier(ELYTRA_GLIDE_MODIFIER_ID) == null) {
            attr.addPermanentModifier(new AttributeModifier(
                    ELYTRA_GLIDE_MODIFIER_ID, 1.0, AttributeModifier.Operation.ADD_VALUE
            ));
        }
    }

    /**
     * Remove the GLIDING_FLIGHT attribute modifier from the player.
     * Must be called whenever the dedicated slot elytra is removed.
     */
    public static void removeGlidingAttribute(ServerPlayer player) {
        AttributeInstance attr = player.getAttribute(NeoForgeMod.GLIDING_FLIGHT);
        if (attr != null) {
            attr.removeModifier(ELYTRA_GLIDE_MODIFIER_ID);
        }
    }

    // -------------------------------------------------------------------------

    /** Move dedicated-slot elytra into inventory/hotbar; drop remainder if full. */
    private static void unequipElytraToInventory(ServerPlayer player, ItemStack elytraInSlot) {
        ItemStack toReturn = elytraInSlot.copy();
        player.setData(ELYTRA_SLOT, ItemStack.EMPTY);
        syncSlot(player, ItemStack.EMPTY);
        player.getInventory().placeItemBackInInventory(toReturn);
        removeGlidingAttribute(player);
    }

    /**
     * Equip the first elytra found in hotbar / main inventory / offhand
     * (armor chest is excluded — right-click hand swap covers that case).
     */
    private static void equipElytraFromInventory(ServerPlayer player) {
        var inventory = player.getInventory();
        for (int i = 0; i < 36; i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.is(net.minecraft.world.item.Items.ELYTRA)) {
                ItemStack found = stack.copy();
                inventory.setItem(i, ItemStack.EMPTY);
                player.setData(ELYTRA_SLOT, found);
                syncSlot(player, found);
                applyGlidingAttribute(player);
                return;
            }
        }
        ItemStack offhand = player.getOffhandItem();
        if (offhand.is(net.minecraft.world.item.Items.ELYTRA)) {
            ItemStack found = offhand.copy();
            player.setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND, ItemStack.EMPTY);
            player.setData(ELYTRA_SLOT, found);
            syncSlot(player, found);
            applyGlidingAttribute(player);
        }
    }

    // Handle right-click with elytra to auto-equip (hand ↔ dedicated slot only)
    @SubscribeEvent
    public void onRightClickItem(net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickItem event) {
        net.minecraft.world.entity.player.Player player = event.getEntity();
        ItemStack held = event.getItemStack();

        if (held.is(net.minecraft.world.item.Items.ELYTRA)) {
            event.setCanceled(true);
            event.setCancellationResult(net.minecraft.world.InteractionResult.SUCCESS);

            if (!player.level().isClientSide() && player instanceof ServerPlayer serverPlayer) {
                ItemStack currentInSlot = serverPlayer.getData(ELYTRA_SLOT);
                serverPlayer.setItemInHand(event.getHand(), currentInSlot.copy());
                serverPlayer.setData(ELYTRA_SLOT, held.copy());
                syncSlot(serverPlayer, held);
                // Elytra placed in dedicated slot — grant gliding flight attribute
                applyGlidingAttribute(serverPlayer);
            }
        }
    }

    // Handle player death drops
    @SubscribeEvent
    public void onLivingDrops(net.neoforged.neoforge.event.entity.living.LivingDropsEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            if (!player.level().getGameRules().get(net.minecraft.world.level.gamerules.GameRules.KEEP_INVENTORY)) {
                // Drop elytra
                ItemStack elytraStack = player.getData(ELYTRA_SLOT);
                if (!elytraStack.isEmpty()) {
                    player.drop(elytraStack.copy(), true, false);
                    player.setData(ELYTRA_SLOT, ItemStack.EMPTY);
                    syncSlot(player, ItemStack.EMPTY);
                    removeGlidingAttribute(player);
                }
                // Drop firework
                ItemStack fireworkStack = player.getData(FIREWORK_SLOT);
                if (!fireworkStack.isEmpty()) {
                    player.drop(fireworkStack.copy(), true, false);
                    player.setData(FIREWORK_SLOT, ItemStack.EMPTY);
                    syncFireworkSlot(player, ItemStack.EMPTY);
                }
            }
        }
    }

    // Keep elytra & firework slot content on dimensions change or keepInventory=true on death
    @SubscribeEvent
    public void onPlayerClone(PlayerEvent.Clone event) {
        if (event.getOriginal() instanceof ServerPlayer oldPlayer && event.getEntity() instanceof ServerPlayer newPlayer) {
            if (!event.isWasDeath() || oldPlayer.level().getGameRules().get(net.minecraft.world.level.gamerules.GameRules.KEEP_INVENTORY)) {
                ItemStack elytraStack = oldPlayer.getData(ELYTRA_SLOT);
                newPlayer.setData(ELYTRA_SLOT, elytraStack.copy());

                ItemStack fireworkStack = oldPlayer.getData(FIREWORK_SLOT);
                newPlayer.setData(FIREWORK_SLOT, fireworkStack.copy());

                // Re-apply gliding attribute if elytra is in the dedicated slot
                if (!elytraStack.isEmpty()) {
                    applyGlidingAttribute(newPlayer);
                }
            }
            // Always clone slot positions so player doesn't lose custom layout on death/dimension change
            SlotPositions pos = oldPlayer.getData(SLOT_POSITIONS);
            newPlayer.setData(SLOT_POSITIONS, pos);
        }
    }

    // Server-side synchronization when a player logs in
    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            syncAttachmentSlotsToPlayer(player);
            // Re-apply gliding attribute if elytra was already in the dedicated slot
            if (!player.getData(ELYTRA_SLOT).isEmpty()) {
                applyGlidingAttribute(player);
            }
        }
    }

    /** Push attachment state (including empty) so creative ghosts cannot linger after mode switches. */
    @SubscribeEvent
    public void onPlayerChangeGameMode(PlayerEvent.PlayerChangeGameModeEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            syncAttachmentSlotsToPlayer(player);
        }
    }

    /** Always sync both slots so clients clear ghosts when the server side is empty. */
    public static void syncAttachmentSlotsToPlayer(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, new ElytraSlotSyncPayload(player.getUUID(), player.getData(ELYTRA_SLOT)));
        PacketDistributor.sendToPlayer(player, new FireworkSlotSyncPayload(player.getUUID(), player.getData(FIREWORK_SLOT)));
    }

    // Sync when tracking starts (so other clients render the elytra)
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

    // Helper to sync to tracking players and self
    public static void syncSlot(ServerPlayer player, ItemStack stack) {
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(player, new ElytraSlotSyncPayload(player.getUUID(), stack));
    }

    public static void syncFireworkSlot(ServerPlayer player, ItemStack stack) {
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(player, new FireworkSlotSyncPayload(player.getUUID(), stack));
    }
}
