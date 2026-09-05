package com.ogatamizuki.economy;

import com.ogatamizuki.economy.backend.EconomyBalanceSync;
import com.ogatamizuki.economy.backend.EconomyEtfPriceScheduler;
import com.ogatamizuki.economy.data.EconomyPersist;
import com.ogatamizuki.economy.master.EconomyMasterData;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class EconomyServerEvents {
    private EconomyServerEvents() {}

    public record EconomyEntityInteractContext(
            Player player,
            Entity target,
            ItemStack heldStack,
            Level level,
            InteractResultSetter resultSetter
    ) {
        public boolean isClientSide() {
            return level.isClientSide();
        }

        public interface InteractResultSetter {
            void setCanceled(InteractionResult result);
        }
    }

    private record EconomyNpcInfo(int shopId, String npcType) {}

    public static void onServerStarting(MinecraftServer server) {
        EconomyCommon.setServer(server);
        EconomyMasterData.reload(server);
        EconomyEtfPriceScheduler.start(server);
        EconomyCommon.LOGGER.info("Economy LOCAL server starting (master data loaded)");
    }

    public static void onServerStopping(MinecraftServer server) {
        EconomyEtfPriceScheduler.stop();
        EconomyPersist.saveAll(server);
        EconomyCommon.setServer(null);
    }

    public static void onServerTick(MinecraftServer server) {
        EconomyPersist.onServerTick(server);
    }

    public static void onPlayerLoggedIn(Player player) {
        String uuid = player.getUUID().toString();
        String username = EconomyNicknameBridge.resolvePlayerName(player);
        UUID playerUuid = player.getUUID();
        EconomyCommon.LOGGER.info("Player logged in: {} ({}) - resetting economy ready status and triggering join sync", username, uuid);
        EconomyCommon.setEconomyReady(playerUuid, false);
        if (player instanceof ServerPlayer serverPlayer) {
            EconomyFeatures.syncToPlayer(serverPlayer);
            EconomyService.joinPlayer(uuid, username, serverPlayer);
        }
    }

    public static void onPlayerLoggedOut(Player player) {
        String uuid = player.getUUID().toString();
        String username = EconomyNicknameBridge.resolvePlayerName(player);
        UUID playerUuid = player.getUUID();
        EconomyCommon.LOGGER.info("Player logged out: {} ({}) - clearing economy ready status and triggering leave", username, uuid);
        RewardChatAggregator.flushPlayer(playerUuid);
        EconomyCommon.setEconomyReady(playerUuid, false);
        EconomyService.leavePlayer(uuid, username);
    }

    public static void onLivingDamage(Entity entity, float originalDamage, DamageSource source) {
        if (entity.level().isClientSide()) return;

        UUID mobUuid = entity.getUUID();
        float amount = originalDamage;

        UUID attackerUuid;
        String attackerName;

        if (source.getEntity() instanceof Player player) {
            attackerUuid = player.getUUID();
            attackerName = player.getName().getString();
        } else {
            attackerUuid = EconomyCommon.ENVIRONMENT_UUID;
            attackerName = "ENVIRONMENT (" + source.type().msgId() + ")";
        }

        Map<UUID, Map<UUID, Float>> damageTracker = EconomyCommon.damageTracker();
        damageTracker.compute(mobUuid, (k, playerDamageMap) -> {
            if (playerDamageMap == null) {
                playerDamageMap = new ConcurrentHashMap<>();
            }
            playerDamageMap.merge(attackerUuid, amount, Float::sum);
            return playerDamageMap;
        });

        EconomyCommon.LOGGER.info("Recorded damage from {} to mob {}: {} (Total for this source: {})",
                attackerName, entity.getType().toString(), amount, damageTracker.get(mobUuid).get(attackerUuid));
    }

    public static void onLivingDeath(Entity entity, DamageSource source, Level level) {
        if (level.isClientSide()) return;

        if (entity instanceof Player player) {
            if (!EconomyCommon.isEconomyReady(player.getUUID())) return;
            EconomyCommon.LOGGER.info("Player {} has died - triggering death penalty", player.getName().getString());
            if (!player.isCreative()) {
                EconomyService.deathPlayer(player);
            } else {
                EconomyCommon.LOGGER.info("Skipping death penalty for player {} (Creative Mode)", player.getName().getString());
            }
            return;
        }

        UUID mobUuid = entity.getUUID();
        Map<UUID, Float> playerDamageMap = EconomyCommon.damageTracker().remove(mobUuid);

        String entityName = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).getPath().toUpperCase();
        String actionType = "KILL_" + entityName;

        EconomyCommon.LOGGER.info("onLivingDeath event fired for entity: {} ({})", entity.getType().toString(), mobUuid);

        if (playerDamageMap != null && !playerDamageMap.isEmpty()) {
            float totalDamage = 0f;
            for (float d : playerDamageMap.values()) {
                totalDamage += d;
            }

            if (totalDamage > 0) {
                EconomyCommon.LOGGER.info("Distributing rewards for {} based on damage ratios. Total damage: {}", entityName, totalDamage);
                for (Map.Entry<UUID, Float> entry : playerDamageMap.entrySet()) {
                    UUID playerUuid = entry.getKey();
                    if (playerUuid.equals(EconomyCommon.ENVIRONMENT_UUID)) {
                        continue;
                    }
                    float damage = entry.getValue();
                    double ratio = damage / totalDamage;

                    Player player = level.getPlayerByUUID(playerUuid);
                    if (player != null) {
                        if (!EconomyCommon.isEconomyReady(playerUuid)) {
                            continue;
                        }
                        EconomyCommon.LOGGER.info("Player {} dealt {} damage (Ratio: {})", player.getName().getString(), damage, ratio);
                        if (player.isCreative()) {
                            EconomyCommon.LOGGER.info("Skipping reward for player {} (Creative Mode)", player.getName().getString());
                            continue;
                        }
                        EconomyService.rewardPlayer(player, actionType, ratio);
                    }
                }
                return;
            }
        }

        if (source.getEntity() instanceof Player player) {
            if (!EconomyCommon.isEconomyReady(player.getUUID())) return;
            EconomyCommon.LOGGER.info("Fallback reward trigger: Player {} landed the killing blow", player.getName().getString());
            if (player.isCreative()) {
                EconomyCommon.LOGGER.info("Skipping fallback reward (Creative Mode)");
                return;
            }
            EconomyService.rewardPlayer(player, actionType, 1.0);
        }
    }

    public static void onBlockBreak(Player player, BlockState state, net.minecraft.world.level.LevelAccessor level, net.minecraft.core.BlockPos pos) {
        if (level.isClientSide()) return;
        if (player == null || player.isCreative()) return;
        if (!EconomyCommon.isEconomyReady(player.getUUID())) return;

        Block block = state.getBlock();

        if (block instanceof CropBlock cropBlock) {
            if (cropBlock.isMaxAge(state)) {
                String cropName = BuiltInRegistries.BLOCK.getKey(block).getPath().toUpperCase();
                if (cropName.equals("BEETROOTS")) cropName = "BEETROOT";
                if (cropName.equals("POTATOES")) cropName = "POTATO";
                if (cropName.equals("CARROTS")) cropName = "CARROT";
                String actionType = "HARVEST_" + cropName;
                EconomyService.rewardPlayer(player, actionType);
            }
        } else {
            String blockName = BuiltInRegistries.BLOCK.getKey(block).getPath().toUpperCase();
            if (blockName.startsWith("DEEPSLATE_")) {
                blockName = blockName.substring("DEEPSLATE_".length());
            }
            if (blockName.equals("MELON") || blockName.equals("PUMPKIN")) {
                MinecraftServer mcServer = EconomyCommon.getServer();
                boolean isPlayerPlaced = false;
                if (mcServer != null && level instanceof ServerLevel serverLevel) {
                    PlacedBlocksSavedData data = PlacedBlocksSavedData.get(mcServer);
                    if (data.isPlaced(serverLevel.dimension(), pos)) {
                        isPlayerPlaced = true;
                        data.removePlaced(serverLevel.dimension(), pos);
                    }
                }
                if (!isPlayerPlaced) {
                    String actionType = "HARVEST_" + blockName;
                    EconomyService.rewardPlayer(player, actionType);
                }
            } else if (blockName.endsWith("_ORE")) {
                String actionType = "MINE_" + blockName;
                EconomyService.rewardPlayer(player, actionType);
            }
        }
    }

    public static void onBlockPlace(Player player, BlockState placedBlock, net.minecraft.world.level.LevelAccessor level, net.minecraft.core.BlockPos pos) {
        if (level.isClientSide()) return;
        if (player == null || player.isCreative()) return;

        Block block = placedBlock.getBlock();
        String blockName = BuiltInRegistries.BLOCK.getKey(block).getPath().toUpperCase();

        if (blockName.equals("MELON") || blockName.equals("PUMPKIN")) {
            MinecraftServer mcServer = EconomyCommon.getServer();
            if (mcServer != null && level instanceof ServerLevel serverLevel) {
                PlacedBlocksSavedData.get(mcServer).addPlaced(serverLevel.dimension(), pos);
            }
        }
    }

    public static void onItemFished(Player player, java.util.List<ItemStack> drops) {
        if (player.level().isClientSide()) return;
        if (player == null || player.isCreative()) return;
        if (!EconomyCommon.isEconomyReady(player.getUUID())) return;

        boolean isRare = true;
        for (ItemStack drop : drops) {
            if (drop.getItem() == Items.COD || drop.getItem() == Items.SALMON) {
                isRare = false;
            }
        }

        String actionType = isRare ? "FISH_RARE" : "FISH_COMMON";
        EconomyService.rewardPlayer(player, actionType);
    }

    public static void onEntityJoinLevel(Entity entity, Level level, boolean loadedFromDisk) {
        if (level.isClientSide()) {
            return;
        }

        parseEconomyNpcInfo(entity, true).ifPresent(info ->
                EconomyNpcSpawnService.applyLocalizedDisplayName(entity, info.shopId()));

        if (loadedFromDisk || !isEconomyRelatedNpc(entity)) {
            return;
        }

        Player spawner = findNearestSpawnEggPlayer(level, entity);
        if (spawner != null) {
            facePlayer(entity, spawner);
        }
    }

    public static void onEntityInteract(EconomyEntityInteractContext context) {
        Entity target = context.target();
        ItemStack heldStack = context.heldStack();

        if (!heldStack.isEmpty() && tryRemoveNpcWithSpawnEgg(context, target, heldStack)) {
            return;
        }

        Optional<EconomyNpcInfo> npcInfo = parseEconomyNpcInfo(target, !context.isClientSide());
        if (npcInfo.isEmpty()) {
            return;
        }

        int shopId = npcInfo.get().shopId();
        String npcType = npcInfo.get().npcType();

        context.resultSetter().setCanceled(InteractionResult.SUCCESS);

        if (!context.isClientSide() && context.player() instanceof ServerPlayer serverPlayer) {
            EconomyPlatform.send(serverPlayer, new OpenShopScreenPayload(shopId, npcType));
            EconomyCommon.LOGGER.info("Intercepted interact for shop NPC (Success): shopId={}, npcType={}, target={}", shopId, npcType, target.getType().toString());
        }
    }

    private static Optional<EconomyNpcInfo> parseEconomyNpcInfo(Entity target, boolean allowPersistentData) {
        if (target instanceof EconomyNpc economyNpc) {
            return Optional.of(new EconomyNpcInfo(economyNpc.getShopId(), economyNpc.getNpcType()));
        }

        for (String tag : target.entityTags()) {
            if (tag.startsWith("EconomyNPC:")) {
                String[] parts = tag.split(":");
                if (parts.length >= 3) {
                    try {
                        return Optional.of(new EconomyNpcInfo(Integer.parseInt(parts[1]), parts[2]));
                    } catch (NumberFormatException e) {
                        EconomyCommon.LOGGER.error("Failed to parse shopId from tag: {}", tag, e);
                    }
                }
            }
        }

        if (target.hasCustomName()) {
            Component customName = target.getCustomName();
            if (customName != null) {
                String nameStr = customName.getString();
                int index = nameStr.indexOf("EconomyNPC:");
                if (index != -1) {
                    String sub = nameStr.substring(index);
                    sub = sub.replace("\"", "").replace("}", "").replace("'", "").replace("\\", "");
                    String[] parts = sub.split(":");
                    if (parts.length >= 3) {
                        try {
                            return Optional.of(new EconomyNpcInfo(Integer.parseInt(parts[1]), parts[2]));
                        } catch (NumberFormatException e) {
                            EconomyCommon.LOGGER.error("Failed to parse shopId from custom name: {}", nameStr, e);
                        }
                    }
                }
            }
        }

        if (allowPersistentData) {
            CompoundTag persistentData = EconomyPlatform.getEntityPersistentData.apply(target);
            if (persistentData.contains("shop_id")) {
                return Optional.of(new EconomyNpcInfo(
                        persistentData.getInt("shop_id").orElse(1),
                        persistentData.getString("npc_type").orElse("SELLER")));
            }
        }

        return Optional.empty();
    }

    private static Optional<Integer> parseShopIdFromEggName(ItemStack stack) {
        Component customName = stack.get(DataComponents.CUSTOM_NAME);
        if (customName == null) {
            return Optional.empty();
        }
        String name = customName.getString();
        int idIndex = name.indexOf("[ID: ");
        if (idIndex == -1) {
            return Optional.empty();
        }
        int endIndex = name.indexOf(']', idIndex);
        if (endIndex == -1) {
            return Optional.empty();
        }
        try {
            return Optional.of(Integer.parseInt(name.substring(idIndex + 5, endIndex).trim()));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    private static boolean isEconomyNpcSpawnEgg(ItemStack stack) {
        return stack.is(EconomyRegistries.ECONOMY_NPC_SPAWN_EGG) || parseShopIdFromEggName(stack).isPresent();
    }

    private static boolean matchesEconomyNpcEgg(ItemStack stack, EconomyNpcInfo npcInfo) {
        if (stack.is(EconomyRegistries.ECONOMY_NPC_SPAWN_EGG)) {
            return true;
        }
        return parseShopIdFromEggName(stack)
                .map(shopId -> shopId == npcInfo.shopId())
                .orElse(false);
    }

    private static boolean canRemoveNpc(Player player) {
        if (player.isCreative()) {
            return true;
        }
        if (player instanceof ServerPlayer serverPlayer) {
            return serverPlayer.createCommandSourceStack().permissions().hasPermission(net.minecraft.server.permissions.Permissions.COMMANDS_GAMEMASTER);
        }
        return false;
    }

    private static boolean tryRemoveNpcWithSpawnEgg(
            EconomyEntityInteractContext context,
            Entity target,
            ItemStack eggStack) {
        Player player = context.player();
        if (!canRemoveNpc(player)) {
            return false;
        }
        if (!(eggStack.getItem() instanceof SpawnEggItem)) {
            return false;
        }

        if (target.getType() == EconomyRegistries.LOAN_NPC && eggStack.is(EconomyRegistries.LOAN_NPC_SPAWN_EGG)) {
            return executeNpcRemoval(context, target, eggStack, player, "融資NPC");
        }

        boolean allowPersistentData = !context.isClientSide();
        Optional<EconomyNpcInfo> npcInfo = parseEconomyNpcInfo(target, allowPersistentData);
        if (npcInfo.isPresent() && isEconomyNpcSpawnEgg(eggStack) && matchesEconomyNpcEgg(eggStack, npcInfo.get())) {
            return executeNpcRemoval(context, target, eggStack, player,
                    "経済NPC (ID: " + npcInfo.get().shopId() + ", タイプ: " + npcInfo.get().npcType() + ")");
        }

        return false;
    }

    private static boolean executeNpcRemoval(
            EconomyEntityInteractContext context,
            Entity target,
            ItemStack eggStack,
            Player player,
            String label) {
        context.resultSetter().setCanceled(InteractionResult.SUCCESS);

        if (!context.isClientSide()) {
            target.discard();
            if (!player.isCreative()) {
                eggStack.shrink(1);
            }
            if (player instanceof ServerPlayer serverPlayer) {
                serverPlayer.sendSystemMessage(Component.literal("§a" + label + " を撤去しました。"));
            }
            EconomyCommon.LOGGER.info("Removed {} via spawn egg by {}", target.getType(), player.getName().getString());
        }
        return true;
    }

    private static boolean isEconomyRelatedNpc(Entity entity) {
        if (entity.getType() == EconomyRegistries.ECONOMY_NPC || entity.getType() == EconomyRegistries.LOAN_NPC) {
            return true;
        }
        return parseEconomyNpcInfo(entity, true).isPresent();
    }

    private static boolean isHoldingSpawnEgg(Player player) {
        return player.getMainHandItem().getItem() instanceof SpawnEggItem
                || player.getOffhandItem().getItem() instanceof SpawnEggItem;
    }

    private static Player findNearestSpawnEggPlayer(Level level, Entity entity) {
        Player nearest = null;
        double nearestDistSq = 64.0;
        for (Player player : level.players()) {
            if (!isHoldingSpawnEgg(player)) {
                continue;
            }
            double distSq = player.distanceToSqr(entity);
            if (distSq < nearestDistSq) {
                nearestDistSq = distSq;
                nearest = player;
            }
        }
        return nearest;
    }

    private static void facePlayer(Entity entity, Player player) {
        double dx = player.getX() - entity.getX();
        double dz = player.getZ() - entity.getZ();
        float yRot = (float) (Mth.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0F;
        entity.setYRot(yRot);
        if (entity instanceof LivingEntity living) {
            living.yHeadRot = yRot;
            living.yBodyRot = yRot;
        }
    }
}
