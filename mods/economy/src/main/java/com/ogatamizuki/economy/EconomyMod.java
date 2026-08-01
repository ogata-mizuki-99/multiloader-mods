package com.ogatamizuki.economy;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import com.ogatamizuki.economy.master.EconomyMasterData;
import com.ogatamizuki.economy.backend.EconomyBalanceSync;
import com.ogatamizuki.economy.backend.EconomyEtfPriceScheduler;
import net.minecraft.server.MinecraftServer;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.event.entity.player.ItemFishedEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.UUID;
import java.util.Map;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.minecraft.world.entity.EntityType;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.network.PacketDistributor;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.Holder;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.minecraft.commands.Commands;
import net.minecraft.commands.CommandSourceStack;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.permissions.Permissions;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(EconomyMod.MODID)
public class EconomyMod {
    // Define mod id in a common place for everything to reference
    public static final String MODID = "economy";
    // Directly reference a slf4j logger
    public static final Logger LOGGER = LogUtils.getLogger();

    private static int currentBalance = 0;
    private static int currentBankBalance = 0;
    private static int currentDebt = 0;
    private static MinecraftServer minecraftServer;
    private static final Set<UUID> economyReadyUuids = ConcurrentHashMap.newKeySet();

    // 環境ダメージ追跡用のダミーUUID (環境からの落下・炎上ダメージ等のロスト用)
    public static final UUID ENVIRONMENT_UUID = new UUID(0, 0);

    // 敵のUUID -> (プレイヤーのUUID -> 与えた累積ダメージ)
    private static final Map<UUID, Map<UUID, Float>> damageTracker = new ConcurrentHashMap<>();

    public static boolean isEconomyReady(UUID playerUuid) {
        return playerUuid != null && economyReadyUuids.contains(playerUuid);
    }

    /** クライアント側: ローカルプレイヤーの経済データが同期済みか */
    public static boolean isEconomyReady() {
        if (net.neoforged.fml.loading.FMLEnvironment.getDist() == net.neoforged.api.distmarker.Dist.CLIENT) {
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
            if (mc.player != null) {
                return isEconomyReady(mc.player.getUUID());
            }
        }
        return false;
    }

    public static void setEconomyReady(UUID playerUuid, boolean ready) {
        if (playerUuid == null) {
            return;
        }
        if (ready) {
            economyReadyUuids.add(playerUuid);
        } else {
            economyReadyUuids.remove(playerUuid);
        }
    }

    public static int getCurrentBalance() {
        return currentBalance;
    }

    public static void setCurrentBalance(int balance) {
        currentBalance = balance;
    }

    public static int getCurrentBankBalance() {
        return currentBankBalance;
    }

    public static void setCurrentBankBalance(int balance) {
        currentBankBalance = balance;
    }

    public static int getCurrentDebt() {
        return currentDebt;
    }

    public static void setCurrentDebt(int debt) {
        currentDebt = debt;
    }

