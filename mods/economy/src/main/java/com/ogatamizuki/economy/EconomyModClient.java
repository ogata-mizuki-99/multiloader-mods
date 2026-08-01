package com.ogatamizuki.economy;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;
import net.neoforged.neoforge.common.NeoForge;
import com.ogatamizuki.economy.client.EconomyConfigScreen;

// This class will not load on dedicated servers. Accessing client side code from here is safe.
@Mod(value = EconomyMod.MODID, dist = Dist.CLIENT)
public class EconomyModClient {
    public EconomyModClient(ModContainer container) {
        // Allows NeoForge to create a config screen for this mod's configs.
        container.registerExtensionPoint(IConfigScreenFactory.class, EconomyConfigScreen::new);

        // Register client event listeners on the mod-specific event bus manually to avoid annotation issues
        IEventBus modEventBus = container.getEventBus();
        modEventBus.addListener(EconomyModClient::onClientSetup);
        modEventBus.addListener(EconomyModClient::onRegisterGuiLayers);
        modEventBus.addListener(EconomyModClient::onRegisterClientPayloads);

        NeoForge.EVENT_BUS.addListener(EconomyModClient::onClientLoggingOut);

        // タイトル画面オーバーレイ（TitleScreenOverlay.ENABLED=true かつ資産配置後に解除）
        // NeoForge.EVENT_BUS.addListener(TitleScreenOverlay::onScreenOpening);
        // NeoForge.EVENT_BUS.addListener(TitleScreenOverlay::onRenderBackground);
    }

