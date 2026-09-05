package com.ogatamizuki.economy.fabric;

import com.ogatamizuki.economy.*;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class EconomyModFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        EconomyPlatform.isClientSupplier = () -> true;
        EconomyPlatform.sendToServer = ClientPlayNetworking::send;

        EconomyCommon.LOGGER.info("Economy Mod (Fabric Client) Initializing...");

        EntityRendererRegistry.register(EconomyRegistries.ECONOMY_NPC, ClientAccess.EconomyNpcRenderer::new);
        EntityRendererRegistry.register(EconomyRegistries.LOAN_NPC, ClientAccess.LoanNpcRenderer::new);

        HudElementRegistry.attachElementAfter(
                VanillaHudElements.HOTBAR,
                EconomyCommon.id("balance"),
                EconomyHudOverlay::render
        );

        // NeoForge ClientChatReceivedEvent 相当: サーバーから送る economy.chat.* パイプ形式をクライアント言語で展開
        ClientReceiveMessageEvents.MODIFY_GAME.register((message, overlay) -> {
            if (message == null) {
                return message;
            }
            String text = message.getString();
            if (text.startsWith("economy.chat.")) {
                return EconomyMasterI18n.parseChatMessage(text);
            }
            return message;
        });

        registerClientPayloads();

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> EconomyClientFeatureFlags.clear());

        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> EconomyClientFeatureFlags.clear());
    }

    private static void registerClientPayloads() {
        ClientPlayNetworking.registerGlobalReceiver(PlayerBalanceSyncPayload.TYPE, (payload, context) -> {
            Minecraft mc = context.client();
            mc.execute(() -> {
                if (mc.player != null) {
                    EconomyCommon.setEconomyReady(mc.player.getUUID(), true);
                }
                EconomyCommon.setCurrentBalance(payload.balance());
                EconomyCommon.setCurrentBankBalance(payload.bankBalance());
                EconomyCommon.setCurrentDebt(payload.debt());
            });
        });

        ClientPlayNetworking.registerGlobalReceiver(ShopTxResultPayload.TYPE, (payload, context) -> {
            Minecraft mc = context.client();
            mc.execute(() -> handleShopTxResult(mc, payload));
        });

        ClientPlayNetworking.registerGlobalReceiver(LoanTxResultPayload.TYPE, (payload, context) -> {
            Minecraft mc = context.client();
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
                Screen currentScreen = mc.gui.screen();
                if (currentScreen instanceof LoanScreen loanScreen) {
                    loanScreen.onTransactionResult(payload.success());
                }
            });
        });

        ClientPlayNetworking.registerGlobalReceiver(StockTradeResultPayload.TYPE, (payload, context) -> {
            Minecraft mc = context.client();
            mc.execute(() -> {
                if (payload.newBalance() >= 0) {
                    EconomyCommon.setCurrentBalance(payload.newBalance());
                }
                if (mc.player != null) {
                    mc.player.sendSystemMessage(EconomyMasterI18n.parseChatMessage(payload.message()));
                }
                Screen currentScreen = mc.gui.screen();
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

        ClientPlayNetworking.registerGlobalReceiver(OpenShopScreenPayload.TYPE, (payload, context) ->
                context.client().execute(() -> ClientAccess.openShopGui(payload.shopId(), payload.npcType())));

        ClientPlayNetworking.registerGlobalReceiver(ShopDetailsResponsePayload.TYPE, (payload, context) ->
                context.client().execute(() -> ClientAccess.completeShopDetails(payload)));

        ClientPlayNetworking.registerGlobalReceiver(EconomyQueryResponsePayload.TYPE, (payload, context) ->
                context.client().execute(() -> ClientAccess.completeQuery(payload)));

        ClientPlayNetworking.registerGlobalReceiver(BankResultPayload.TYPE, (payload, context) ->
                context.client().execute(() -> ClientAccess.completeBankResult(payload)));

        ClientPlayNetworking.registerGlobalReceiver(EconomyAdminResultPayload.TYPE, (payload, context) -> {
            Minecraft mc = context.client();
            mc.execute(() -> {
                Component message = EconomyMasterI18n.chatMessage(payload.message());
                if (mc.player != null) {
                    mc.player.sendSystemMessage(message);
                }
                Screen currentScreen = mc.gui.screen();
                if (currentScreen instanceof EconomyAdminScreen adminScreen) {
                    adminScreen.onActionResult(payload.success(), message.getString());
                }
            });
        });

        ClientPlayNetworking.registerGlobalReceiver(EconomyFeatureFlagsPayload.TYPE, (payload, context) ->
                context.client().execute(() -> EconomyClientFeatureFlags.apply(payload)));

        ClientPlayNetworking.registerGlobalReceiver(FleaMarketResultPayload.TYPE, (payload, context) -> {
            Minecraft mc = context.client();
            mc.execute(() -> {
                if (payload.newBalance() >= 0) {
                    EconomyCommon.setCurrentBalance(payload.newBalance());
                }
                if (mc.player != null && payload.message() != null && !payload.message().isBlank()) {
                    mc.player.sendSystemMessage(EconomyMasterI18n.parseChatMessage(payload.message()));
                }
                Screen currentScreen = mc.gui.screen();
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

    private static void handleShopTxResult(Minecraft mc, ShopTxResultPayload payload) {
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
        Screen currentScreen = mc.gui.screen();
        if (currentScreen instanceof ShopScreen shopScreen) {
            if (payload.success()) {
                shopScreen.onTransactionSuccess();
            } else {
                shopScreen.onTransactionFailed();
            }
        }
    }
}
