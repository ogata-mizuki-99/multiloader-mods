package com.ogatamizuki.economy.fabric;

import com.ogatamizuki.economy.*;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class EconomyModFabric implements ModInitializer {
    public static final Logger LOGGER = LogManager.getLogger(EconomyModFabric.class);
    private static MinecraftServer currentServer;

    public static MinecraftServer getServer() {
        return currentServer;
    }

    @Override
    public void onInitialize() {
        EconomyCommon.LOGGER.info("Economy Mod (Fabric) Initializing...");
        wirePlatform();
        FabricRegistryHelper.prepare();
        registerContent();
        registerPayloads();
        registerServerEvents();
        EconomyFabricConfig.load();
    }

    private static void wirePlatform() {
        EconomyPlatform.sendToPlayer = ServerPlayNetworking::send;
        EconomyPlatform.sendToAllPlayers = payload -> {
            var server = EconomyCommon.getServer();
            if (server != null) {
                for (ServerPlayer player : PlayerLookup.all(server)) {
                    ServerPlayNetworking.send(player, payload);
                }
            }
        };
        EconomyPlatform.getServerSupplier = () -> currentServer;
        EconomyPlatform.isClientSupplier = () -> false;
        EconomyPlatform.isModLoadedCheck = modId ->
                net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded(modId);
        EconomyPlatform.persistRuntimeConfig = () -> {
            EconomyFabricConfig config = new EconomyFabricConfig();
            config.syncFromRuntime();
            config.save();
        };
    }

    private static void registerContent() {
        ResourceKey<EntityType<?>> economyNpcKey = ResourceKey.create(Registries.ENTITY_TYPE, EconomyCommon.id("economy_npc"));
        EconomyRegistries.ECONOMY_NPC = FabricRegistryHelper.register(
                BuiltInRegistries.ENTITY_TYPE,
                economyNpcKey,
                EntityType.Builder.of(EconomyNpc::new, MobCategory.MISC)
                        .sized(0.6F, 1.95F)
                        .build(economyNpcKey)
        );
        FabricDefaultAttributeRegistry.register(EconomyRegistries.ECONOMY_NPC, EconomyNpc.createAttributes());

        ResourceKey<EntityType<?>> loanNpcKey = ResourceKey.create(Registries.ENTITY_TYPE, EconomyCommon.id("loan_npc"));
        EconomyRegistries.LOAN_NPC = FabricRegistryHelper.register(
                BuiltInRegistries.ENTITY_TYPE,
                loanNpcKey,
                EntityType.Builder.of(LoanNpc::new, MobCategory.MISC)
                        .sized(0.6F, 1.95F)
                        .build(loanNpcKey)
        );
        FabricDefaultAttributeRegistry.register(EconomyRegistries.LOAN_NPC, LoanNpc.createAttributes());

        ResourceKey<Block> atmKey = ResourceKey.create(Registries.BLOCK, EconomyCommon.id("atm"));
        EconomyRegistries.ATM_BLOCK = FabricRegistryHelper.register(
                BuiltInRegistries.BLOCK,
                atmKey,
                new AtmBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(3.0f).setId(atmKey))
        );
        ResourceKey<Item> atmItemKey = ResourceKey.create(Registries.ITEM, EconomyCommon.id("atm"));
        EconomyRegistries.ATM_BLOCK_ITEM = FabricRegistryHelper.register(
                BuiltInRegistries.ITEM,
                atmItemKey,
                new BlockItem(EconomyRegistries.ATM_BLOCK, new Item.Properties().setId(atmItemKey))
        );

        ResourceKey<Block> adminKey = ResourceKey.create(Registries.BLOCK, EconomyCommon.id("economy_admin"));
        EconomyRegistries.ECONOMY_ADMIN_BLOCK = FabricRegistryHelper.register(
                BuiltInRegistries.BLOCK,
                adminKey,
                new EconomyAdminBlock(BlockBehaviour.Properties.of()
                        .mapColor(MapColor.WOOD)
                        .strength(3.0F)
                        .sound(SoundType.WOOD)
                        .noOcclusion()
                        .setId(adminKey))
        );
        ResourceKey<Item> adminItemKey = ResourceKey.create(Registries.ITEM, EconomyCommon.id("economy_admin"));
        EconomyRegistries.ECONOMY_ADMIN_BLOCK_ITEM = FabricRegistryHelper.register(
                BuiltInRegistries.ITEM,
                adminItemKey,
                new BlockItem(EconomyRegistries.ECONOMY_ADMIN_BLOCK, new Item.Properties().setId(adminItemKey))
        );

        EconomyRegistries.GOLD_COIN = registerSimpleItem("gold_coin");
        EconomyRegistries.SILVER_COIN = registerSimpleItem("silver_coin");
        EconomyRegistries.BRONZE_COIN = registerSimpleItem("bronze_coin");

        ResourceKey<Item> mobileKey = ResourceKey.create(Registries.ITEM, EconomyCommon.id("mobile_terminal"));
        EconomyRegistries.MOBILE_TERMINAL = FabricRegistryHelper.register(
                BuiltInRegistries.ITEM,
                mobileKey,
                new MobileTerminalItem(new Item.Properties().setId(mobileKey))
        );

        ResourceKey<Item> compilerKey = ResourceKey.create(Registries.ITEM, EconomyCommon.id("ranking_compiler"));
        EconomyRegistries.RANKING_COMPILER = FabricRegistryHelper.register(
                BuiltInRegistries.ITEM,
                compilerKey,
                new RankingCompilerItem(new Item.Properties().setId(compilerKey))
        );

        ResourceKey<Item> viewerKey = ResourceKey.create(Registries.ITEM, EconomyCommon.id("ranking_viewer"));
        EconomyRegistries.RANKING_VIEWER = FabricRegistryHelper.register(
                BuiltInRegistries.ITEM,
                viewerKey,
                new RankingViewerItem(new Item.Properties().setId(viewerKey))
        );

        ResourceKey<Item> economyEggKey = ResourceKey.create(Registries.ITEM, EconomyCommon.id("economy_npc_spawn_egg"));
        EconomyRegistries.ECONOMY_NPC_SPAWN_EGG = FabricRegistryHelper.register(
                BuiltInRegistries.ITEM,
                economyEggKey,
                new SpawnEggItem(new Item.Properties().setId(economyEggKey).spawnEgg(EconomyRegistries.ECONOMY_NPC))
        );

        ResourceKey<Item> loanEggKey = ResourceKey.create(Registries.ITEM, EconomyCommon.id("loan_npc_spawn_egg"));
        EconomyRegistries.LOAN_NPC_SPAWN_EGG = FabricRegistryHelper.register(
                BuiltInRegistries.ITEM,
                loanEggKey,
                new SpawnEggItem(new Item.Properties().setId(loanEggKey).spawnEgg(EconomyRegistries.LOAN_NPC))
        );

        ResourceKey<CreativeModeTab> tabKey = ResourceKey.create(Registries.CREATIVE_MODE_TAB, EconomyCommon.id("example_tab"));
        EconomyRegistries.EXAMPLE_TAB = FabricRegistryHelper.register(
                BuiltInRegistries.CREATIVE_MODE_TAB,
                tabKey,
                FabricRegistryHelper.createTabBuilder()
                        .title(Component.translatable("itemGroup.economy"))
                        .icon(() -> EconomyRegistries.ATM_BLOCK_ITEM.getDefaultInstance())
                        .displayItems((parameters, output) -> {
                            output.accept(EconomyRegistries.ATM_BLOCK_ITEM);
                            output.accept(EconomyRegistries.GOLD_COIN);
                            output.accept(EconomyRegistries.SILVER_COIN);
                            output.accept(EconomyRegistries.BRONZE_COIN);
                            output.accept(EconomyRegistries.ECONOMY_NPC_SPAWN_EGG);
                            output.accept(EconomyRegistries.LOAN_NPC_SPAWN_EGG);
                            output.accept(EconomyRegistries.MOBILE_TERMINAL);
                            output.accept(EconomyRegistries.RANKING_COMPILER);
                            output.accept(EconomyRegistries.RANKING_VIEWER);
                            output.accept(EconomyRegistries.ECONOMY_ADMIN_BLOCK_ITEM);
                        })
                        .build()
        );
    }

    private static Item registerSimpleItem(String path) {
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, EconomyCommon.id(path));
        return FabricRegistryHelper.register(
                BuiltInRegistries.ITEM,
                key,
                new Item(new Item.Properties().setId(key))
        );
    }

    private static void registerPayloads() {
        PayloadTypeRegistry.clientboundPlay().register(ShopTxResultPayload.TYPE, ShopTxResultPayload.STREAM_CODEC);
        PayloadTypeRegistry.clientboundPlay().register(LoanTxResultPayload.TYPE, LoanTxResultPayload.STREAM_CODEC);
        PayloadTypeRegistry.clientboundPlay().register(StockTradeResultPayload.TYPE, StockTradeResultPayload.STREAM_CODEC);
        PayloadTypeRegistry.clientboundPlay().register(OpenShopScreenPayload.TYPE, OpenShopScreenPayload.STREAM_CODEC);
        PayloadTypeRegistry.clientboundPlay().register(FleaMarketResultPayload.TYPE, FleaMarketResultPayload.STREAM_CODEC);
        PayloadTypeRegistry.clientboundPlay().register(PlayerBalanceSyncPayload.TYPE, PlayerBalanceSyncPayload.STREAM_CODEC);
        PayloadTypeRegistry.clientboundPlay().register(BankResultPayload.TYPE, BankResultPayload.STREAM_CODEC);
        PayloadTypeRegistry.clientboundPlay().register(ShopDetailsResponsePayload.TYPE, ShopDetailsResponsePayload.STREAM_CODEC);
        PayloadTypeRegistry.clientboundPlay().register(EconomyQueryResponsePayload.TYPE, EconomyQueryResponsePayload.STREAM_CODEC);
        PayloadTypeRegistry.clientboundPlay().register(EconomyAdminResultPayload.TYPE, EconomyAdminResultPayload.STREAM_CODEC);
        PayloadTypeRegistry.clientboundPlay().register(EconomyFeatureFlagsPayload.TYPE, EconomyFeatureFlagsPayload.STREAM_CODEC);

        PayloadTypeRegistry.serverboundPlay().register(LoanRequestPayload.TYPE, LoanRequestPayload.STREAM_CODEC);
        PayloadTypeRegistry.serverboundPlay().register(ShopBuyRequestPayload.TYPE, ShopBuyRequestPayload.STREAM_CODEC);
        PayloadTypeRegistry.serverboundPlay().register(ShopSellRequestPayload.TYPE, ShopSellRequestPayload.STREAM_CODEC);
        PayloadTypeRegistry.serverboundPlay().register(StockTradeRequestPayload.TYPE, StockTradeRequestPayload.STREAM_CODEC);
        PayloadTypeRegistry.serverboundPlay().register(FleaMarketListRequestPayload.TYPE, FleaMarketListRequestPayload.STREAM_CODEC);
        PayloadTypeRegistry.serverboundPlay().register(FleaMarketBuyRequestPayload.TYPE, FleaMarketBuyRequestPayload.STREAM_CODEC);
        PayloadTypeRegistry.serverboundPlay().register(FleaMarketCancelRequestPayload.TYPE, FleaMarketCancelRequestPayload.STREAM_CODEC);
        PayloadTypeRegistry.serverboundPlay().register(BankRequestPayload.TYPE, BankRequestPayload.STREAM_CODEC);
        PayloadTypeRegistry.serverboundPlay().register(ShopDetailsRequestPayload.TYPE, ShopDetailsRequestPayload.STREAM_CODEC);
        PayloadTypeRegistry.serverboundPlay().register(EconomyQueryRequestPayload.TYPE, EconomyQueryRequestPayload.STREAM_CODEC);
        PayloadTypeRegistry.serverboundPlay().register(EconomyAdminActionPayload.TYPE, EconomyAdminActionPayload.STREAM_CODEC);
        PayloadTypeRegistry.serverboundPlay().register(EconomyMasterConfigPayload.TYPE, EconomyMasterConfigPayload.STREAM_CODEC);
        PayloadTypeRegistry.serverboundPlay().register(EconomyMasterEditPayload.TYPE, EconomyMasterEditPayload.STREAM_CODEC);
        PayloadTypeRegistry.serverboundPlay().register(EconomyCommonConfigPushPayload.TYPE, EconomyCommonConfigPushPayload.STREAM_CODEC);

        registerServerReceiver(LoanRequestPayload.TYPE, EconomyNetworkHandlers::handleLoanRequest);
        registerServerReceiver(ShopBuyRequestPayload.TYPE, EconomyNetworkHandlers::handleBuyRequest);
        registerServerReceiver(ShopSellRequestPayload.TYPE, EconomyNetworkHandlers::handleSellRequest);
        registerServerReceiver(StockTradeRequestPayload.TYPE, EconomyNetworkHandlers::handleStockTradeRequest);
        registerServerReceiver(FleaMarketListRequestPayload.TYPE, EconomyNetworkHandlers::handleFleaMarketListRequest);
        registerServerReceiver(FleaMarketBuyRequestPayload.TYPE, EconomyNetworkHandlers::handleFleaMarketBuyRequest);
        registerServerReceiver(FleaMarketCancelRequestPayload.TYPE, EconomyNetworkHandlers::handleFleaMarketCancelRequest);
        registerServerReceiver(BankRequestPayload.TYPE, EconomyNetworkHandlers::handleBankRequest);
        registerServerReceiver(ShopDetailsRequestPayload.TYPE, EconomyNetworkHandlers::handleShopDetailsRequest);
        registerServerReceiver(EconomyQueryRequestPayload.TYPE, EconomyNetworkHandlers::handleEconomyQueryRequest);
        registerServerReceiver(EconomyAdminActionPayload.TYPE, EconomyNetworkHandlers::handleAdminAction);
        registerServerReceiver(EconomyMasterConfigPayload.TYPE, EconomyNetworkHandlers::handleMasterConfig);
        registerServerReceiver(EconomyMasterEditPayload.TYPE, EconomyNetworkHandlers::handleMasterEdit);
        registerServerReceiver(EconomyCommonConfigPushPayload.TYPE, EconomyNetworkHandlers::handleCommonConfigPush);
    }

    private static <T extends net.minecraft.network.protocol.common.custom.CustomPacketPayload> void registerServerReceiver(
            net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<T> type,
            java.util.function.BiConsumer<T, ServerPlayer> handler) {
        ServerPlayNetworking.registerGlobalReceiver(type, (payload, context) ->
                context.server().execute(() -> handler.accept(payload, context.player())));
    }

    private static void registerServerEvents() {
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            currentServer = server;
            EconomyServerEvents.onServerStarting(server);
        });
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            EconomyServerEvents.onServerStopping(server);
            currentServer = null;
        });
        ServerTickEvents.END_SERVER_TICK.register(EconomyServerEvents::onServerTick);

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                EconomyCommands.register(dispatcher));

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
                EconomyServerEvents.onPlayerLoggedIn(handler.getPlayer()));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
                EconomyServerEvents.onPlayerLoggedOut(handler.getPlayer()));

        ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, baseDamageTaken, damageTaken, blocked) ->
                EconomyServerEvents.onLivingDamage(entity, damageTaken, source));
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, damageSource) ->
                EconomyServerEvents.onLivingDeath(entity, damageSource, entity.level()));

        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) ->
                EconomyServerEvents.onBlockBreak(player, state, world, pos));

        // Block place tracking (melon/pumpkin anti-farm): no Fabric callback equivalent; harvest rewards may differ slightly.

        ServerEntityEvents.ENTITY_LOAD.register((entity, world) ->
                EconomyServerEvents.onEntityJoinLevel(entity, world, false));

        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            final InteractionResult[] result = {InteractionResult.PASS};
            EconomyServerEvents.onEntityInteract(new EconomyServerEvents.EconomyEntityInteractContext(
                    player,
                    entity,
                    player.getItemInHand(hand),
                    world,
                    interactionResult -> result[0] = interactionResult
            ));
            return result[0];
        });

        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            EconomyServerEvents.onPlayerLoggedIn(newPlayer);
        });
    }
}
