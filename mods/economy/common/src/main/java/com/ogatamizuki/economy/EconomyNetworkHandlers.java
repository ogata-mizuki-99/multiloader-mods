package com.ogatamizuki.economy;

import com.ogatamizuki.economy.backend.EconomyBalanceSync;
import com.ogatamizuki.economy.backend.EconomyEtfPriceScheduler;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;
import java.util.UUID;

public final class EconomyNetworkHandlers {
    private EconomyNetworkHandlers() {}

    public static void handleCommonConfigPush(EconomyCommonConfigPushPayload payload, ServerPlayer serverPlayer) {
        if (!EconomyAdminAuth.canPerformAdminActions(serverPlayer)) {
            serverPlayer.sendSystemMessage(
                    Component.translatable("economy.configuration.push_denied")
                            .withStyle(ChatFormatting.RED));
            return;
        }
        int aggregate = Math.max(0, Math.min(30, payload.rewardChatAggregateSeconds()));
        EconomyRuntimeConfig.enableBalanceHud = payload.enableBalanceHud();
        EconomyRuntimeConfig.enableActionRewards = payload.enableActionRewards();
        EconomyRuntimeConfig.enableEtfUpdates = payload.enableEtfUpdates();
        EconomyRuntimeConfig.rewardChatAggregateSeconds = aggregate;
        EconomyPlatform.persistRuntimeConfig.run();

        EconomyFeatures.syncToAllPlayers();
        EconomyEtfPriceScheduler.stop();
        MinecraftServer server = serverPlayer.level().getServer();
        if (server != null) {
            EconomyEtfPriceScheduler.start(server);
        }
        EconomyCommon.LOGGER.info(
                "Economy common config pushed by {}: hud={}, actionRewards={}, etfUpdates={}, rewardChatAggregateSeconds={}",
                serverPlayer.getGameProfile().name(),
                EconomyRuntimeConfig.enableBalanceHud,
                EconomyRuntimeConfig.enableActionRewards,
                EconomyRuntimeConfig.enableEtfUpdates,
                EconomyRuntimeConfig.rewardChatAggregateSeconds
        );
        serverPlayer.sendSystemMessage(
                Component.translatable("economy.configuration.push_ok")
                        .withStyle(ChatFormatting.GREEN));
    }

