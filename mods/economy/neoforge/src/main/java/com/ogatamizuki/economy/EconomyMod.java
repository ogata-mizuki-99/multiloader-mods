package com.ogatamizuki.economy;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.ItemFishedEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.slf4j.Logger;

@Mod(EconomyCommon.MODID)
public class EconomyMod {
    public static final String MODID = EconomyCommon.MODID;
    public static final Logger LOGGER = EconomyCommon.LOGGER;

    public static boolean isEconomyReady(java.util.UUID playerUuid) {
        return EconomyCommon.isEconomyReady(playerUuid);
    }

    public static boolean isEconomyReady() {
        return EconomyCommon.isEconomyReady();
    }

    public static void setEconomyReady(java.util.UUID playerUuid, boolean ready) {
        EconomyCommon.setEconomyReady(playerUuid, ready);
    }

    public static int getCurrentBalance() {
        return EconomyCommon.getCurrentBalance();
    }

    public static void setCurrentBalance(int balance) {
        EconomyCommon.setCurrentBalance(balance);
    }

    public static int getCurrentBankBalance() {
        return EconomyCommon.getCurrentBankBalance();
    }

    public static void setCurrentBankBalance(int balance) {
        EconomyCommon.setCurrentBankBalance(balance);
    }

    public static int getCurrentDebt() {
        return EconomyCommon.getCurrentDebt();
    }

    public static void setCurrentDebt(int debt) {
        EconomyCommon.setCurrentDebt(debt);
    }

    public static MinecraftServer getServer() {
        return EconomyCommon.getServer();
    }

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<EconomyNpc>> ECONOMY_NPC = ENTITY_TYPES.register("economy_npc",
            () -> EntityType.Builder.of(EconomyNpc::new, MobCategory.MISC)
                    .sized(0.6F, 1.95F)
                    .build(net.minecraft.resources.ResourceKey.create(
                            Registries.ENTITY_TYPE,
                            Identifier.fromNamespaceAndPath(MODID, "economy_npc"))));

    public static final DeferredHolder<EntityType<?>, EntityType<LoanNpc>> LOAN_NPC = ENTITY_TYPES.register("loan_npc",
            () -> EntityType.Builder.of(LoanNpc::new, MobCategory.MISC)
                    .sized(0.6F, 1.95F)
                    .build(net.minecraft.resources.ResourceKey.create(
                            Registries.ENTITY_TYPE,
                            Identifier.fromNamespaceAndPath(MODID, "loan_npc"))));

    public static final DeferredBlock<AtmBlock> ATM_BLOCK = BLOCKS.registerBlock("atm", AtmBlock::new, p -> p.mapColor(MapColor.METAL).strength(3.0f));
    public static final DeferredItem<BlockItem> ATM_BLOCK_ITEM = ITEMS.registerSimpleBlockItem("atm", ATM_BLOCK);

    public static final DeferredBlock<EconomyAdminBlock> ECONOMY_ADMIN_BLOCK = BLOCKS.registerBlock(
            "economy_admin",
            EconomyAdminBlock::new,
            p -> p.mapColor(MapColor.WOOD)
                    .strength(3.0F)
                    .sound(SoundType.WOOD)
                    .noOcclusion());
    public static final DeferredItem<BlockItem> ECONOMY_ADMIN_BLOCK_ITEM = ITEMS.registerSimpleBlockItem("economy_admin", ECONOMY_ADMIN_BLOCK);

    public static final DeferredItem<Item> GOLD_COIN = ITEMS.registerSimpleItem("gold_coin");
    public static final DeferredItem<Item> SILVER_COIN = ITEMS.registerSimpleItem("silver_coin");
    public static final DeferredItem<Item> BRONZE_COIN = ITEMS.registerSimpleItem("bronze_coin");

    public static final DeferredItem<MobileTerminalItem> MOBILE_TERMINAL = ITEMS.registerItem("mobile_terminal", MobileTerminalItem::new);
    public static final DeferredItem<RankingCompilerItem> RANKING_COMPILER = ITEMS.registerItem("ranking_compiler", RankingCompilerItem::new);
    public static final DeferredItem<RankingViewerItem> RANKING_VIEWER = ITEMS.registerItem("ranking_viewer", RankingViewerItem::new);

