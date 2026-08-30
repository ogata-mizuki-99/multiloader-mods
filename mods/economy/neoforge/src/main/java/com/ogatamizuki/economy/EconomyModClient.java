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
import net.neoforged.neoforge.client.event.ClientChatReceivedEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;
import net.neoforged.neoforge.common.NeoForge;
import com.ogatamizuki.economy.client.EconomyConfigScreen;

@Mod(value = EconomyCommon.MODID, dist = Dist.CLIENT)
public class EconomyModClient {
    public EconomyModClient(ModContainer container) {
        EconomyPlatform.sendToServer = payload -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.getConnection() != null) {
                mc.getConnection().send(payload);
            }
        };

        container.registerExtensionPoint(IConfigScreenFactory.class, EconomyConfigScreen::new);

        IEventBus modEventBus = container.getEventBus();
        modEventBus.addListener(EconomyModClient::onClientSetup);
        modEventBus.addListener(EconomyModClient::onRegisterRenderers);
        modEventBus.addListener(EconomyModClient::onRegisterGuiLayers);
        modEventBus.addListener(EconomyModClient::onRegisterClientPayloads);

        NeoForge.EVENT_BUS.addListener(EconomyModClient::onClientLoggingOut);
        NeoForge.EVENT_BUS.addListener(EconomyModClient::onClientChatReceived);
    }

    private static void onClientLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        EconomyClientFeatureFlags.clear();
    }

    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
        EconomyCommon.LOGGER.info("HELLO FROM CLIENT SETUP");
        EconomyCommon.LOGGER.info("MINECRAFT NAME >> {}", Minecraft.getInstance().getUser().getName());
    }

    private static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(EconomyMod.ECONOMY_NPC.get(), ClientAccess.EconomyNpcRenderer::new);
        event.registerEntityRenderer(EconomyMod.LOAN_NPC.get(), ClientAccess.LoanNpcRenderer::new);
    }

    private static void onRegisterGuiLayers(RegisterGuiLayersEvent event) {
        EconomyCommon.LOGGER.info("Registering GUI Layer above HOTBAR");
        event.registerAbove(
                VanillaGuiLayers.HOTBAR,
                Identifier.fromNamespaceAndPath(EconomyCommon.MODID, "balance"),
                EconomyHudOverlay::render
        );
    }

    private static void onRegisterClientPayloads(RegisterClientPayloadHandlersEvent event) {
        event.register(PlayerBalanceSyncPayload.TYPE, (payload, context) -> {
            Minecraft mc = Minecraft.getInstance();
            mc.execute(() -> {
                if (mc.player != null) {
                    EconomyCommon.setEconomyReady(mc.player.getUUID(), true);
                }
                EconomyCommon.setCurrentBalance(payload.balance());
                EconomyCommon.setCurrentBankBalance(payload.bankBalance());
                EconomyCommon.setCurrentDebt(payload.debt());
                EconomyCommon.LOGGER.info("Balance synced from server: {} JPY (bank: {}, debt: {})",
                        payload.balance(), payload.bankBalance(), payload.debt());
            });
        });

        event.register(ShopTxResultPayload.TYPE, (payload, context) -> {
            Minecraft mc = Minecraft.getInstance();
            mc.execute(() -> {
                if (payload.newBalance() >= 0) {
                    EconomyCommon.setCurrentBalance(payload.newBalance());
                }

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

                if (mc.player != null) {
                    mc.player.sendSystemMessage(EconomyMasterI18n.parseChatMessage(payload.message()));
                }

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

        event.register(LoanTxResultPayload.TYPE, (payload, context) -> {
            Minecraft mc = Minecraft.getInstance();
            mc.execute(() -> {
                if (payload.newBalance() >= 0) {
                    EconomyCommon.setCurrentBalance(payload.newBalance());
                }
                if (payload.newDebt() >= 0) {
                    EconomyCommon.setCurrentDebt(payload.newDebt());
                }

                if (mc.player != null) {
                    mc.player.sendSystemMessage(EconomyMasterI18n.parseChatMessage(payload.message()));
                }

                Screen currentScreen = mc.screen;
                if (currentScreen instanceof LoanScreen loanScreen) {
                    loanScreen.onTransactionResult(payload.success());
                }
            });
        });

        event.register(StockTradeResultPayload.TYPE, (payload, context) -> {
            Minecraft mc = Minecraft.getInstance();
            mc.execute(() -> {
                if (payload.newBalance() >= 0) {
                    EconomyCommon.setCurrentBalance(payload.newBalance());
                }

                if (mc.player != null) {
                    mc.player.sendSystemMessage(EconomyMasterI18n.parseChatMessage(payload.message()));
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

        event.register(OpenShopScreenPayload.TYPE, (payload, context) -> {
            Minecraft mc = Minecraft.getInstance();
            mc.execute(() -> ClientAccess.openShopGui(payload.shopId(), payload.npcType()));
        });

        event.register(ShopDetailsResponsePayload.TYPE, (payload, context) -> ClientAccess.completeShopDetails(payload));

        event.register(EconomyQueryResponsePayload.TYPE, (payload, context) -> ClientAccess.completeQuery(payload));

        event.register(BankResultPayload.TYPE, (payload, context) -> ClientAccess.completeBankResult(payload));

        event.register(EconomyAdminResultPayload.TYPE, (payload, context) -> {
            Minecraft mc = Minecraft.getInstance();
            mc.execute(() -> {
                Component message = EconomyMasterI18n.chatMessage(payload.message());
                if (mc.player != null) {
                    mc.player.sendSystemMessage(message);
                }
                Screen currentScreen = mc.screen;
                if (currentScreen instanceof EconomyAdminScreen adminScreen) {
                    adminScreen.onActionResult(payload.success(), message.getString());
                }
            });
        });

        event.register(EconomyFeatureFlagsPayload.TYPE, (payload, context) -> {
            Minecraft.getInstance().execute(() -> EconomyClientFeatureFlags.apply(payload));
        });

        event.register(FleaMarketResultPayload.TYPE, (payload, context) -> {
            Minecraft mc = Minecraft.getInstance();
            mc.execute(() -> {
                if (payload.newBalance() >= 0) {
                    EconomyCommon.setCurrentBalance(payload.newBalance());
                }

                if (mc.player != null && payload.message() != null && !payload.message().isBlank()) {
                    mc.player.sendSystemMessage(EconomyMasterI18n.parseChatMessage(payload.message()));
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

    private static void onClientChatReceived(ClientChatReceivedEvent event) {
        Component original = event.getMessage();
        if (original != null) {
            String text = original.getString();
            if (text.startsWith("economy.chat.")) {
                event.setMessage(EconomyMasterI18n.parseChatMessage(text));
            }
        }
    }
}