    public static void handleMasterEdit(EconomyMasterEditPayload payload, ServerPlayer serverPlayer) {
        if (!EconomyAdminAuth.canPerformAdminActions(serverPlayer)) {
            EconomyPlatform.send(
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
            EconomyPlatform.send(serverPlayer, new EconomyAdminResultPayload(true, "§a[経済] " + msg));
        } else {
            String error = result.has("error") ? result.get("error").getAsString() : "マスタ編集の反映に失敗しました。";
            EconomyPlatform.send(serverPlayer, new EconomyAdminResultPayload(false, "§c[経済] " + error));
        }
    }

    public static void handleMasterConfig(EconomyMasterConfigPayload payload, ServerPlayer serverPlayer) {
        if (!EconomyAdminAuth.canPerformAdminActions(serverPlayer)) {
            EconomyPlatform.send(
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
            EconomyPlatform.send(
                    serverPlayer,
                    new EconomyAdminResultPayload(false, "§c[経済] 不明なマスタ操作です: " + payload.action())
            );
            return;
        }

        if (result.has("success") && result.get("success").getAsBoolean()) {
            String msg = result.has("message") ? result.get("message").getAsString() : "マスタ設定を反映しました。";
            EconomyPlatform.send(serverPlayer, new EconomyAdminResultPayload(true, "§a[経済] " + msg));
        } else {
            String error = result.has("error") ? result.get("error").getAsString() : "マスタ設定の反映に失敗しました。";
            EconomyPlatform.send(serverPlayer, new EconomyAdminResultPayload(false, "§c[経済] " + error));
        }
    }

    public static void handleAdminAction(EconomyAdminActionPayload payload, ServerPlayer serverPlayer) {
        if (!EconomyAdminAuth.canPerformAdminActions(serverPlayer)) {
            EconomyPlatform.send(
                    serverPlayer,
                    new EconomyAdminResultPayload(false, "§c[経済] 管理操作の権限（管理者権限）がありません。")
            );
            return;
        }

        if ("COMPILE_RANKING".equalsIgnoreCase(payload.action())) {
            EconomyCommands.compileRanking(serverPlayer.createCommandSourceStack());
            EconomyPlatform.send(
                    serverPlayer,
                    new EconomyAdminResultPayload(true, "economy.chat.ranking_compile_started")
            );
            return;
        }

        if ("RELOAD_MASTER".equalsIgnoreCase(payload.action())) {
            com.google.gson.JsonObject result = com.ogatamizuki.economy.backend.local.EconomyLocalAdminService.reloadMaster();
            if (result.has("success") && result.get("success").getAsBoolean()) {
                String msg = result.has("message") ? result.get("message").getAsString() : "マスタを再読込しました。";
                EconomyPlatform.send(serverPlayer, new EconomyAdminResultPayload(true, "§a[経済] " + msg));
            } else {
                String error = result.has("error") ? result.get("error").getAsString() : "マスタ再読込に失敗しました。";
                EconomyPlatform.send(serverPlayer, new EconomyAdminResultPayload(false, "§c[経済] " + error));
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
            EconomyPlatform.send(
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
            EconomyPlatform.send(
                    serverPlayer,
                    new EconomyAdminResultPayload(true, "§a[経済] リセットが完了しました。（プレイヤー " + updated + " 件更新）")
            );
        } else {
            String error = result.has("error") ? result.get("error").getAsString() : "リセットに失敗しました。";
            EconomyPlatform.send(
                    serverPlayer,
                    new EconomyAdminResultPayload(false, "§c[経済] " + error)
            );
        }
    }

    public static void handleEconomyQueryRequest(EconomyQueryRequestPayload payload, ServerPlayer serverPlayer) {
        if (isAdminEconomyQuery(payload.queryType())
                && !EconomyAdminAuth.canPerformAdminActions(serverPlayer)) {
            EconomyPlatform.send(
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
        EconomyPlatform.send(
                serverPlayer,
                new EconomyQueryResponsePayload(payload.queryType(), payload.arg1(), payload.arg2(), json != null ? json : "null")
        );
    }

    public static void handleBankRequest(BankRequestPayload payload, ServerPlayer serverPlayer) {
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
        EconomyPlatform.send(
                serverPlayer,
                new BankResultPayload(success, record.balance(), record.bankBalance(), record.debt())
        );
    }

    public static void handleShopDetailsRequest(ShopDetailsRequestPayload payload, ServerPlayer serverPlayer) {
        List<String> chunks = com.ogatamizuki.economy.backend.local.EconomyLocalShopService.fetchShopDetailsChunks(
                payload.shopId(), serverPlayer.getUUID());
        if (chunks.isEmpty()) {
            EconomyPlatform.send(
                    serverPlayer,
                    new ShopDetailsResponsePayload(payload.shopId(), 0, 1, "{}")
            );
            return;
        }
        int totalChunks = chunks.size();
        for (int i = 0; i < totalChunks; i++) {
            EconomyPlatform.send(
                    serverPlayer,
                    new ShopDetailsResponsePayload(payload.shopId(), i, totalChunks, chunks.get(i))
            );
        }
    }

    public static void handleLoanRequest(LoanRequestPayload payload, ServerPlayer serverPlayer) {
        String uuid = serverPlayer.getUUID().toString();
        String action = payload.action();
        int amount = payload.amount();

        if ("BORROW".equalsIgnoreCase(action)) {
            EconomyService.borrowLoan(uuid, amount).thenAccept(res ->
                    EconomyPlatform.runOnServerThread(() -> {
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
                    EconomyPlatform.runOnServerThread(() -> {
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

    public static void handleBuyRequest(ShopBuyRequestPayload payload, ServerPlayer serverPlayer) {
        String uuid = serverPlayer.getUUID().toString();
        EconomyCommon.LOGGER.info("Shop buy request: player={} shopItemId={} qty={}",
                serverPlayer.getName().getString(), payload.shopItemId(), payload.quantity());

        EconomyService.buyShopItem(uuid, payload.shopItemId(), payload.quantity()).thenAccept(res ->
                EconomyPlatform.runOnServerThread(() -> {
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
                                    EconomyCommon.LOGGER.info("Gave {} x{} to player {}", itemKey, quantity, serverPlayer.getName().getString());
                                } else {
                                    EconomyCommon.LOGGER.warn("Item not found in registry for buy grant: {}", itemKey);
                                }
                            } catch (Exception e) {
                                EconomyCommon.LOGGER.error("Failed to grant item on buy: ", e);
                            }
                        }
                        String msg = "§a[ショップ] §e" + itemName + "§f を " + quantity + " 個購入しました！";
                        notifyShopTxResult(serverPlayer, true, newBalance, msg, syncItemKey, remainingItemCount);
                    } else {
                        String error = (res != null && res.has("error")) ? res.get("error").getAsString() : "購入に失敗しました。";
                        EconomyCommon.LOGGER.warn("Shop buy rejected: player={} shopItemId={} qty={} reason={}",
                                serverPlayer.getName().getString(), payload.shopItemId(), payload.quantity(), error);
                        notifyShopTxResult(serverPlayer, false, -1, "§c[エラー] " + error, "", -1);
                    }
                })
        ).exceptionally(ex -> {
            EconomyCommon.LOGGER.error("Shop buy request failed for {}: ", serverPlayer.getName().getString(), ex);
            EconomyPlatform.runOnServerThread(() -> notifyShopTxResult(
                    serverPlayer, false, -1, "§c[エラー] 購入処理中にエラーが発生しました。", "", -1));
            return null;
        });
    }

    public static void handleSellRequest(ShopSellRequestPayload payload, ServerPlayer serverPlayer) {
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
                EconomyPlatform.runOnServerThread(() -> {
                    if (res != null && res.has("success") && res.get("success").getAsBoolean()) {
                        int newBalance = res.has("newBalance") ? res.get("newBalance").getAsInt() : -1;
                        int totalGain = res.has("totalGain") ? res.get("totalGain").getAsInt() : 0;
                        String itemName = res.has("itemName") ? res.get("itemName").getAsString() : "アイテム";

                        if (!serverPlayer.isCreative()) {
                            EconomyItemMatcher.removeMatching(serverPlayer, itemDef, quantity);
                            serverPlayer.inventoryMenu.broadcastChanges();
                            EconomyCommon.LOGGER.info("Removed {} x{} (id={}) from player {} inventory",
                                    itemDef.itemKey(), quantity, itemId, serverPlayer.getName().getString());
                        }

                        int remainingItemCount = EconomyItemMatcher.countMatching(serverPlayer, itemDef);
                        String msg = "economy.chat.shop.sell_success|" + itemDef.itemKey() + "|" + quantity + "|" + totalGain;
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
            EconomyCommon.LOGGER.error("Shop sell request failed for {}: ", serverPlayer.getName().getString(), ex);
            EconomyPlatform.runOnServerThread(() -> notifyShopTxResult(
                    serverPlayer, false, -1, "§c[エラー] 売却処理中にエラーが発生しました。", "", -1));
            return null;
        });
    }

    public static void handleStockTradeRequest(StockTradeRequestPayload payload, ServerPlayer serverPlayer) {
        String uuid = serverPlayer.getUUID().toString();

        EconomyService.tradeStock(uuid, payload.stockCategoryId(), payload.tradeType(), payload.quantity())
                .thenAccept(res -> EconomyPlatform.runOnServerThread(() -> {
                    if (res != null && res.has("success") && res.get("success").getAsBoolean()) {
                        int newBalance = res.has("newBalance") ? res.get("newBalance").getAsInt() : -1;
                        int currentPrice = res.has("currentPrice") ? res.get("currentPrice").getAsInt() : 0;
                        int portfolioQuantity = res.has("portfolioQuantity") ? res.get("portfolioQuantity").getAsInt() : 0;

                        String msg = "economy.chat.etf.trade_success|" + newBalance;

                        EconomyPlatform.send(
                                serverPlayer,
                                new StockTradeResultPayload(true, newBalance, currentPrice, portfolioQuantity, msg)
                        );
                    } else {
                        String error = (res != null && res.has("error")) ? res.get("error").getAsString() : "取引に失敗しました。";
                        EconomyPlatform.send(
                                serverPlayer,
                                new StockTradeResultPayload(false, -1, 0, 0, "§c[エラー] " + error)
                        );
                    }
                }));
    }

    public static void handleFleaMarketListRequest(FleaMarketListRequestPayload payload, ServerPlayer serverPlayer) {
        String uuid = serverPlayer.getUUID().toString();
        int price = payload.price();
        int quantity = payload.quantity();
        ItemStack requested = com.ogatamizuki.economy.data.FleaMarketStackCodec.decode(
                serverPlayer.registryAccess(),
                payload.itemStackSnbt(),
                payload.itemKey(),
                1);
        if (requested.isEmpty() || price <= 0 || quantity <= 0) {
            EconomyPlatform.send(serverPlayer, new FleaMarketResultPayload(false, "§c[フリマ] §f出品内容が無効です。", -1));
            return;
        }

        Identifier itemId = BuiltInRegistries.ITEM.getKey(requested.getItem());
        String itemKey = itemId.toString();
        ItemStack template = requested.copyWithCount(1);

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
                EconomyPlatform.send(serverPlayer, new FleaMarketResultPayload(false, "§c[フリマ] §f出品に必要なアイテムがインベントリにありません。", -1));
                return;
            }
        } else {
            storedTemplate = template;
        }
        if (storedTemplate.isEmpty()) {
            storedTemplate = template;
        }

        final String resolvedItemName;
        String clientItemName = payload.itemName();
        if (clientItemName == null || clientItemName.isBlank()) {
            resolvedItemName = storedTemplate.getHoverName().getString();
        } else {
            resolvedItemName = clientItemName;
        }

        final ItemStack listingStack = storedTemplate.copyWithCount(1);

        EconomyService.listFleaMarketItem(uuid, itemKey, resolvedItemName, price, quantity, listingStack).thenAccept(res ->
                EconomyPlatform.runOnServerThread(() -> {
                    if (res != null && res.has("success") && res.get("success").getAsBoolean()) {
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
                        EconomyPlatform.send(serverPlayer, new FleaMarketResultPayload(true, msg, -1));
                    } else {
                        String error = (res != null && res.has("error")) ? res.get("error").getAsString() : "出品に失敗しました。";
                        EconomyPlatform.send(serverPlayer, new FleaMarketResultPayload(false, "§c[エラー] " + error, -1));
                    }
                })
        ).exceptionally(ex -> {
            EconomyCommon.LOGGER.error("Flea market list request failed for {}: ", serverPlayer.getName().getString(), ex);
            EconomyPlatform.runOnServerThread(() -> EconomyPlatform.send(
                    serverPlayer, new FleaMarketResultPayload(false, "§c[エラー] 出品処理中にエラーが発生しました。", -1)));
            return null;
        });
    }

    public static void handleFleaMarketBuyRequest(FleaMarketBuyRequestPayload payload, ServerPlayer serverPlayer) {
        String uuid = serverPlayer.getUUID().toString();

        EconomyService.buyFleaMarketItem(uuid, payload.listingId(), payload.quantity()).thenAccept(res ->
                EconomyPlatform.runOnServerThread(() -> {
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
                        EconomyPlatform.send(serverPlayer, new FleaMarketResultPayload(true, "", newBalance));
                    } else {
                        String error = (res != null && res.has("error")) ? res.get("error").getAsString() : "購入に失敗しました。";
                        EconomyPlatform.send(serverPlayer, new FleaMarketResultPayload(false, "§c[エラー] " + error, -1));
                    }
                })
        );
    }

    public static void handleFleaMarketCancelRequest(FleaMarketCancelRequestPayload payload, ServerPlayer serverPlayer) {
        String uuid = serverPlayer.getUUID().toString();

        EconomyService.cancelFleaMarketListing(uuid, payload.listingId()).thenAccept(res ->
                EconomyPlatform.runOnServerThread(() -> {
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
                        EconomyPlatform.send(serverPlayer, new FleaMarketResultPayload(true, "", -1));
                    } else {
                        String error = (res != null && res.has("error")) ? res.get("error").getAsString() : "出品取消に失敗しました。";
                        EconomyPlatform.send(serverPlayer, new FleaMarketResultPayload(false, "§c[エラー] " + error, -1));
                    }
                })
        );
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

    private static void sendAdminJsonResult(ServerPlayer serverPlayer, com.google.gson.JsonObject result) {
        if (result.has("success") && result.get("success").getAsBoolean()) {
            String msg = result.has("message") ? result.get("message").getAsString() : "操作が完了しました。";
            EconomyPlatform.send(serverPlayer, new EconomyAdminResultPayload(true, "§a[経済] " + msg));
        } else {
            String error = result.has("error") ? result.get("error").getAsString() : "操作に失敗しました。";
            EconomyPlatform.send(serverPlayer, new EconomyAdminResultPayload(false, "§c[経済] " + error));
        }
    }

    private static void notifyLoanTxResult(
            ServerPlayer serverPlayer,
            boolean success,
            int newBalance,
            int newDebt,
            String message
    ) {
        EconomyPlatform.send(
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

    private static void notifyShopTxResult(
            ServerPlayer serverPlayer,
            boolean success,
            int newBalance,
            String message,
            String itemKey,
            int remainingItemCount
    ) {
        EconomyPlatform.send(
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
        EconomyPlatform.send(
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

    private static void grantFleaMarketStacks(ServerPlayer serverPlayer, String stackNbt, String itemKey, int quantity) {
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
            EconomyCommon.LOGGER.error("Failed to grant flea market item stacks: ", e);
        }
    }
}