    public static MinecraftServer getServer() {
        return minecraftServer;
    }
    // Create a Deferred Register to hold Blocks which will all be registered under the "economy" namespace
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);
    // Create a Deferred Register to hold Items which will all be registered under the "economy" namespace
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    // Create a Deferred Register to hold CreativeModeTabs which will all be registered under the "economy" namespace
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);
    // Create a Deferred Register to hold EntityTypes which will all be registered under the "economy" namespace
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, MODID);
    // タイトル演出 SE（TitleScreenOverlay 再有効化時に登録。参照: TitleScreenOverlay.java）
    // public static final DeferredRegister<SoundEvent> SOUND_EVENTS = DeferredRegister.create(Registries.SOUND_EVENT, MODID);
    // public static final DeferredHolder<SoundEvent, SoundEvent> TITLE_TRANSITION_SE = SOUND_EVENTS.register(
    //         "title_transition",
    //         () -> SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath(MODID, "title_transition")));

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

    // ATM Block
    public static final DeferredBlock<AtmBlock> ATM_BLOCK = BLOCKS.registerBlock("atm", AtmBlock::new, p -> p.mapColor(MapColor.METAL).strength(3.0f));
    public static final DeferredItem<BlockItem> ATM_BLOCK_ITEM = ITEMS.registerSimpleBlockItem("atm", ATM_BLOCK);

    // Economy Admin Block (GM only)
    public static final DeferredBlock<EconomyAdminBlock> ECONOMY_ADMIN_BLOCK = BLOCKS.registerBlock(
            "economy_admin",
            EconomyAdminBlock::new,
            p -> p.mapColor(MapColor.WOOD)
                    .strength(3.0F)
                    .sound(SoundType.WOOD)
                    .noOcclusion());
    public static final DeferredItem<BlockItem> ECONOMY_ADMIN_BLOCK_ITEM = ITEMS.registerSimpleBlockItem("economy_admin", ECONOMY_ADMIN_BLOCK);

    // Coin Items
    public static final DeferredItem<Item> GOLD_COIN = ITEMS.registerSimpleItem("gold_coin");
    public static final DeferredItem<Item> SILVER_COIN = ITEMS.registerSimpleItem("silver_coin");
    public static final DeferredItem<Item> BRONZE_COIN = ITEMS.registerSimpleItem("bronze_coin");

    // Mobile terminal for ETF trading
    public static final DeferredItem<MobileTerminalItem> MOBILE_TERMINAL = ITEMS.registerItem("mobile_terminal", MobileTerminalItem::new);

    // Ranking Aggregator / Compiler for Admins
    public static final DeferredItem<RankingCompilerItem> RANKING_COMPILER = ITEMS.registerItem("ranking_compiler", RankingCompilerItem::new);

    // Ranking viewer for players (opens GUI)
    public static final DeferredItem<RankingViewerItem> RANKING_VIEWER = ITEMS.registerItem("ranking_viewer", RankingViewerItem::new);

    // Spawn Egg Item
    public static final DeferredItem<SpawnEggItem> ECONOMY_NPC_SPAWN_EGG = ITEMS.registerItem("economy_npc_spawn_egg",
            properties -> new SpawnEggItem(properties.spawnEgg(ECONOMY_NPC.get())));

    public static final DeferredItem<SpawnEggItem> LOAN_NPC_SPAWN_EGG = ITEMS.registerItem("loan_npc_spawn_egg",
            properties -> new SpawnEggItem(properties.spawnEgg(LOAN_NPC.get())));

    // Creates a creative tab with the id "economy:example_tab" for the ATM, that is placed after the combat tab
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> EXAMPLE_TAB = CREATIVE_MODE_TABS.register("example_tab", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.economy")) //The language key for the title of your CreativeModeTab
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

    // The constructor for the mod class is the first code that is run when your mod is loaded.
    // FML will recognize some parameter types like IEventBus or ModContainer and pass them in automatically.
    public EconomyMod(IEventBus modEventBus, ModContainer modContainer) {
        // Register the commonSetup method for modloading
        modEventBus.addListener(this::commonSetup);

        // Register the Deferred Register to the mod event bus so blocks get registered
        BLOCKS.register(modEventBus);
        // Register the Deferred Register to the mod event bus so items get registered
        ITEMS.register(modEventBus);
        // Register the Deferred Register to the mod event bus so tabs get registered
        CREATIVE_MODE_TABS.register(modEventBus);
        // Register the Deferred Register to the mod event bus so entities get registered
        ENTITY_TYPES.register(modEventBus);
        // SOUND_EVENTS.register(modEventBus); // TitleScreenOverlay 再有効化時

        modEventBus.addListener(this::createEntityAttributes);
        modEventBus.addListener(this::registerPayloads);
        modEventBus.addListener(this::onConfigReload);

        if (net.neoforged.fml.loading.FMLEnvironment.getDist() == net.neoforged.api.distmarker.Dist.CLIENT) {
            modEventBus.addListener(ClientAccess::registerRenderers);
        }

        // Register ourselves for server and other game events we are interested in.
        // Note that this is necessary if and only if we want *this* class (EconomyMod) to respond directly to events.
        // Do not add this line if there are no @SubscribeEvent-annotated functions in this class, like onServerStarting() below.
        NeoForge.EVENT_BUS.register(this);

        // Register our mod's ModConfigSpec so that FML can create and load the config file for us
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void createEntityAttributes(EntityAttributeCreationEvent event) {
        event.put(ECONOMY_NPC.get(), EconomyNpc.createAttributes().build());
        event.put(LOAN_NPC.get(), LoanNpc.createAttributes().build());
    }

    private void onConfigReload(ModConfigEvent.Reloading event) {
        if (event.getConfig().getSpec() != Config.SPEC) {
            return;
        }
        MinecraftServer server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return;
        }
        server.execute(() -> {
            EconomyFeatures.syncToAllPlayers();
            EconomyEtfPriceScheduler.stop();
            EconomyEtfPriceScheduler.start(server);
            LOGGER.info(
                    "Economy feature config reloaded: hud={}, actionRewards={}, etfUpdates={}, rewardChatAggregateSeconds={}",
                    Config.ENABLE_BALANCE_HUD.get(),
                    Config.ENABLE_ACTION_REWARDS.get(),
                    Config.ENABLE_ETF_UPDATES.get(),
                    Config.REWARD_CHAT_AGGREGATE_SECONDS.get()
            );
        });
    }

    /**
     * カスタムネットワークパケット（Payload）の登録。
     * playToServer はここでハンドラ付き登録。
     * playToClient は Dedicated Server でもチャンネル定義が必要なため TYPE+CODEC のみここで登録し、
     * 受信ハンドラは EconomyModClient の RegisterClientPayloadHandlersEvent で登録。
     */
    private void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");

        // サーバー → クライアント: チャンネル定義（Dedicated Server 側必須）
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

        // クライアント → サーバー: 借入・返済リクエスト
        registrar.playToServer(
                LoanRequestPayload.TYPE,
                LoanRequestPayload.STREAM_CODEC,
                this::handleLoanRequest);

        // クライアント → サーバー: 購入リクエスト
        registrar.playToServer(
                ShopBuyRequestPayload.TYPE,
                ShopBuyRequestPayload.STREAM_CODEC,
                this::handleBuyRequest);

        // クライアント → サーバー: 売却リクエスト
        registrar.playToServer(
                ShopSellRequestPayload.TYPE,
                ShopSellRequestPayload.STREAM_CODEC,
                this::handleSellRequest);

        // クライアント → サーバー: ETF取引リクエスト
        registrar.playToServer(
                StockTradeRequestPayload.TYPE,
                StockTradeRequestPayload.STREAM_CODEC,
                this::handleStockTradeRequest);

        // クライアント → サーバー: フリマ出品リクエスト
        registrar.playToServer(
                FleaMarketListRequestPayload.TYPE,
                FleaMarketListRequestPayload.STREAM_CODEC,
                this::handleFleaMarketListRequest);

        // クライアント → サーバー: フリマ購入リクエスト
        registrar.playToServer(
                FleaMarketBuyRequestPayload.TYPE,
                FleaMarketBuyRequestPayload.STREAM_CODEC,
                this::handleFleaMarketBuyRequest);

        // クライアント → サーバー: フリマキャンセルリクエスト
        registrar.playToServer(
                FleaMarketCancelRequestPayload.TYPE,
                FleaMarketCancelRequestPayload.STREAM_CODEC,
                this::handleFleaMarketCancelRequest);

        registrar.playToServer(
                BankRequestPayload.TYPE,
                BankRequestPayload.STREAM_CODEC,
                this::handleBankRequest);

        registrar.playToServer(
                ShopDetailsRequestPayload.TYPE,
                ShopDetailsRequestPayload.STREAM_CODEC,
                this::handleShopDetailsRequest);

        registrar.playToServer(
                EconomyQueryRequestPayload.TYPE,
                EconomyQueryRequestPayload.STREAM_CODEC,
                this::handleEconomyQueryRequest);

        registrar.playToServer(
                EconomyAdminActionPayload.TYPE,
                EconomyAdminActionPayload.STREAM_CODEC,
                this::handleAdminAction);

        registrar.playToServer(
                EconomyMasterConfigPayload.TYPE,
                EconomyMasterConfigPayload.STREAM_CODEC,
                this::handleMasterConfig);

        registrar.playToServer(
                EconomyMasterEditPayload.TYPE,
                EconomyMasterEditPayload.STREAM_CODEC,
                this::handleMasterEdit);

        registrar.playToServer(
                EconomyCommonConfigPushPayload.TYPE,
                EconomyCommonConfigPushPayload.STREAM_CODEC,
                this::handleCommonConfigPush);
    }

    private void handleCommonConfigPush(
            EconomyCommonConfigPushPayload payload,
            net.neoforged.neoforge.network.handling.IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer serverPlayer)) {
                return;
            }
            if (!EconomyAdminAuth.canPerformAdminActions(serverPlayer)) {
                serverPlayer.sendSystemMessage(
                        Component.translatable("economy.configuration.push_denied")
                                .withStyle(net.minecraft.ChatFormatting.RED));
                return;
            }
            int aggregate = Math.max(0, Math.min(30, payload.rewardChatAggregateSeconds()));
            Config.ENABLE_BALANCE_HUD.set(payload.enableBalanceHud());
            Config.ENABLE_BALANCE_HUD.save();
            Config.ENABLE_ACTION_REWARDS.set(payload.enableActionRewards());
            Config.ENABLE_ACTION_REWARDS.save();
            Config.ENABLE_ETF_UPDATES.set(payload.enableEtfUpdates());
            Config.ENABLE_ETF_UPDATES.save();
            Config.REWARD_CHAT_AGGREGATE_SECONDS.set(aggregate);
            Config.REWARD_CHAT_AGGREGATE_SECONDS.save();

            EconomyFeatures.syncToAllPlayers();
            EconomyEtfPriceScheduler.stop();
            MinecraftServer server = serverPlayer.level().getServer();
            if (server != null) {
                EconomyEtfPriceScheduler.start(server);
            }
            LOGGER.info(
                    "Economy common config pushed by {}: hud={}, actionRewards={}, etfUpdates={}, rewardChatAggregateSeconds={}",
                    serverPlayer.getGameProfile().name(),
                    Config.ENABLE_BALANCE_HUD.get(),
                    Config.ENABLE_ACTION_REWARDS.get(),
                    Config.ENABLE_ETF_UPDATES.get(),
                    Config.REWARD_CHAT_AGGREGATE_SECONDS.get()
            );
            serverPlayer.sendSystemMessage(
                    Component.translatable("economy.configuration.push_ok")
                            .withStyle(net.minecraft.ChatFormatting.GREEN));
        });
    }

    private void handleMasterEdit(EconomyMasterEditPayload payload, net.neoforged.neoforge.network.handling.IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer serverPlayer)) {
            return;
        }
        if (!EconomyAdminAuth.canPerformAdminActions(serverPlayer)) {
            PacketDistributor.sendToPlayer(
                    serverPlayer,
                    new EconomyAdminResultPayload(false, "§c[経済] 管理操作の権限（管理者権限）がありません。")
            );
            return;
        }

        com.google.gson.JsonObject result = switch (payload.action()) {
            case "SAVE_REWARDS" -> com.ogatamizuki.economy.backend.local.EconomyLocalAdminService.saveMasterRewardEdits(payload.jsonBody());
            case "SAVE_ITEMS" -> com.ogatamizuki.economy.backend.local.EconomyLocalAdminService.saveMasterItemEdits(payload.jsonBody());
            case "SAVE_SHOP_ITEMS" -> com.ogatamizuki.economy.backend.local.EconomyLocalAdminService.saveMasterShopItemEdits(payload.jsonBody());
            case "SAVE_ETF_ITEMS" -> com.ogatamizuki.economy.backend.local.EconomyLocalAdminService.saveMasterEtfItemEdits(payload.jsonBody());
            default -> {
                com.google.gson.JsonObject err = new com.google.gson.JsonObject();
                err.addProperty("error", "不明なマスタ編集操作です: " + payload.action());
                yield err;
            }
        };

        if (result.has("success") && result.get("success").getAsBoolean()) {
            String msg = result.has("message") ? result.get("message").getAsString() : "マスタを反映しました。";
            PacketDistributor.sendToPlayer(serverPlayer, new EconomyAdminResultPayload(true, "§a[経済] " + msg));
        } else {
            String error = result.has("error") ? result.get("error").getAsString() : "マスタ編集の反映に失敗しました。";
            PacketDistributor.sendToPlayer(serverPlayer, new EconomyAdminResultPayload(false, "§c[経済] " + error));
        }
    }

    private void handleMasterConfig(EconomyMasterConfigPayload payload, net.neoforged.neoforge.network.handling.IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer serverPlayer)) {
            return;
        }
        if (!EconomyAdminAuth.canPerformAdminActions(serverPlayer)) {
            PacketDistributor.sendToPlayer(
                    serverPlayer,
                    new EconomyAdminResultPayload(false, "§c[経済] 管理操作の権限（管理者権限）がありません。")
            );
            return;
        }

        com.google.gson.JsonObject result;
        if ("RESET_OVERRIDE".equalsIgnoreCase(payload.action())) {
            result = com.ogatamizuki.economy.backend.local.EconomyLocalAdminService.resetMasterConfig();
        } else if ("SAVE".equalsIgnoreCase(payload.action())) {
            var values = new com.ogatamizuki.economy.master.EconomyMasterData.MasterConfigValues(
                    clamp(payload.deathPenaltyRate(), 0.0, 1.0),
                    clamp(payload.shortSellLimitRate(), 0.0, 10.0),
                    Math.max(1, Math.min(payload.etfIntervalMinutes(), 1440)),
                    Math.max(0, payload.loanMaxAmount()),
                    clamp(payload.loanAssetMultiplier(), 0.0, 100.0)
            );
            result = com.ogatamizuki.economy.backend.local.EconomyLocalAdminService.saveMasterConfig(values);
        } else {
            PacketDistributor.sendToPlayer(
                    serverPlayer,
                    new EconomyAdminResultPayload(false, "§c[経済] 不明なマスタ操作です: " + payload.action())
            );
            return;
        }

        if (result.has("success") && result.get("success").getAsBoolean()) {
            String msg = result.has("message") ? result.get("message").getAsString() : "マスタ設定を反映しました。";
            PacketDistributor.sendToPlayer(serverPlayer, new EconomyAdminResultPayload(true, "§a[経済] " + msg));
        } else {
            String error = result.has("error") ? result.get("error").getAsString() : "マスタ設定の反映に失敗しました。";
            PacketDistributor.sendToPlayer(serverPlayer, new EconomyAdminResultPayload(false, "§c[経済] " + error));
        }
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static boolean isAdminEconomyQuery(String queryType) {
        return switch (queryType) {
            case "PLAYER_BALANCES", "MASTER_CONFIG", "MASTER_REWARDS", "MASTER_ITEMS",
                    "MASTER_SHOPS", "MASTER_SHOP_ITEMS", "MASTER_ETF_ITEMS" -> true;
            default -> false;
        };
    }

    private void handleAdminAction(EconomyAdminActionPayload payload, net.neoforged.neoforge.network.handling.IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer serverPlayer)) {
            return;
        }
        if (!EconomyAdminAuth.canPerformAdminActions(serverPlayer)) {
            PacketDistributor.sendToPlayer(
                    serverPlayer,
                    new EconomyAdminResultPayload(false, "§c[経済] 管理操作の権限（管理者権限）がありません。")
            );
            return;
        }

        if ("COMPILE_RANKING".equalsIgnoreCase(payload.action())) {
            compileRanking(serverPlayer.createCommandSourceStack());
            PacketDistributor.sendToPlayer(
                    serverPlayer,
                    new EconomyAdminResultPayload(true, "§a[経済] ランキング集計を開始しました。チャットで進捗を確認してください。")
            );
            return;
        }

        if ("RELOAD_MASTER".equalsIgnoreCase(payload.action())) {
            com.google.gson.JsonObject result = com.ogatamizuki.economy.backend.local.EconomyLocalAdminService.reloadMaster();
            if (result.has("success") && result.get("success").getAsBoolean()) {
                String msg = result.has("message") ? result.get("message").getAsString() : "マスタを再読込しました。";
                PacketDistributor.sendToPlayer(serverPlayer, new EconomyAdminResultPayload(true, "§a[経済] " + msg));
            } else {
                String error = result.has("error") ? result.get("error").getAsString() : "マスタ再読込に失敗しました。";
                PacketDistributor.sendToPlayer(serverPlayer, new EconomyAdminResultPayload(false, "§c[経済] " + error));
            }
            return;
        }

        if ("GIVE_SPAWN_EGG".equalsIgnoreCase(payload.action())) {
            com.google.gson.JsonObject result = com.ogatamizuki.economy.backend.local.EconomyLocalAdminService.giveSpawnEgg(
                    serverPlayer, payload.shopId());
            sendAdminJsonResult(serverPlayer, result);
            return;
        }

        if ("GIVE_ALL_SPAWN_EGGS".equalsIgnoreCase(payload.action())) {
            com.google.gson.JsonObject result = com.ogatamizuki.economy.backend.local.EconomyLocalAdminService.giveAllSpawnEggs(serverPlayer);
            sendAdminJsonResult(serverPlayer, result);
            return;
        }

        if (!"RESET".equalsIgnoreCase(payload.action())) {
            PacketDistributor.sendToPlayer(
                    serverPlayer,
                    new EconomyAdminResultPayload(false, "§c[経済] 不明な操作です: " + payload.action())
            );
            return;
        }

        var options = new com.ogatamizuki.economy.backend.local.EconomyLocalAdminService.ResetOptions(
                payload.resetBalances(),
                payload.resetRankingMetrics(),
                payload.resetPortfolios(),
                payload.resetShopLimits(),
                payload.resetFleaMarket(),
                payload.resetRankingSnapshots(),
                payload.resetEtfPrices(),
                payload.resetPlayTime(),
                payload.resetTravelDistance(),
                payload.resetBlocksBroken(),
                payload.resetDeaths(),
                payload.resetPlayerKills(),
                payload.resetMobKills(),
                payload.resetHarvests(),
                payload.resetPotionsBrewed(),
                payload.resetFishCaught()
        );
        com.google.gson.JsonObject result = com.ogatamizuki.economy.backend.local.EconomyLocalAdminService.reset(options);
        if (result.has("success") && result.get("success").getAsBoolean()) {
            if (options.resetBalances()) {
                var data = com.ogatamizuki.economy.data.EconomyWorldSavedData.get(serverPlayer.level().getServer());
                for (ServerPlayer online : serverPlayer.level().getServer().getPlayerList().getPlayers()) {
                    var record = data.getOrCreate(online.getUUID(), online.getName().getString());
                    EconomyBalanceSync.applyBalanceAndSync(online, record.balance(), record.bankBalance(), record.debt());
                }
            }
            int updated = result.has("playersUpdated") ? result.get("playersUpdated").getAsInt() : 0;
            PacketDistributor.sendToPlayer(
                    serverPlayer,
                    new EconomyAdminResultPayload(true, "§a[経済] リセットが完了しました。（プレイヤー " + updated + " 件更新）")
            );
        } else {
            String error = result.has("error") ? result.get("error").getAsString() : "リセットに失敗しました。";
            PacketDistributor.sendToPlayer(
                    serverPlayer,
                    new EconomyAdminResultPayload(false, "§c[経済] " + error)
            );
        }
    }

    private static void sendAdminJsonResult(ServerPlayer serverPlayer, com.google.gson.JsonObject result) {
        if (result.has("success") && result.get("success").getAsBoolean()) {
            String msg = result.has("message") ? result.get("message").getAsString() : "操作が完了しました。";
            PacketDistributor.sendToPlayer(serverPlayer, new EconomyAdminResultPayload(true, "§a[経済] " + msg));
        } else {
            String error = result.has("error") ? result.get("error").getAsString() : "操作に失敗しました。";
            PacketDistributor.sendToPlayer(serverPlayer, new EconomyAdminResultPayload(false, "§c[経済] " + error));
        }
    }

    private void handleEconomyQueryRequest(EconomyQueryRequestPayload payload, net.neoforged.neoforge.network.handling.IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer serverPlayer)) {
            return;
        }
        if (isAdminEconomyQuery(payload.queryType())
                && !EconomyAdminAuth.canPerformAdminActions(serverPlayer)) {
            PacketDistributor.sendToPlayer(
                    serverPlayer,
                    new EconomyQueryResponsePayload(payload.queryType(), payload.arg1(), payload.arg2(), "null")
            );
            return;
        }
        String json = switch (payload.queryType()) {
            case "STOCKS" -> com.ogatamizuki.economy.backend.local.EconomyLocalEtfService.fetchStocks(serverPlayer.getUUID());
            case "STOCK_HISTORY" -> com.ogatamizuki.economy.backend.local.EconomyLocalEtfService.fetchHistory(payload.arg1(), payload.arg2());
            case "STOCK_COMPONENTS" -> com.ogatamizuki.economy.backend.local.EconomyLocalEtfService.fetchComponents(payload.arg1());
            case "FLEA_LISTINGS" -> com.ogatamizuki.economy.backend.local.EconomyLocalFleaMarketService.fetchListings();
            case "RANKING_LATEST" -> com.ogatamizuki.economy.backend.local.EconomyLocalRankingService.fetchLatest();
            case "LOAN_LIMIT" -> com.ogatamizuki.economy.backend.local.EconomyLocalLoanService.fetchLimit(serverPlayer.getUUID()).toString();
            case "PLAYER_BALANCES" -> com.ogatamizuki.economy.backend.local.EconomyLocalAdminService.fetchPlayerBalances();
            case "MASTER_CONFIG" -> com.ogatamizuki.economy.backend.local.EconomyLocalAdminService.fetchMasterConfig(serverPlayer.level().getServer());
            case "MASTER_REWARDS" -> com.ogatamizuki.economy.backend.local.EconomyLocalAdminService.fetchMasterRewards(serverPlayer.level().getServer());
            case "MASTER_ITEMS" -> com.ogatamizuki.economy.backend.local.EconomyLocalAdminService.fetchMasterItems(
                    serverPlayer.level().getServer(), payload.arg2());
            case "MASTER_SHOPS" -> com.ogatamizuki.economy.backend.local.EconomyLocalAdminService.fetchMasterShops(serverPlayer.level().getServer());
            case "MASTER_SHOP_ITEMS" -> com.ogatamizuki.economy.backend.local.EconomyLocalAdminService.fetchMasterShopItems(serverPlayer.level().getServer());
            case "MASTER_ETF_ITEMS" -> com.ogatamizuki.economy.backend.local.EconomyLocalAdminService.fetchMasterEtfItems(serverPlayer.level().getServer());
            default -> null;
        };
        PacketDistributor.sendToPlayer(
                serverPlayer,
                new EconomyQueryResponsePayload(payload.queryType(), payload.arg1(), payload.arg2(), json != null ? json : "null")
        );
    }

    private void handleBankRequest(BankRequestPayload payload, net.neoforged.neoforge.network.handling.IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer serverPlayer)) {
            return;
        }
        UUID playerUuid = serverPlayer.getUUID();
        boolean success;
        if ("DEPOSIT".equalsIgnoreCase(payload.action())) {
            success = com.ogatamizuki.economy.backend.local.EconomyLocalPlayerService.deposit(playerUuid, payload.amount());
        } else if ("WITHDRAW".equalsIgnoreCase(payload.action())) {
            success = com.ogatamizuki.economy.backend.local.EconomyLocalPlayerService.withdraw(playerUuid, payload.amount());
        } else {
            success = false;
        }

        var data = com.ogatamizuki.economy.data.EconomyWorldSavedData.get(serverPlayer.level().getServer());
        var record = data.getOrCreate(playerUuid, serverPlayer.getName().getString());
        if (success) {
            EconomyBalanceSync.applyBalanceAndSync(serverPlayer, record.balance(), record.bankBalance(), record.debt());
        }
        PacketDistributor.sendToPlayer(
                serverPlayer,
                new BankResultPayload(success, record.balance(), record.bankBalance(), record.debt())
        );
    }

    private void handleShopDetailsRequest(ShopDetailsRequestPayload payload, net.neoforged.neoforge.network.handling.IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer serverPlayer)) {
            return;
        }
        List<String> chunks = com.ogatamizuki.economy.backend.local.EconomyLocalShopService.fetchShopDetailsChunks(
                payload.shopId(), serverPlayer.getUUID());
        if (chunks.isEmpty()) {
            PacketDistributor.sendToPlayer(
                    serverPlayer,
                    new ShopDetailsResponsePayload(payload.shopId(), 0, 1, "{}")
            );
            return;
        }
        int totalChunks = chunks.size();
        for (int i = 0; i < totalChunks; i++) {
            PacketDistributor.sendToPlayer(
                    serverPlayer,
                    new ShopDetailsResponsePayload(payload.shopId(), i, totalChunks, chunks.get(i))
            );
        }
    }

    private void handleLoanRequest(LoanRequestPayload payload, net.neoforged.neoforge.network.handling.IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer serverPlayer)) return;
        String uuid = serverPlayer.getUUID().toString();
        String action = payload.action();
        int amount = payload.amount();

        if ("BORROW".equalsIgnoreCase(action)) {
            EconomyService.borrowLoan(uuid, amount).thenAccept(res ->
                    runOnServerThread(() -> {
                        if (res != null && res.has("success") && res.get("success").getAsBoolean()) {
                            int newBalance = res.has("newBalance") ? res.get("newBalance").getAsInt() : -1;
                            int newDebt = res.has("newDebt") ? res.get("newDebt").getAsInt() : -1;
                            String msg = res.has("message") ? res.get("message").getAsString() : "借入に成功しました。";
                            notifyLoanTxResult(serverPlayer, true, newBalance, newDebt, "§a[借金] §f" + msg);
                        } else {
                            String error = (res != null && res.has("error")) ? res.get("error").getAsString() : "借入に失敗しました。";
                            notifyLoanTxResult(serverPlayer, false, -1, -1, "§c[エラー] " + error);
                        }
                    })
            );
        } else if ("REPAY".equalsIgnoreCase(action)) {
            EconomyService.repayLoan(uuid, amount).thenAccept(res ->
                    runOnServerThread(() -> {
                        if (res != null && res.has("success") && res.get("success").getAsBoolean()) {
                            int newBalance = res.has("newBalance") ? res.get("newBalance").getAsInt() : -1;
                            int newDebt = res.has("newDebt") ? res.get("newDebt").getAsInt() : -1;
                            String msg = res.has("message") ? res.get("message").getAsString() : "返済に成功しました。";
                            notifyLoanTxResult(serverPlayer, true, newBalance, newDebt, "§a[借金] §f" + msg);
                        } else {
                            String error = (res != null && res.has("error")) ? res.get("error").getAsString() : "返済に失敗しました。";
                            notifyLoanTxResult(serverPlayer, false, -1, -1, "§c[エラー] " + error);
                        }
                    })
            );
        }
    }

    private static void notifyLoanTxResult(
            ServerPlayer serverPlayer,
            boolean success,
            int newBalance,
            int newDebt,
            String message
    ) {
        PacketDistributor.sendToPlayer(
                serverPlayer,
                new LoanTxResultPayload(success, newBalance, newDebt, message)
        );
    }

    private static int countInventoryItems(Player player, Identifier itemId) {
        int count = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.isEmpty() && BuiltInRegistries.ITEM.getKey(stack.getItem()).equals(itemId)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    private static void runOnServerThread(Runnable action) {
        var mcServer = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
        if (mcServer != null) {
            mcServer.execute(action);
        } else {
            LOGGER.warn("MinecraftServer not available; running shop tx task on current thread.");
            action.run();
        }
    }

    private static void notifyShopTxResult(
            ServerPlayer serverPlayer,
            boolean success,
            int newBalance,
            String message,
            String itemKey,
            int remainingItemCount
    ) {
        PacketDistributor.sendToPlayer(
                serverPlayer,
                ShopTxResultPayload.simple(success, newBalance, message, itemKey, remainingItemCount)
        );
    }

    private static void notifyShopTxResult(
            ServerPlayer serverPlayer,
            boolean success,
            int newBalance,
            String message,
            String itemKey,
            String matchPotion,
            String matchEnchantment,
            Integer matchEnchantmentLevel,
            int remainingItemCount
    ) {
        PacketDistributor.sendToPlayer(
                serverPlayer,
                ShopTxResultPayload.withMatch(
                        success,
                        newBalance,
                        message,
                        itemKey,
                        matchPotion,
                        matchEnchantment,
                        matchEnchantmentLevel,
                        remainingItemCount
                )
        );
    }

    /**
     * サーバー側: 購入リクエスト処理。ローカルショップサービスで検証し、成功したらアイテムを付与する。
     */
    private void handleBuyRequest(ShopBuyRequestPayload payload, net.neoforged.neoforge.network.handling.IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer serverPlayer)) return;
        String uuid = serverPlayer.getUUID().toString();
        LOGGER.info("Shop buy request: player={} shopItemId={} qty={}",
                serverPlayer.getName().getString(), payload.shopItemId(), payload.quantity());

        EconomyService.buyShopItem(uuid, payload.shopItemId(), payload.quantity()).thenAccept(res ->
                runOnServerThread(() -> {
                    if (res != null && res.has("success") && res.get("success").getAsBoolean()) {
                        int newBalance = res.has("newBalance") ? res.get("newBalance").getAsInt() : -1;
                        String itemKey = res.has("itemKey") ? res.get("itemKey").getAsString() : null;
                        int quantity = payload.quantity();
                        String itemName = res.has("itemName") ? res.get("itemName").getAsString() : "アイテム";

                        String syncItemKey = "";
                        int remainingItemCount = -1;
                        if (itemKey != null) {
                            try {
                                Identifier itemId = Identifier.parse(itemKey);
                                Item item = BuiltInRegistries.ITEM.get(itemId)
                                        .map(Holder::value)
                                        .orElse(Items.AIR);
                                if (item != Items.AIR) {
                                    int remaining = quantity;
                                    while (remaining > 0) {
                                        int stackSize = Math.min(remaining, item.getDefaultMaxStackSize());
                                        ItemStack stack = new ItemStack(item, stackSize);
                                        serverPlayer.getInventory().add(stack);
                                        remaining -= stackSize;
                                    }
                                    serverPlayer.inventoryMenu.broadcastChanges();
                                    syncItemKey = itemKey;
                                    remainingItemCount = countInventoryItems(serverPlayer, itemId);
                                    LOGGER.info("Gave {} x{} to player {}", itemKey, quantity, serverPlayer.getName().getString());
                                } else {
                                    LOGGER.warn("Item not found in registry for buy grant: {}", itemKey);
                                }
                            } catch (Exception e) {
                                LOGGER.error("Failed to grant item on buy: ", e);
                            }
                        }
                        String msg = "§a[ショップ] §e" + itemName + "§f を " + quantity + " 個購入しました！";
                        notifyShopTxResult(serverPlayer, true, newBalance, msg, syncItemKey, remainingItemCount);
                    } else {
                        String error = (res != null && res.has("error")) ? res.get("error").getAsString() : "購入に失敗しました。";
                        LOGGER.warn("Shop buy rejected: player={} shopItemId={} qty={} reason={}",
                                serverPlayer.getName().getString(), payload.shopItemId(), payload.quantity(), error);
                        notifyShopTxResult(serverPlayer, false, -1, "§c[エラー] " + error, "", -1);
                    }
                })
        ).exceptionally(ex -> {
            LOGGER.error("Shop buy request failed for {}: ", serverPlayer.getName().getString(), ex);
            runOnServerThread(() -> notifyShopTxResult(
                    serverPlayer, false, -1, "§c[エラー] 購入処理中にエラーが発生しました。", "", -1));
            return null;
        });
    }

    /**
     * サーバー側: 売却リクエスト処理。インベントリ確認後、ローカルショップサービスで処理し、成功したらアイテムを削除する。
     */
    private void handleSellRequest(ShopSellRequestPayload payload, net.neoforged.neoforge.network.handling.IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer serverPlayer)) return;
        String uuid = serverPlayer.getUUID().toString();
        int itemId = payload.itemId();
        int quantity = payload.quantity();

        var itemOpt = com.ogatamizuki.economy.master.EconomyMasterData.get().item(itemId);
        if (itemOpt.isEmpty()) {
            notifyShopTxResult(
                    serverPlayer,
                    false,
                    -1,
                    "§c[エラー] 売却対象のアイテムが見つかりません。",
                    "",
                    -1
            );
            return;
        }
        var itemDef = itemOpt.get();

        // サーバー側でインベントリチェック（クリエイティブ以外）
        if (!serverPlayer.isCreative()) {
            int count = EconomyItemMatcher.countMatching(serverPlayer, itemDef);
            if (count < quantity) {
                notifyShopTxResult(
                        serverPlayer,
                        false,
                        -1,
                        "§c[エラー] 売却に必要なアイテムがインベントリにありません。",
                        "",
                        -1
                );
                return;
            }
        }

        EconomyService.sellShopItem(uuid, itemId, quantity).thenAccept(res ->
                runOnServerThread(() -> {
                    if (res != null && res.has("success") && res.get("success").getAsBoolean()) {
                        int newBalance = res.has("newBalance") ? res.get("newBalance").getAsInt() : -1;
                        int totalGain = res.has("totalGain") ? res.get("totalGain").getAsInt() : 0;
                        String itemName = res.has("itemName") ? res.get("itemName").getAsString() : "アイテム";

                        if (!serverPlayer.isCreative()) {
                            EconomyItemMatcher.removeMatching(serverPlayer, itemDef, quantity);
                            serverPlayer.inventoryMenu.broadcastChanges();
                            LOGGER.info("Removed {} x{} (id={}) from player {} inventory",
                                    itemDef.itemKey(), quantity, itemId, serverPlayer.getName().getString());
                        }

                        int remainingItemCount = EconomyItemMatcher.countMatching(serverPlayer, itemDef);
                        java.text.NumberFormat fmt = java.text.NumberFormat.getNumberInstance(java.util.Locale.JAPAN);
                        String msg = "§a[ショップ] §e" + itemName + "§f を " + quantity + " 個売却し、§e¥" + fmt.format(totalGain) + "§f を獲得しました！";
                        notifyShopTxResult(
                                serverPlayer,
                                true,
                                newBalance,
                                msg,
                                itemDef.itemKey(),
                                itemDef.matchPotion(),
                                itemDef.matchEnchantment(),
                                itemDef.matchEnchantmentLevel(),
                                remainingItemCount
                        );
                    } else {
                        String error = (res != null && res.has("error")) ? res.get("error").getAsString() : "売却に失敗しました。";
                        notifyShopTxResult(serverPlayer, false, -1, "§c[エラー] " + error, "", -1);
                    }
                })
        ).exceptionally(ex -> {
            LOGGER.error("Shop sell request failed for {}: ", serverPlayer.getName().getString(), ex);
            runOnServerThread(() -> notifyShopTxResult(
                    serverPlayer, false, -1, "§c[エラー] 売却処理中にエラーが発生しました。", "", -1));
            return null;
        });
    }

    /**
     * サーバー側: ETF取引リクエスト処理。
     */
    private void handleStockTradeRequest(StockTradeRequestPayload payload, net.neoforged.neoforge.network.handling.IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer serverPlayer)) return;
        String uuid = serverPlayer.getUUID().toString();

        EconomyService.tradeStock(uuid, payload.stockCategoryId(), payload.tradeType(), payload.quantity())
                .thenAccept(res -> runOnServerThread(() -> {
                    if (res != null && res.has("success") && res.get("success").getAsBoolean()) {
                        int newBalance = res.has("newBalance") ? res.get("newBalance").getAsInt() : -1;
                        int currentPrice = res.has("currentPrice") ? res.get("currentPrice").getAsInt() : 0;
                        int portfolioQuantity = res.has("portfolioQuantity") ? res.get("portfolioQuantity").getAsInt() : 0;

                        java.text.NumberFormat fmt = java.text.NumberFormat.getNumberInstance(java.util.Locale.JAPAN);
                        String msg = "§a[ETF] §f取引が完了しました。(残高: §e¥" + fmt.format(newBalance) + "§f)";

                        PacketDistributor.sendToPlayer(
                                serverPlayer,
                                new StockTradeResultPayload(true, newBalance, currentPrice, portfolioQuantity, msg)
                        );
                    } else {
                        String error = (res != null && res.has("error")) ? res.get("error").getAsString() : "取引に失敗しました。";
                        PacketDistributor.sendToPlayer(
                                serverPlayer,
                                new StockTradeResultPayload(false, -1, 0, 0, "§c[エラー] " + error)
                        );
                    }
                }));
    }

    private void handleFleaMarketListRequest(FleaMarketListRequestPayload payload, net.neoforged.neoforge.network.handling.IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer serverPlayer)) return;
        String uuid = serverPlayer.getUUID().toString();
        int price = payload.price();
        int quantity = payload.quantity();
        ItemStack requested = com.ogatamizuki.economy.data.FleaMarketStackCodec.decode(
                serverPlayer.registryAccess(),
                payload.itemStackSnbt(),
                payload.itemKey(),
                1);
        if (requested.isEmpty() || price <= 0 || quantity <= 0) {
            PacketDistributor.sendToPlayer(serverPlayer, new FleaMarketResultPayload(false, "§c[フリマ] §f出品内容が無効です。", -1));
            return;
        }

        Identifier itemId = BuiltInRegistries.ITEM.getKey(requested.getItem());
        String itemKey = itemId.toString();
        ItemStack template = requested.copyWithCount(1);

        // 1. サーバー側で同一 components の個数を確認し、保存用テンプレートもサーバー在庫から取る
        ItemStack storedTemplate = ItemStack.EMPTY;
        int available = 0;
        if (!serverPlayer.isCreative()) {
            for (int i = 0; i < serverPlayer.getInventory().getContainerSize(); i++) {
                ItemStack stack = serverPlayer.getInventory().getItem(i);
                if (!stack.isEmpty() && ItemStack.isSameItemSameComponents(stack, template)) {
                    available += stack.getCount();
                    if (storedTemplate.isEmpty()) {
                        storedTemplate = stack.copyWithCount(1);
                    }
                }
            }
            if (available < quantity) {
                PacketDistributor.sendToPlayer(serverPlayer, new FleaMarketResultPayload(false, "§c[フリマ] §f出品に必要なアイテムがインベントリにありません。", -1));
                return;
            }
        } else {
            storedTemplate = template;
        }
        if (storedTemplate.isEmpty()) {
            storedTemplate = template;
        }

        // 2. クライアント側の表示名を使用（Dedicated Server では getName() が英語になるため）
        final String resolvedItemName;
        String clientItemName = payload.itemName();
        if (clientItemName == null || clientItemName.isBlank()) {
            resolvedItemName = storedTemplate.getHoverName().getString();
        } else {
            resolvedItemName = clientItemName;
        }

        final ItemStack listingStack = storedTemplate.copyWithCount(1);

        // 3. API申請
        EconomyService.listFleaMarketItem(uuid, itemKey, resolvedItemName, price, quantity, listingStack).thenAccept(res ->
                runOnServerThread(() -> {
                    if (res != null && res.has("success") && res.get("success").getAsBoolean()) {
                        // 成功したら同一 components のアイテムをインベントリから回収
                        if (!serverPlayer.isCreative()) {
                            int remaining = quantity;
                            for (int i = 0; i < serverPlayer.getInventory().getContainerSize() && remaining > 0; i++) {
                                ItemStack stack = serverPlayer.getInventory().getItem(i);
                                if (!stack.isEmpty() && ItemStack.isSameItemSameComponents(stack, listingStack)) {
                                    if (stack.getCount() <= remaining) {
                                        remaining -= stack.getCount();
                                        serverPlayer.getInventory().setItem(i, ItemStack.EMPTY);
                                    } else {
                                        stack.shrink(remaining);
                                        remaining = 0;
                                    }
                                }
                            }
                            serverPlayer.inventoryMenu.broadcastChanges();
                        }
                        String msg = "§a[フリマ] §e" + resolvedItemName + "§f を " + quantity + " 個（単価: ¥" + price + "）出品しました。";
                        PacketDistributor.sendToPlayer(serverPlayer, new FleaMarketResultPayload(true, msg, -1));
                    } else {
                        String error = (res != null && res.has("error")) ? res.get("error").getAsString() : "出品に失敗しました。";
                        PacketDistributor.sendToPlayer(serverPlayer, new FleaMarketResultPayload(false, "§c[エラー] " + error, -1));
                    }
                })
        ).exceptionally(ex -> {
            LOGGER.error("Flea market list request failed for {}: ", serverPlayer.getName().getString(), ex);
            runOnServerThread(() -> PacketDistributor.sendToPlayer(
                    serverPlayer, new FleaMarketResultPayload(false, "§c[エラー] 出品処理中にエラーが発生しました。", -1)));
            return null;
        });
    }

    private void handleFleaMarketBuyRequest(FleaMarketBuyRequestPayload payload, net.neoforged.neoforge.network.handling.IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer serverPlayer)) return;
        String uuid = serverPlayer.getUUID().toString();

        EconomyService.buyFleaMarketItem(uuid, payload.listingId(), payload.quantity()).thenAccept(res ->
                runOnServerThread(() -> {
                    if (res != null && res.has("success") && res.get("success").getAsBoolean()) {
                        int newBalance = res.has("newBalance") ? res.get("newBalance").getAsInt() : -1;
                        String itemKey = res.has("itemKey") ? res.get("itemKey").getAsString() : null;
                        int quantity = payload.quantity();
                        String itemName = res.has("itemName") ? res.get("itemName").getAsString() : "アイテム";
                        String stackNbt = res.has("itemStackNbt") ? res.get("itemStackNbt").getAsString() : "";

                        grantFleaMarketStacks(serverPlayer, stackNbt, itemKey, quantity);

                        Component displayName = EconomyItemDisplayNames.resolve(
                                serverPlayer.registryAccess(), stackNbt, itemKey, itemName);
                        serverPlayer.sendSystemMessage(Component.literal("§a[フリマ] §e")
                                .append(displayName)
                                .append(Component.literal("§f を " + quantity + " 個購入しました！")));
                        PacketDistributor.sendToPlayer(serverPlayer, new FleaMarketResultPayload(true, "", newBalance));
                    } else {
                        String error = (res != null && res.has("error")) ? res.get("error").getAsString() : "購入に失敗しました。";
                        PacketDistributor.sendToPlayer(serverPlayer, new FleaMarketResultPayload(false, "§c[エラー] " + error, -1));
                    }
                })
        );
    }

    private void handleFleaMarketCancelRequest(FleaMarketCancelRequestPayload payload, net.neoforged.neoforge.network.handling.IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer serverPlayer)) return;
        String uuid = serverPlayer.getUUID().toString();

        EconomyService.cancelFleaMarketListing(uuid, payload.listingId()).thenAccept(res ->
                runOnServerThread(() -> {
                    if (res != null && res.has("success") && res.get("success").getAsBoolean()) {
                        String itemKey = res.has("itemKey") ? res.get("itemKey").getAsString() : null;
                        int remainingQuantity = res.has("remainingQuantity") ? res.get("remainingQuantity").getAsInt() : 0;
                        String itemName = res.has("itemName") ? res.get("itemName").getAsString() : "アイテム";
                        String stackNbt = res.has("itemStackNbt") ? res.get("itemStackNbt").getAsString() : "";

                        if (remainingQuantity > 0) {
                            grantFleaMarketStacks(serverPlayer, stackNbt, itemKey, remainingQuantity);
                        }

                        Component displayName = EconomyItemDisplayNames.resolve(
                                serverPlayer.registryAccess(), stackNbt, itemKey, itemName);
                        serverPlayer.sendSystemMessage(Component.literal("§a[フリマ] 出品を取り消し、売れ残りの §e")
                                .append(displayName)
                                .append(Component.literal("§f を " + remainingQuantity + " 個回収しました。")));
                        PacketDistributor.sendToPlayer(serverPlayer, new FleaMarketResultPayload(true, "", -1));
                    } else {
                        String error = (res != null && res.has("error")) ? res.get("error").getAsString() : "出品取消に失敗しました。";
                        PacketDistributor.sendToPlayer(serverPlayer, new FleaMarketResultPayload(false, "§c[エラー] " + error, -1));
                    }
                })
        );
    }

    private void grantFleaMarketStacks(ServerPlayer serverPlayer, String stackNbt, String itemKey, int quantity) {
        if (quantity <= 0) {
            return;
        }
        try {
            ItemStack template = com.ogatamizuki.economy.data.FleaMarketStackCodec.decode(
                    serverPlayer.registryAccess(),
                    stackNbt,
                    itemKey,
                    1
            );
            if (template.isEmpty()) {
                return;
            }
            int remaining = quantity;
            int maxStack = Math.max(1, template.getMaxStackSize());
            while (remaining > 0) {
                int stackSize = Math.min(remaining, maxStack);
                serverPlayer.getInventory().add(template.copyWithCount(stackSize));
                remaining -= stackSize;
            }
            serverPlayer.inventoryMenu.broadcastChanges();
        } catch (Exception e) {
            LOGGER.error("Failed to grant flea market item stacks: ", e);
        }
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        LOGGER.info("Economy standalone mod common setup");
    }



    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        minecraftServer = event.getServer();
        EconomyMasterData.reload(event.getServer());
        EconomyEtfPriceScheduler.start(event.getServer());
        LOGGER.info("Economy LOCAL server starting (master data loaded)");
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        EconomyEtfPriceScheduler.stop();
        minecraftServer = null;
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        LOGGER.info("Registering Economy commands");
        event.getDispatcher().register(
            Commands.literal("economy")
                .then(Commands.literal("spawn_egg")
                    .requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
                    .then(Commands.argument("shop_id", IntegerArgumentType.integer(1))
                        .then(Commands.argument("npc_type", StringArgumentType.word())
                            .executes(context -> {
                                int shopId = IntegerArgumentType.getInteger(context, "shop_id");
                                String npcType = StringArgumentType.getString(context, "npc_type");
                                return giveSpawnEgg(context.getSource(), shopId, npcType);
                            })
                        )
                        .executes(context -> {
                            int shopId = IntegerArgumentType.getInteger(context, "shop_id");
                            return giveSpawnEgg(context.getSource(), shopId, null);
                        })
                    )
                )
                .then(Commands.literal("ranking")
                    .then(Commands.literal("compile")
                        .requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
                        .executes(context -> {
                            compileRanking(context.getSource());
                            return 1;
                        })
                    )
                    .then(Commands.literal("view")
                        .executes(context -> {
                            viewRanking(context.getSource(), null);
                            return 1;
                        })
                        .then(Commands.argument("metric", StringArgumentType.word())
                            .suggests((context, builder) -> net.minecraft.commands.SharedSuggestionProvider.suggest(new String[]{
                                "total", "総資産", "balance", "手持ち", "bank", "銀行", "earnings", "獲得額",
                                "lost", "ロスト", "debt", "借金", "time", "参加時間", "distance", "移動距離", "broken", "ブロック破壊",
                                "deaths", "死亡", "kills", "モブキル", "player_kills", "プレイヤーキル", "harvest", "収穫",
                                "potion", "ポーション", "fish", "釣り", "etf_buy", "etf購入", "etf_short", "etf空売り",
                                "etf_profit", "etf利益", "etf_trades", "etf取引数"
                            }, builder))
                            .executes(context -> {
                                viewRanking(context.getSource(), StringArgumentType.getString(context, "metric"));
                                return 1;
                            })
                        )
                    )
                )
        );
    }

    private int giveSpawnEgg(CommandSourceStack source, int shopId, String npcType) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        
        if (npcType == null) {
            // Asynchronously fetch shop details to get the npc_type and npc_model
            EconomyService.fetchShopDetails(shopId, player.getUUID().toString()).thenAccept(res -> {
                String resolvedType = "SELLER";
                String resolvedModel = "minecraft:villager";
                String resolvedName = "経済NPC";
                if (res != null) {
                    try {
                        com.google.gson.JsonObject json = com.google.gson.JsonParser.parseString(res).getAsJsonObject();
                        if (json.has("npcType")) {
                            resolvedType = json.get("npcType").getAsString();
                        }
                        if (json.has("npcModel") && !json.get("npcModel").isJsonNull()) {
                            resolvedModel = json.get("npcModel").getAsString();
                        }
                        if (json.has("shopName")) {
                            resolvedName = json.get("shopName").getAsString();
                        }
                    } catch (Exception e) {
                        LOGGER.error("Failed to parse shop details: ", e);
                    }
                }
                
                final String finalType = resolvedType;
                final String finalModel = resolvedModel;
                final String finalName = resolvedName;
                var mcServer = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
                if (mcServer != null) {
                    mcServer.execute(() -> {
                        if ("LOAN".equalsIgnoreCase(finalType)) {
                            EconomyNpcSpawnService.giveLoanSpawnEgg(player, shopId, finalName);
                        } else {
                            EconomyNpcSpawnService.giveConfiguredSpawnEgg(player, shopId, finalType, finalModel, finalName);
                        }
                        source.sendSuccess(() -> Component.literal(
                                "§aNPCスポナーエッグ (ID: " + shopId + ", タイプ: " + finalType.toUpperCase() + ") を付与しました。"), true);
                    });
                }
            });
            source.sendSuccess(() -> Component.literal("§eショップ情報をサーバーに問い合わせています..."), true);
            return 1;
        } else {
            String defaultName = "経済NPC";
            if ("BUYER".equalsIgnoreCase(npcType)) defaultName = "買取所";
            else if ("STOCK_TRADER".equalsIgnoreCase(npcType)) defaultName = "取引市場";
            else if ("FLEA_MARKET".equalsIgnoreCase(npcType)) defaultName = "フリーマーケット";
            else if ("LOAN".equalsIgnoreCase(npcType)) defaultName = "闇金融";
            if ("LOAN".equalsIgnoreCase(npcType)) {
                EconomyNpcSpawnService.giveLoanSpawnEgg(player, shopId, defaultName);
                source.sendSuccess(() -> Component.literal("§a融資NPCスポナーエッグ (ID: " + shopId + ") を付与しました。"), true);
            } else {
                EconomyNpcSpawnService.giveConfiguredSpawnEgg(player, shopId, npcType, "minecraft:villager", defaultName);
                source.sendSuccess(() -> Component.literal("§aNPCスポナーエッグ (ID: " + shopId + ", タイプ: " + npcType.toUpperCase() + ") を付与しました。"), true);
            }
            return 1;
        }
    }

    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        Player player = event.getEntity();
        String uuid = player.getUUID().toString();
        String username = EconomyNicknameBridge.resolvePlayerName(player);
        UUID playerUuid = player.getUUID();
        LOGGER.info("Player logged in: {} ({}) - resetting economy ready status and triggering join sync", username, uuid);
        setEconomyReady(playerUuid, false);
        if (player instanceof ServerPlayer serverPlayer) {
            EconomyFeatures.syncToPlayer(serverPlayer);
            EconomyService.joinPlayer(uuid, username, serverPlayer);
        }
    }

    @SubscribeEvent
    public void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        Player player = event.getEntity();
        String uuid = player.getUUID().toString();
        String username = EconomyNicknameBridge.resolvePlayerName(player);
        UUID playerUuid = player.getUUID();
        LOGGER.info("Player logged out: {} ({}) - clearing economy ready status and triggering leave", username, uuid);
        RewardChatAggregator.flushPlayer(playerUuid);
        setEconomyReady(playerUuid, false);
        EconomyService.leavePlayer(uuid, username);
    }

    @SubscribeEvent
    public void onLivingDamage(LivingDamageEvent.Post event) {
        if (event.getEntity().level().isClientSide()) return;
        
        UUID mobUuid = event.getEntity().getUUID();
        float amount = event.getOriginalDamage();
        DamageSource source = event.getSource();
        
        UUID attackerUuid;
        String attackerName;
        
        if (source.getEntity() instanceof Player player) {
            attackerUuid = player.getUUID();
            attackerName = player.getName().getString();
        } else {
            attackerUuid = ENVIRONMENT_UUID;
            attackerName = "ENVIRONMENT (" + source.type().msgId() + ")";
        }

        damageTracker.compute(mobUuid, (k, playerDamageMap) -> {
            if (playerDamageMap == null) {
                playerDamageMap = new ConcurrentHashMap<>();
            }
            playerDamageMap.merge(attackerUuid, amount, (oldVal, newVal) -> oldVal + newVal);
            return playerDamageMap;
        });
        
        LOGGER.info("Recorded damage from {} to mob {}: {} (Total for this source: {})", 
                attackerName, event.getEntity().getType().toString(), amount, damageTracker.get(mobUuid).get(attackerUuid));
    }

    @SubscribeEvent
    public void onLivingDeath(LivingDeathEvent event) {
        if (event.getEntity().level().isClientSide()) return;

        // プレイヤー自身の死亡を検知
        if (event.getEntity() instanceof Player player) {
            if (!isEconomyReady(player.getUUID())) return;
            LOGGER.info("Player {} has died - triggering death penalty", player.getName().getString());
            if (!player.isCreative()) {
                EconomyService.deathPlayer(player);
            } else {
                LOGGER.info("Skipping death penalty for player {} (Creative Mode)", player.getName().getString());
            }
            return;
        }

        UUID mobUuid = event.getEntity().getUUID();
        Map<UUID, Float> playerDamageMap = damageTracker.remove(mobUuid);

        String entityName = BuiltInRegistries.ENTITY_TYPE.getKey(event.getEntity().getType()).getPath().toUpperCase();
        String actionType = "KILL_" + entityName;

        LOGGER.info("onLivingDeath event fired for entity: {} ({})", event.getEntity().getType().toString(), mobUuid);

        if (playerDamageMap != null && !playerDamageMap.isEmpty()) {
            float totalDamage = 0f;
            for (float d : playerDamageMap.values()) {
                totalDamage += d;
            }

            if (totalDamage > 0) {
                LOGGER.info("Distributing rewards for {} based on damage ratios. Total damage: {}", entityName, totalDamage);
                for (Map.Entry<UUID, Float> entry : playerDamageMap.entrySet()) {
                    UUID playerUuid = entry.getKey();
                    if (playerUuid.equals(ENVIRONMENT_UUID)) {
                        // 環境ダメージ分はスキップ（報酬を発生させない）
                        continue;
                    }
                    float damage = entry.getValue();
                    double ratio = damage / totalDamage;

                    Player player = event.getEntity().level().getPlayerByUUID(playerUuid);
                    if (player != null) {
                        if (!isEconomyReady(playerUuid)) {
                            continue;
                        }
                        LOGGER.info("Player {} dealt {} damage (Ratio: {})", player.getName().getString(), damage, ratio);
                        if (player.isCreative()) {
                            LOGGER.info("Skipping reward for player {} (Creative Mode)", player.getName().getString());
                            continue;
                        }
                        EconomyService.rewardPlayer(player, actionType, ratio);
                    }
                }
                return;
            }
        }

        // フォールバック: ダメージ記録がない場合は、最後にとどめを刺したプレイヤーに 100% の報酬
        DamageSource source = event.getSource();
        if (source.getEntity() instanceof Player player) {
            if (!isEconomyReady(player.getUUID())) return;
            LOGGER.info("Fallback reward trigger: Player {} landed the killing blow", player.getName().getString());
            if (player.isCreative()) {
                LOGGER.info("Skipping fallback reward (Creative Mode)");
                return;
            }
            EconomyService.rewardPlayer(player, actionType, 1.0);
        }
    }

    @SubscribeEvent
    public void onBlockBreak(BreakBlockEvent event) {
        if (event.getLevel().isClientSide()) return;
        Player player = event.getPlayer();
        if (player == null || player.isCreative()) return;
        if (!isEconomyReady(player.getUUID())) return;

        BlockState state = event.getState();
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
                var mcServer = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
                boolean isPlayerPlaced = false;
                if (mcServer != null && event.getLevel() instanceof ServerLevel serverLevel) {
                    PlacedBlocksSavedData data = PlacedBlocksSavedData.get(mcServer);
                    if (data.isPlaced(serverLevel.dimension(), event.getPos())) {
                        isPlayerPlaced = true;
                        data.removePlaced(serverLevel.dimension(), event.getPos());
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

    @SubscribeEvent
    public void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (event.getLevel().isClientSide()) return;
        if (!(event.getEntity() instanceof Player player) || player.isCreative()) return;

        BlockState state = event.getPlacedBlock();
        Block block = state.getBlock();
        String blockName = BuiltInRegistries.BLOCK.getKey(block).getPath().toUpperCase();

        if (blockName.equals("MELON") || blockName.equals("PUMPKIN")) {
            var mcServer = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
            if (mcServer != null && event.getLevel() instanceof ServerLevel serverLevel) {
                PlacedBlocksSavedData.get(mcServer).addPlaced(serverLevel.dimension(), event.getPos());
            }
        }
    }

    @SubscribeEvent
    public void onItemFished(ItemFishedEvent event) {
        if (event.getEntity().level().isClientSide()) return;
        Player player = event.getEntity();
        if (player == null || player.isCreative()) return;
        if (!isEconomyReady(player.getUUID())) return;

        boolean isRare = true;
        for (ItemStack drop : event.getDrops()) {
            Item item = drop.getItem();
            if (item == Items.COD || item == Items.SALMON) {
                isRare = false;
            }
        }

        String actionType = isRare ? "FISH_RARE" : "FISH_COMMON";
        EconomyService.rewardPlayer(player, actionType);
    }

    private record EconomyNpcInfo(int shopId, String npcType) {}

    private static Optional<EconomyNpcInfo> parseEconomyNpcInfo(net.minecraft.world.entity.Entity target, boolean allowPersistentData) {
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
                        EconomyMod.LOGGER.error("Failed to parse shopId from tag: {}", tag, e);
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
                            EconomyMod.LOGGER.error("Failed to parse shopId from custom name: {}", nameStr, e);
                        }
                    }
                }
            }
        }

        if (allowPersistentData) {
            CompoundTag persistentData = target.getPersistentData();
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
        return stack.is(ECONOMY_NPC_SPAWN_EGG.get()) || parseShopIdFromEggName(stack).isPresent();
    }

    private static boolean matchesEconomyNpcEgg(ItemStack stack, EconomyNpcInfo npcInfo) {
        if (stack.is(ECONOMY_NPC_SPAWN_EGG.get())) {
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
            return serverPlayer.createCommandSourceStack().permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER);
        }
        return false;
    }

    private boolean tryRemoveNpcWithSpawnEgg(
            net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.EntityInteract event,
            net.minecraft.world.entity.Entity target,
            ItemStack eggStack) {
        Player player = event.getEntity();
        if (!canRemoveNpc(player)) {
            return false;
        }
        if (!(eggStack.getItem() instanceof SpawnEggItem)) {
            return false;
        }

        if (target.getType() == LOAN_NPC.get() && eggStack.is(LOAN_NPC_SPAWN_EGG.get())) {
            return executeNpcRemoval(event, target, eggStack, player, "融資NPC");
        }

        boolean allowPersistentData = !event.getLevel().isClientSide();
        Optional<EconomyNpcInfo> npcInfo = parseEconomyNpcInfo(target, allowPersistentData);
        if (npcInfo.isPresent() && isEconomyNpcSpawnEgg(eggStack) && matchesEconomyNpcEgg(eggStack, npcInfo.get())) {
            return executeNpcRemoval(event, target, eggStack, player,
                    "経済NPC (ID: " + npcInfo.get().shopId() + ", タイプ: " + npcInfo.get().npcType() + ")");
        }

        return false;
    }

    private boolean executeNpcRemoval(
            net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.EntityInteract event,
            net.minecraft.world.entity.Entity target,
            ItemStack eggStack,
            Player player,
            String label) {
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);

        if (!event.getLevel().isClientSide()) {
            target.discard();
            if (!player.isCreative()) {
                eggStack.shrink(1);
            }
            if (player instanceof ServerPlayer serverPlayer) {
                serverPlayer.sendSystemMessage(Component.literal("§a" + label + " を撤去しました。"));
            }
            LOGGER.info("Removed {} via spawn egg by {}", target.getType(), player.getName().getString());
        }
        return true;
    }

    private static boolean isEconomyRelatedNpc(Entity entity) {
        if (entity.getType() == ECONOMY_NPC.get() || entity.getType() == LOAN_NPC.get()) {
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

    @SubscribeEvent
    public void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (event.loadedFromDisk() || event.getLevel().isClientSide()) {
            return;
        }

        Entity entity = event.getEntity();
        if (!isEconomyRelatedNpc(entity)) {
            return;
        }

        Player spawner = findNearestSpawnEggPlayer(event.getLevel(), entity);
        if (spawner != null) {
            facePlayer(entity, spawner);
        }
    }

    @SubscribeEvent
    public void onEntityInteract(net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.EntityInteract event) {
        net.minecraft.world.entity.Entity target = event.getTarget();
        ItemStack heldStack = event.getItemStack();

        if (!heldStack.isEmpty() && tryRemoveNpcWithSpawnEgg(event, target, heldStack)) {
            return;
        }

        Optional<EconomyNpcInfo> npcInfo = parseEconomyNpcInfo(target, !event.getLevel().isClientSide());
        if (npcInfo.isEmpty()) {
            return;
        }

        int shopId = npcInfo.get().shopId();
        String npcType = npcInfo.get().npcType();

        // クライアント側でもキャンセルしてバニラUIオープンを抑制
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);

        if (!event.getLevel().isClientSide() && event.getEntity() instanceof ServerPlayer serverPlayer) {
            // クライアントへショップ画面を開くパケットを送信
            PacketDistributor.sendToPlayer(serverPlayer, new OpenShopScreenPayload(shopId, npcType));
            LOGGER.info("Intercepted interact for shop NPC (Success): shopId={}, npcType={}, target={}", shopId, npcType, target.getType().toString());
        }
    }

    public static void compileRanking(CommandSourceStack source) {
        source.sendSuccess(() -> Component.literal("§e[経済] ランキング集計処理を非同期で開始します..."), true);

        // サーバースレッド上でオンラインプレイヤーの情報を取得＆保存
        var server = source.getServer();
        final java.util.Map<String, String> onlinePlayerNames = new java.util.HashMap<>();
        for (var p : server.getPlayerList().getPlayers()) {
            String uuidStr = p.getUUID().toString();
            String username = EconomyNicknameBridge.resolvePlayerName(p);
            onlinePlayerNames.put(uuidStr, username);
            // サーバースレッドから安全に統計ファイルを保存
            if (p instanceof net.minecraft.server.level.ServerPlayer sp) {
                try {
                    sp.getStats().save();
                    LOGGER.info("Saved stats for player: {} ({})", username, uuidStr);
                } catch (Exception e) {
                    LOGGER.warn("Failed to save stats for online player {}: {}", username, e.getMessage());
                }
            }
        }
        LOGGER.info("Online players to compile: {}", onlinePlayerNames.size());

        // 統計ディレクトリのパスをサーバースレッドで解決しておく
        final java.nio.file.Path statsPath;
        try {
            statsPath = server.getWorldPath(new net.minecraft.world.level.storage.LevelResource("players/stats"))
                .toAbsolutePath().normalize();
            LOGGER.info("Stats directory path (normalized): {}", statsPath);
        } catch (Exception e) {
            source.sendFailure(Component.literal("§c[エラー] 統計ディレクトリのパス取得に失敗しました: " + e.getMessage()));
            return;
        }

        java.util.concurrent.CompletableFuture.runAsync(() -> {
            try {
                java.util.Map<String, com.google.gson.JsonObject> playerStatsMap = new java.util.HashMap<>();

                // 2. 全プレイヤーの統計情報をファイルから読み込み
                // （オンラインプレイヤーのファイルは上記で保存済み）
                try {
                    java.io.File statsDir = statsPath.toFile();
                    LOGGER.info("Stats dir exists: {}, is directory: {}", statsDir.exists(), statsDir.isDirectory());
                    if (statsDir.exists() && statsDir.isDirectory()) {
                        java.io.File[] files = statsDir.listFiles((dir, name) -> name.endsWith(".json"));
                        LOGGER.info("Stats files found: {}", files != null ? files.length : 0);
                        if (files != null) {
                            for (java.io.File file : files) {
                                String filename = file.getName();
                                String uuidStr = filename.substring(0, filename.length() - 5);
                                
                                // UUID形式チェック
                                try {
                                    UUID.fromString(uuidStr);
                                } catch (Exception e) {
                                    continue;
                                }

                                // オンライン / nickname ストレージから表示名を解決
                                String username = EconomyNicknameBridge.resolveUsernameForRanking(uuidStr, onlinePlayerNames);

                                String content = java.nio.file.Files.readString(file.toPath());
                                com.google.gson.JsonObject json = com.google.gson.JsonParser.parseString(content).getAsJsonObject();
                                
                                int playTime = 0;
                                long travelDistanceCm = 0;
                                int blocksBroken = 0;
                                int deaths = 0;
                                int playerKills = 0;
                                int mobKills = 0;
                                int harvests = 0;
                                int potionsBrewed = 0;
                                int fishCaught = 0;

                                if (json.has("stats")) {
                                    com.google.gson.JsonObject statsObj = json.getAsJsonObject("stats");
                                    if (statsObj.has("minecraft:custom")) {
                                        com.google.gson.JsonObject custom = statsObj.getAsJsonObject("minecraft:custom");
                                        if (custom.has("minecraft:play_time")) playTime = custom.get("minecraft:play_time").getAsInt() / 20;
                                        if (custom.has("minecraft:deaths")) deaths = custom.get("minecraft:deaths").getAsInt();
                                        if (custom.has("minecraft:player_kills")) playerKills = custom.get("minecraft:player_kills").getAsInt();
                                        if (custom.has("minecraft:mob_kills")) mobKills = custom.get("minecraft:mob_kills").getAsInt();
                                        if (custom.has("minecraft:potions_brewed")) potionsBrewed = custom.get("minecraft:potions_brewed").getAsInt();
                                        if (custom.has("minecraft:fish_caught")) fishCaught = custom.get("minecraft:fish_caught").getAsInt();

                                        String[] distKeys = {
                                            "minecraft:walk_one_cm", "minecraft:crouch_one_cm", "minecraft:sprint_one_cm",
                                            "minecraft:swim_one_cm", "minecraft:fall_one_cm", "minecraft:fly_one_cm",
                                            "minecraft:climb_one_cm", "minecraft:dive_one_cm", "minecraft:walk_on_water_one_cm",
                                            "minecraft:walk_under_water_one_cm", "minecraft:strider_one_cm", "minecraft:aviate_one_cm"
                                        };
                                        for (String dk : distKeys) {
                                            if (custom.has(dk)) travelDistanceCm += custom.get(dk).getAsLong();
                                        }
                                    }

                                    if (statsObj.has("minecraft:mined")) {
                                        com.google.gson.JsonObject mined = statsObj.getAsJsonObject("minecraft:mined");
                                        for (var entry : mined.entrySet()) {
                                            int val = entry.getValue().getAsInt();
                                            blocksBroken += val;
                                            String blockKey = entry.getKey();
                                            if (blockKey.contains("wheat") || blockKey.contains("carrot") || 
                                                blockKey.contains("potato") || blockKey.contains("beetroot") ||
                                                blockKey.contains("melon") || blockKey.contains("pumpkin")) {
                                                harvests += val;
                                            }
                                        }
                                    }
                                }

                                com.google.gson.JsonObject pJson = new com.google.gson.JsonObject();
                                pJson.addProperty("playerUuid", uuidStr);
                                pJson.addProperty("username", username);
                                pJson.addProperty("playTime", playTime);
                                pJson.addProperty("travelDistance", travelDistanceCm / 100.0);
                                pJson.addProperty("blocksBroken", blocksBroken);
                                pJson.addProperty("deaths", deaths);
                                pJson.addProperty("playerKills", playerKills);
                                pJson.addProperty("mobKills", mobKills);
                                pJson.addProperty("harvests", harvests);
                                pJson.addProperty("potionsBrewed", potionsBrewed);
                                pJson.addProperty("fishCaught", fishCaught);

                                playerStatsMap.put(uuidStr, pJson);
                            }
                        }
                    }
                } catch (Exception e) {
                    LOGGER.warn("Failed to read offline player stats files: " + e.getMessage());
                }

                // 集計対象が全くいない場合はエラー
                if (playerStatsMap.isEmpty()) {
                    source.sendFailure(Component.literal("§c[エラー] 集計対象となるプレイヤー情報がありません。"));
                    return;
                }

                com.google.gson.JsonArray playersArray = new com.google.gson.JsonArray();
                for (var pStats : playerStatsMap.values()) {
                    playersArray.add(pStats);
                }

                com.google.gson.JsonObject payload = new com.google.gson.JsonObject();
                payload.add("players", playersArray);

                EconomyService.syncRanking(payload.toString()).thenAccept(res ->
                    runOnServerThread(() -> {
                        if (res != null) {
                            source.sendSuccess(() -> Component.literal("§a[経済] ランキング集計と同期が正常に完了しました！"), true);
                        } else {
                            source.sendFailure(Component.literal("§c[エラー] ランキングサーバーへの同期に失敗しました。"));
                        }
                    })
                );

            } catch (Exception e) {
                LOGGER.error("Failed to compile ranking: ", e);
                source.sendFailure(Component.literal("§c[エラー] ランキング集計中にエラーが発生しました: " + e.getMessage()));
            }
        });
    }

    public static void viewRanking(CommandSourceStack source, String metric) {
        EconomyService.fetchLatestRanking().thenAccept(res -> {
            var mcServer = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
            if (mcServer != null) {
                mcServer.execute(() -> {
                    if (res == null) {
                        source.sendSuccess(() -> Component.literal("§c[ランキング] 集計データが存在しないか、サーバーから取得できませんでした。"), true);
                        return;
                    }
                    try {
                        com.google.gson.JsonObject json = com.google.gson.JsonParser.parseString(res).getAsJsonObject();
                        com.google.gson.JsonArray records = json.getAsJsonArray("records");
                        if (records == null || records.size() == 0) {
                            source.sendSuccess(() -> Component.literal("§c[ランキング] スナップショットはありますが、記録されたデータが空です。"), true);
                            return;
                        }

                        RankingMetric rankingMetric = RankingMetric.resolve(metric);
                        String sortField = rankingMetric.sortField();
                        String label = rankingMetric.label();

                        java.util.List<com.google.gson.JsonObject> list = new java.util.ArrayList<>();
                        for (int i = 0; i < records.size(); i++) {
                            list.add(records.get(i).getAsJsonObject());
                        }

                        list.sort((a, b) -> {
                            double valA = a.has(sortField) ? a.get(sortField).getAsDouble() : 0.0;
                            double valB = b.has(sortField) ? b.get(sortField).getAsDouble() : 0.0;
                            return Double.compare(valB, valA);
                        });

                        String announcer = resolveRankingAnnouncer(source);
                        final String finalLabel = label;

                        mcServer.getPlayerList().broadcastSystemMessage(
                                Component.literal("§7[" + announcer + " が §6" + finalLabel + "ランキング §7を公開]"), false);
                        mcServer.getPlayerList().broadcastSystemMessage(
                                Component.literal("§6=== [" + finalLabel + "ランキング] ==="), false);

                        int rank = 1;
                        for (com.google.gson.JsonObject record : list) {
                            if (rank > 10) break;
                            String username = record.get("username").getAsString();
                            double val = record.has(sortField) ? record.get(sortField).getAsDouble() : 0.0;
                            String valStr = rankingMetric.formatValue(val);

                            final int finalRank = rank;
                            mcServer.getPlayerList().broadcastSystemMessage(
                                    Component.literal("§e" + finalRank + "位: §f" + username + " §a- §e" + valStr), false);
                            rank++;
                        }
                    } catch (Exception e) {
                        LOGGER.error("Failed to render ranking view: ", e);
                        source.sendFailure(Component.literal("§c[エラー] ランキング表示中にエラーが発生しました。"));
                    }
                });
            }
        });
    }

    private static String resolveRankingAnnouncer(CommandSourceStack source) {
        if (source.getEntity() instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            return EconomyNicknameBridge.resolvePlayerName(serverPlayer);
        }
        return "サーバー";
    }
}