    private static void onClientLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        EconomyClientFeatureFlags.clear();
    }

    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
        // Some client setup code
        EconomyMod.LOGGER.info("HELLO FROM CLIENT SETUP");
        EconomyMod.LOGGER.info("MINECRAFT NAME >> {}", Minecraft.getInstance().getUser().getName());
    }

    private static void onRegisterGuiLayers(RegisterGuiLayersEvent event) {
        EconomyMod.LOGGER.info("Registering GUI Layer above HOTBAR");
        event.registerAbove(
            VanillaGuiLayers.HOTBAR,
            Identifier.fromNamespaceAndPath(EconomyMod.MODID, "balance"),
            EconomyHudOverlay::render
        );
    }

    /**
     * クライアント側ネットワークパケットハンドラの登録。
     * チャンネル定義は EconomyMod.registerPayloads で共通登録済み（Dedicated Server 用）。
     */
    private static void onRegisterClientPayloads(RegisterClientPayloadHandlersEvent event) {
        event.register(PlayerBalanceSyncPayload.TYPE, (payload, context) -> {
            Minecraft mc = Minecraft.getInstance();
            mc.execute(() -> {
                if (mc.player != null) {
                    EconomyMod.setEconomyReady(mc.player.getUUID(), true);
                }
                EconomyMod.setCurrentBalance(payload.balance());
                EconomyMod.setCurrentBankBalance(payload.bankBalance());
                EconomyMod.setCurrentDebt(payload.debt());
                EconomyMod.LOGGER.info("Balance synced from server: {} JPY (bank: {}, debt: {})",
                        payload.balance(), payload.bankBalance(), payload.debt());
            });
        });

        // サーバー → クライアント: 取引結果通知（チャンネル定義は EconomyMod 側）
        event.register(ShopTxResultPayload.TYPE, (payload, context) -> {
                    Minecraft mc = Minecraft.getInstance();
                    mc.execute(() -> {
                        // 所持金を更新（newBalance が有効な場合のみ）
                        if (payload.newBalance() >= 0) {
                            EconomyMod.setCurrentBalance(payload.newBalance());
                        }

                        // サーバー確定の残数でクライアントインベントリを同期
                        if (payload.success()
                                && payload.itemKey() != null
                                && !payload.itemKey().isEmpty()
                                && payload.remainingItemCount() >= 0) {
                            ClientInventorySync.syncMatchingCount(
                                    payload.itemKey(),
                                    payload.matchPotion(),
                                    payload.matchEnchantment(),
                                    payload.matchEnchantmentLevel(),
                                    payload.remainingItemCount()
                            );
                        }

                        // チャットにメッセージ表示
                        if (mc.player != null) {
                            mc.player.sendSystemMessage(Component.literal(payload.message()));
                        }

                        // 現在のScreenがShopScreenであればリフレッシュ or ロック解除
                        Screen currentScreen = mc.screen;
                        if (currentScreen instanceof ShopScreen shopScreen) {
                            if (payload.success()) {
                                shopScreen.onTransactionSuccess();
                            } else {
                                shopScreen.onTransactionFailed();
                            }
                        }
                    });
                });

        // サーバー → クライアント: 借金取引結果
        event.register(LoanTxResultPayload.TYPE, (payload, context) -> {
                    Minecraft mc = Minecraft.getInstance();
                    mc.execute(() -> {
                        if (payload.newBalance() >= 0) {
                            EconomyMod.setCurrentBalance(payload.newBalance());
                        }
                        if (payload.newDebt() >= 0) {
                            EconomyMod.setCurrentDebt(payload.newDebt());
                        }

                        if (mc.player != null) {
                            mc.player.sendSystemMessage(Component.literal(payload.message()));
                        }

                        Screen currentScreen = mc.screen;
                        if (currentScreen instanceof LoanScreen loanScreen) {
                            loanScreen.onTransactionResult(payload.success());
                        }
                    });
                });

        // サーバー → クライアント: ETF取引結果
        event.register(StockTradeResultPayload.TYPE, (payload, context) -> {
                    Minecraft mc = Minecraft.getInstance();
                    mc.execute(() -> {
                        if (payload.newBalance() >= 0) {
                            EconomyMod.setCurrentBalance(payload.newBalance());
                        }

                        if (mc.player != null) {
                            mc.player.sendSystemMessage(Component.literal(payload.message()));
                        }

                        Screen currentScreen = mc.screen;
                        if (currentScreen instanceof StockTradeScreen stockScreen) {
                            if (payload.success()) {
                                stockScreen.onTransactionSuccess(
                                        payload.newBalance(),
                                        payload.currentPrice(),
                                        payload.portfolioQuantity()
                                );
                            } else {
                                stockScreen.onTransactionFailed();
                            }
                        }
                    });
                });

        // サーバー → クライアント: ショップ画面オープン
        event.register(OpenShopScreenPayload.TYPE, (payload, context) -> {
                    Minecraft mc = Minecraft.getInstance();
                    mc.execute(() -> {
                        ClientAccess.openShopGui(payload.shopId(), payload.npcType());
                    });
                });

        event.register(ShopDetailsResponsePayload.TYPE, (payload, context) -> ClientAccess.completeShopDetails(payload));

        event.register(EconomyQueryResponsePayload.TYPE, (payload, context) -> ClientAccess.completeQuery(payload));

        event.register(BankResultPayload.TYPE, (payload, context) -> ClientAccess.completeBankResult(payload));

        event.register(EconomyAdminResultPayload.TYPE, (payload, context) -> {
            Minecraft mc = Minecraft.getInstance();
            mc.execute(() -> {
                if (mc.player != null) {
                    mc.player.sendSystemMessage(Component.literal(payload.message()));
                }
                Screen currentScreen = mc.screen;
                if (currentScreen instanceof EconomyAdminScreen adminScreen) {
                    adminScreen.onActionResult(payload.success(), payload.message());
                }
            });
        });

        event.register(EconomyFeatureFlagsPayload.TYPE, (payload, context) -> {
            Minecraft.getInstance().execute(() -> EconomyClientFeatureFlags.apply(payload));
        });

        // サーバー → クライアント: フリマ取引結果
        event.register(FleaMarketResultPayload.TYPE, (payload, context) -> {
                    Minecraft mc = Minecraft.getInstance();
                    mc.execute(() -> {
                        if (payload.newBalance() >= 0) {
                            EconomyMod.setCurrentBalance(payload.newBalance());
                        }

                        if (mc.player != null && payload.message() != null && !payload.message().isBlank()) {
                            mc.player.sendSystemMessage(Component.literal(payload.message()));
                        }

                        Screen currentScreen = mc.screen;
                        if (currentScreen instanceof FleaMarketScreen fleaScreen) {
                            if (payload.success()) {
                                fleaScreen.onTransactionSuccess();
                            } else {
                                fleaScreen.onTransactionFailed();
                            }
                        }
                    });
                });
    }
}