    public static final DeferredItem<SpawnEggItem> ECONOMY_NPC_SPAWN_EGG = ITEMS.registerItem("economy_npc_spawn_egg",
            properties -> new SpawnEggItem(properties.spawnEgg(ECONOMY_NPC.get())));

    public static final DeferredItem<SpawnEggItem> LOAN_NPC_SPAWN_EGG = ITEMS.registerItem("loan_npc_spawn_egg",
            properties -> new SpawnEggItem(properties.spawnEgg(LOAN_NPC.get())));

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> EXAMPLE_TAB = CREATIVE_MODE_TABS.register("example_tab", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.economy"))
            .withTabsBefore(CreativeModeTabs.COMBAT)
            .icon(() -> ATM_BLOCK_ITEM.get().getDefaultInstance())
            .displayItems((parameters, output) -> {
                output.accept(ATM_BLOCK_ITEM.get());
                output.accept(GOLD_COIN.get());
                output.accept(SILVER_COIN.get());
                output.accept(BRONZE_COIN.get());
                output.accept(ECONOMY_NPC_SPAWN_EGG.get());
                output.accept(LOAN_NPC_SPAWN_EGG.get());
                output.accept(MOBILE_TERMINAL.get());
                output.accept(RANKING_COMPILER.get());
                output.accept(RANKING_VIEWER.get());
                output.accept(ECONOMY_ADMIN_BLOCK_ITEM.get());
            }).build());

    public EconomyMod(IEventBus modEventBus, ModContainer modContainer) {
        EconomyPlatform.sendToPlayer = PacketDistributor::sendToPlayer;
        EconomyPlatform.sendToAllPlayers = PacketDistributor::sendToAllPlayers;
        EconomyPlatform.getServerSupplier = net.neoforged.neoforge.server.ServerLifecycleHooks::getCurrentServer;
        EconomyPlatform.isClientSupplier = () -> FMLEnvironment.getDist() == Dist.CLIENT;
        EconomyPlatform.isModLoadedCheck = ModList.get()::isLoaded;
        EconomyPlatform.persistRuntimeConfig = Config::syncFromRuntimeConfig;
        EconomyPlatform.getEntityPersistentData = entity -> entity.getPersistentData();

        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::onConfigLoad);
        modEventBus.addListener(this::onConfigReload);

        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);
        ENTITY_TYPES.register(modEventBus);

        modEventBus.addListener(this::createEntityAttributes);
        modEventBus.addListener(this::registerPayloads);

        NeoForge.EVENT_BUS.register(this);

        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    public static void syncRegistries() {
        EconomyRegistries.ECONOMY_NPC = ECONOMY_NPC.get();
        EconomyRegistries.LOAN_NPC = LOAN_NPC.get();
        EconomyRegistries.ATM_BLOCK = ATM_BLOCK.get();
        EconomyRegistries.ATM_BLOCK_ITEM = ATM_BLOCK_ITEM.get();
        EconomyRegistries.ECONOMY_ADMIN_BLOCK = ECONOMY_ADMIN_BLOCK.get();
        EconomyRegistries.ECONOMY_ADMIN_BLOCK_ITEM = ECONOMY_ADMIN_BLOCK_ITEM.get();
        EconomyRegistries.GOLD_COIN = GOLD_COIN.get();
        EconomyRegistries.SILVER_COIN = SILVER_COIN.get();
        EconomyRegistries.BRONZE_COIN = BRONZE_COIN.get();
        EconomyRegistries.MOBILE_TERMINAL = MOBILE_TERMINAL.get();
        EconomyRegistries.RANKING_COMPILER = RANKING_COMPILER.get();
        EconomyRegistries.RANKING_VIEWER = RANKING_VIEWER.get();
        EconomyRegistries.ECONOMY_NPC_SPAWN_EGG = ECONOMY_NPC_SPAWN_EGG.get();
        EconomyRegistries.LOAN_NPC_SPAWN_EGG = LOAN_NPC_SPAWN_EGG.get();
        EconomyRegistries.EXAMPLE_TAB = EXAMPLE_TAB.get();
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        syncRegistries();
        LOGGER.info("Economy standalone mod common setup");
    }

    private void onConfigLoad(ModConfigEvent.Loading event) {
        if (event.getConfig().getSpec() != Config.SPEC) {
            return;
        }
        Config.syncToRuntimeConfig();
    }

    private void onConfigReload(ModConfigEvent.Reloading event) {
        if (event.getConfig().getSpec() != Config.SPEC) {
            return;
        }
        Config.syncToRuntimeConfig();
        EconomyFeatures.onConfigReloaded(ServerLifecycleHooks.getCurrentServer());
    }

    private void createEntityAttributes(EntityAttributeCreationEvent event) {
        event.put(ECONOMY_NPC.get(), EconomyNpc.createAttributes().build());
        event.put(LOAN_NPC.get(), LoanNpc.createAttributes().build());
    }

    private void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");

        registrar.playToClient(ShopTxResultPayload.TYPE, ShopTxResultPayload.STREAM_CODEC);
        registrar.playToClient(LoanTxResultPayload.TYPE, LoanTxResultPayload.STREAM_CODEC);
        registrar.playToClient(StockTradeResultPayload.TYPE, StockTradeResultPayload.STREAM_CODEC);
        registrar.playToClient(OpenShopScreenPayload.TYPE, OpenShopScreenPayload.STREAM_CODEC);
        registrar.playToClient(FleaMarketResultPayload.TYPE, FleaMarketResultPayload.STREAM_CODEC);
        registrar.playToClient(PlayerBalanceSyncPayload.TYPE, PlayerBalanceSyncPayload.STREAM_CODEC);
        registrar.playToClient(BankResultPayload.TYPE, BankResultPayload.STREAM_CODEC);
        registrar.playToClient(ShopDetailsResponsePayload.TYPE, ShopDetailsResponsePayload.STREAM_CODEC);
        registrar.playToClient(EconomyQueryResponsePayload.TYPE, EconomyQueryResponsePayload.STREAM_CODEC);
        registrar.playToClient(EconomyAdminResultPayload.TYPE, EconomyAdminResultPayload.STREAM_CODEC);
        registrar.playToClient(EconomyFeatureFlagsPayload.TYPE, EconomyFeatureFlagsPayload.STREAM_CODEC);

        registrar.playToServer(LoanRequestPayload.TYPE, LoanRequestPayload.STREAM_CODEC,
                (payload, context) -> forwardToServerPlayer(context, sp -> EconomyNetworkHandlers.handleLoanRequest(payload, sp)));
        registrar.playToServer(ShopBuyRequestPayload.TYPE, ShopBuyRequestPayload.STREAM_CODEC,
                (payload, context) -> forwardToServerPlayer(context, sp -> EconomyNetworkHandlers.handleBuyRequest(payload, sp)));
        registrar.playToServer(ShopSellRequestPayload.TYPE, ShopSellRequestPayload.STREAM_CODEC,
                (payload, context) -> forwardToServerPlayer(context, sp -> EconomyNetworkHandlers.handleSellRequest(payload, sp)));
        registrar.playToServer(StockTradeRequestPayload.TYPE, StockTradeRequestPayload.STREAM_CODEC,
                (payload, context) -> forwardToServerPlayer(context, sp -> EconomyNetworkHandlers.handleStockTradeRequest(payload, sp)));
        registrar.playToServer(FleaMarketListRequestPayload.TYPE, FleaMarketListRequestPayload.STREAM_CODEC,
                (payload, context) -> forwardToServerPlayer(context, sp -> EconomyNetworkHandlers.handleFleaMarketListRequest(payload, sp)));
        registrar.playToServer(FleaMarketBuyRequestPayload.TYPE, FleaMarketBuyRequestPayload.STREAM_CODEC,
                (payload, context) -> forwardToServerPlayer(context, sp -> EconomyNetworkHandlers.handleFleaMarketBuyRequest(payload, sp)));
        registrar.playToServer(FleaMarketCancelRequestPayload.TYPE, FleaMarketCancelRequestPayload.STREAM_CODEC,
                (payload, context) -> forwardToServerPlayer(context, sp -> EconomyNetworkHandlers.handleFleaMarketCancelRequest(payload, sp)));
        registrar.playToServer(BankRequestPayload.TYPE, BankRequestPayload.STREAM_CODEC,
                (payload, context) -> forwardToServerPlayer(context, sp -> EconomyNetworkHandlers.handleBankRequest(payload, sp)));
        registrar.playToServer(ShopDetailsRequestPayload.TYPE, ShopDetailsRequestPayload.STREAM_CODEC,
                (payload, context) -> forwardToServerPlayer(context, sp -> EconomyNetworkHandlers.handleShopDetailsRequest(payload, sp)));
        registrar.playToServer(EconomyQueryRequestPayload.TYPE, EconomyQueryRequestPayload.STREAM_CODEC,
                (payload, context) -> forwardToServerPlayer(context, sp -> EconomyNetworkHandlers.handleEconomyQueryRequest(payload, sp)));
        registrar.playToServer(EconomyAdminActionPayload.TYPE, EconomyAdminActionPayload.STREAM_CODEC,
                (payload, context) -> forwardToServerPlayer(context, sp -> EconomyNetworkHandlers.handleAdminAction(payload, sp)));
        registrar.playToServer(EconomyMasterConfigPayload.TYPE, EconomyMasterConfigPayload.STREAM_CODEC,
                (payload, context) -> forwardToServerPlayer(context, sp -> EconomyNetworkHandlers.handleMasterConfig(payload, sp)));
        registrar.playToServer(EconomyMasterEditPayload.TYPE, EconomyMasterEditPayload.STREAM_CODEC,
                (payload, context) -> forwardToServerPlayer(context, sp -> EconomyNetworkHandlers.handleMasterEdit(payload, sp)));
        registrar.playToServer(EconomyCommonConfigPushPayload.TYPE, EconomyCommonConfigPushPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer serverPlayer) {
                        EconomyNetworkHandlers.handleCommonConfigPush(payload, serverPlayer);
                    }
                }));
    }

    private static void forwardToServerPlayer(
            net.neoforged.neoforge.network.handling.IPayloadContext context,
            java.util.function.Consumer<ServerPlayer> handler) {
        if (context.player() instanceof ServerPlayer serverPlayer) {
            handler.accept(serverPlayer);
        }
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        EconomyServerEvents.onServerStarting(event.getServer());
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        EconomyServerEvents.onServerStopping(event.getServer());
    }

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        EconomyServerEvents.onServerTick(event.getServer());
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        EconomyCommands.register(event.getDispatcher());
    }

    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        EconomyServerEvents.onPlayerLoggedIn(event.getEntity());
    }

    @SubscribeEvent
    public void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        EconomyServerEvents.onPlayerLoggedOut(event.getEntity());
    }

    @SubscribeEvent
    public void onLivingDamage(LivingDamageEvent.Post event) {
        EconomyServerEvents.onLivingDamage(event.getEntity(), event.getOriginalDamage(), event.getSource());
    }

    @SubscribeEvent
    public void onLivingDeath(LivingDeathEvent event) {
        EconomyServerEvents.onLivingDeath(event.getEntity(), event.getSource(), event.getEntity().level());
    }

    @SubscribeEvent
    public void onBlockBreak(BreakBlockEvent event) {
        EconomyServerEvents.onBlockBreak(event.getPlayer(), event.getState(), event.getLevel(), event.getPos());
    }

    @SubscribeEvent
    public void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (event.getEntity() instanceof net.minecraft.world.entity.player.Player player) {
            EconomyServerEvents.onBlockPlace(player, event.getPlacedBlock(), event.getLevel(), event.getPos());
        }
    }

    @SubscribeEvent
    public void onItemFished(ItemFishedEvent event) {
        EconomyServerEvents.onItemFished(event.getEntity(), event.getDrops());
    }

    @SubscribeEvent
    public void onEntityJoinLevel(EntityJoinLevelEvent event) {
        EconomyServerEvents.onEntityJoinLevel(event.getEntity(), event.getLevel(), event.loadedFromDisk());
    }

    @SubscribeEvent
    public void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        EconomyServerEvents.onEntityInteract(new EconomyServerEvents.EconomyEntityInteractContext(
                event.getEntity(),
                event.getTarget(),
                event.getItemStack(),
                event.getLevel(),
                result -> {
                    event.setCanceled(true);
                    event.setCancellationResult(result);
                }
        ));
    }
}
