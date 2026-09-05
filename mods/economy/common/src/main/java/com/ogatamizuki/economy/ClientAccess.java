package com.ogatamizuki.economy;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/**
 * 物理クライアント専用コード（Minecraft クラスや Screen クラスなど）へのアクセスを隔離するクラス。
 * 専用サーバー環境（Dedicated Server）でクラスロード時にクラッシュするのを防ぎます。
 */
public class ClientAccess {
    private static final ConcurrentHashMap<Integer, CompletableFuture<String>> PENDING_SHOP_DETAILS = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Integer, ShopDetailsChunkAssembly> PENDING_SHOP_DETAIL_CHUNKS = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, CompletableFuture<String>> PENDING_QUERIES = new ConcurrentHashMap<>();
    private static volatile CompletableFuture<Void> pendingBankRequest;

    private static String queryKey(String queryType, String arg1, int arg2) {
        return queryType + "|" + arg1 + "|" + arg2;
    }

    public static CompletableFuture<String> requestQuery(String queryType, String arg1, int arg2) {
        String key = queryKey(queryType, arg1, arg2);
        CompletableFuture<String> future = new CompletableFuture<>();
        PENDING_QUERIES.put(key, future);
        EconomyPlatform.sendToServer(new EconomyQueryRequestPayload(queryType, arg1, arg2));
        return future;
    }

    /**
     * MASTER_ITEMS は件数が多く 1 パケットに収まらないため、ページ単位で取得して結合する。
     * arg2 = page index。
     */
    public static CompletableFuture<String> requestMasterItemsAll() {
        return requestMasterItemsPage(0, new JsonArray(), null);
    }

    private static CompletableFuture<String> requestMasterItemsPage(int page, JsonArray accumulated, String sourceHint) {
        return requestQuery("MASTER_ITEMS", "", page).thenCompose(json -> {
            if (json == null || "null".equals(json)) {
                return CompletableFuture.completedFuture("null");
            }
            try {
                JsonObject root = JsonParser.parseString(json).getAsJsonObject();
                String hint = sourceHint;
                if (hint == null && root.has("sourceHint") && !root.get("sourceHint").isJsonNull()) {
                    hint = root.get("sourceHint").getAsString();
                }
                JsonArray entries = root.getAsJsonArray("entries");
                if (entries != null) {
                    entries.forEach(accumulated::add);
                }
                int pageCount = root.has("pageCount") ? root.get("pageCount").getAsInt() : 1;
                if (page + 1 >= pageCount) {
                    JsonObject merged = new JsonObject();
                    merged.addProperty("total", root.has("total") ? root.get("total").getAsInt() : accumulated.size());
                    if (hint != null) {
                        merged.addProperty("sourceHint", hint);
                    }
                    merged.add("entries", accumulated);
                    return CompletableFuture.completedFuture(merged.toString());
                }
                return requestMasterItemsPage(page + 1, accumulated, hint);
            } catch (Exception e) {
                EconomyCommon.LOGGER.error("Failed to assemble MASTER_ITEMS pages", e);
                return CompletableFuture.completedFuture("null");
            }
        });
    }

    public static void completeQuery(EconomyQueryResponsePayload payload) {
        Minecraft.getInstance().execute(() -> {
            String key = queryKey(payload.queryType(), payload.arg1(), payload.arg2());
            CompletableFuture<String> pending = PENDING_QUERIES.remove(key);
            if (pending != null) {
                pending.complete(payload.json());
            }
        });
    }

    public static CompletableFuture<Void> requestDepositBank(int amount) {
        pendingBankRequest = new CompletableFuture<>();
        EconomyPlatform.sendToServer(new BankRequestPayload("DEPOSIT", amount));
        return pendingBankRequest;
    }

    public static CompletableFuture<Void> requestWithdrawBank(int amount) {
        pendingBankRequest = new CompletableFuture<>();
        EconomyPlatform.sendToServer(new BankRequestPayload("WITHDRAW", amount));
        return pendingBankRequest;
    }

    public static CompletableFuture<String> requestShopDetails(int shopId) {
        CompletableFuture<String> future = new CompletableFuture<>();
        PENDING_SHOP_DETAILS.put(shopId, future);
        // 前回の未完了チャンク組み立てを破棄（連打・再オープン時の混線防止）
        PENDING_SHOP_DETAIL_CHUNKS.remove(shopId);
        EconomyPlatform.sendToServer(new ShopDetailsRequestPayload(shopId));
        return future;
    }

    public static void completeBankResult(BankResultPayload payload) {
        Minecraft.getInstance().execute(() -> {
            if (payload.success()) {
                EconomyCommon.setCurrentBalance(payload.balance());
                EconomyCommon.setCurrentBankBalance(payload.bankBalance());
                EconomyCommon.setCurrentDebt(payload.debt());
            }
            CompletableFuture<Void> pending = pendingBankRequest;
            if (pending != null) {
                pending.complete(null);
                pendingBankRequest = null;
            }
        });
    }

    public static void completeShopDetails(ShopDetailsResponsePayload payload) {
        Minecraft.getInstance().execute(() -> {
            CompletableFuture<String> pending = PENDING_SHOP_DETAILS.get(payload.shopId());
            if (pending == null || pending.isDone()) {
                return;
            }
            if (payload.totalChunks() <= 1) {
                PENDING_SHOP_DETAIL_CHUNKS.remove(payload.shopId());
                PENDING_SHOP_DETAILS.remove(payload.shopId());
                pending.complete(payload.json());
                return;
            }

            ShopDetailsChunkAssembly assembly = PENDING_SHOP_DETAIL_CHUNKS.computeIfAbsent(
                    payload.shopId(),
                    ignored -> new ShopDetailsChunkAssembly(payload.totalChunks())
            );
            // チャンク数不一致（古い応答）は破棄して作り直す
            if (assembly.expectedChunks() != payload.totalChunks()) {
                assembly = new ShopDetailsChunkAssembly(payload.totalChunks());
                PENDING_SHOP_DETAIL_CHUNKS.put(payload.shopId(), assembly);
            }
            assembly.put(payload.chunkIndex(), payload.json());
            if (!assembly.isComplete()) {
                return;
            }

            PENDING_SHOP_DETAIL_CHUNKS.remove(payload.shopId());
            PENDING_SHOP_DETAILS.remove(payload.shopId());
            pending.complete(assembly.merge());
        });
    }

    private static final class ShopDetailsChunkAssembly {
        private final String[] parts;
        private int received;

        private ShopDetailsChunkAssembly(int totalChunks) {
            this.parts = new String[Math.max(1, totalChunks)];
        }

        private int expectedChunks() {
            return parts.length;
        }

        private void put(int chunkIndex, String json) {
            if (chunkIndex < 0 || chunkIndex >= parts.length || parts[chunkIndex] != null) {
                return;
            }
            parts[chunkIndex] = json;
            received++;
        }

        private boolean isComplete() {
            return received == parts.length;
        }

        private String merge() {
            JsonObject merged = JsonParser.parseString(parts[0]).getAsJsonObject();
            JsonArray mergedItems = merged.getAsJsonArray("items");
            for (int i = 1; i < parts.length; i++) {
                JsonArray partItems = JsonParser.parseString(parts[i]).getAsJsonObject().getAsJsonArray("items");
                partItems.forEach(mergedItems::add);
            }
            return merged.toString();
        }
    }

    public static void openAtmScreen() {
        Minecraft.getInstance().gui.setScreen(new AtmScreen());
    }

    public static void openAdminScreen() {
        Minecraft.getInstance().gui.setScreen(new EconomyAdminScreen());
    }

    public static void openShopGui(int shopId, String npcType) {
        if ("STOCK_TRADER".equalsIgnoreCase(npcType)) {
            openStockTradeScreen();
        } else if ("FLEA_MARKET".equalsIgnoreCase(npcType)) {
            Minecraft.getInstance().gui.setScreen(new FleaMarketScreen());
        } else {
            Minecraft.getInstance().gui.setScreen(new ShopScreen(shopId, npcType));
        }
    }

    public static void openStockTradeScreen() {
        Minecraft.getInstance().gui.setScreen(new StockTradeScreen());
    }

    public static void openRankingScreen() {
        if (!EconomyCommon.isEconomyReady()) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) {
                mc.player.sendSystemMessage(Component.translatable("economy.chat.ranking_not_ready"));
            }
            return;
        }

        EconomyService.fetchLatestRanking().thenAccept(res -> {
            Minecraft.getInstance().execute(() -> {
                Minecraft mc = Minecraft.getInstance();
                if (mc.player == null) {
                    return;
                }
                if (res == null) {
                    mc.player.sendSystemMessage(Component.translatable("economy.chat.ranking_no_data"));
                    return;
                }
                try {
                    com.google.gson.JsonObject json = com.google.gson.JsonParser.parseString(res).getAsJsonObject();
                    com.google.gson.JsonArray records = json.getAsJsonArray("records");
                    if (records == null || records.isEmpty()) {
                        mc.player.sendSystemMessage(Component.translatable("economy.chat.ranking_empty"));
                        return;
                    }
                    mc.gui.setScreen(new RankingScreen(null, json));
                } catch (Exception e) {
                    EconomyCommon.LOGGER.error("Failed to open ranking viewer: ", e);
                    mc.player.sendSystemMessage(Component.translatable("economy.chat.ranking_view_error"));
                }
            });
        });
    }

    public static void openLoanScreen() {
        Minecraft.getInstance().gui.setScreen(new LoanScreen());
    }

    public static class LoanNpcRenderer extends net.minecraft.client.renderer.entity.MobRenderer<LoanNpc, net.minecraft.client.renderer.entity.state.IllagerRenderState, net.minecraft.client.model.monster.illager.IllagerModel<net.minecraft.client.renderer.entity.state.IllagerRenderState>> {
        private static final net.minecraft.resources.Identifier PILLAGER_TEXTURE = net.minecraft.resources.Identifier.fromNamespaceAndPath("minecraft", "textures/entity/illager/pillager.png");

        public LoanNpcRenderer(net.minecraft.client.renderer.entity.EntityRendererProvider.Context context) {
            super(context, new net.minecraft.client.model.monster.illager.IllagerModel<>(context.bakeLayer(net.minecraft.client.model.geom.ModelLayers.PILLAGER)), 0.5F);
        }

        @Override
        public net.minecraft.client.renderer.entity.state.IllagerRenderState createRenderState() {
            return new net.minecraft.client.renderer.entity.state.IllagerRenderState();
        }

        @Override
        public void extractRenderState(LoanNpc entity, net.minecraft.client.renderer.entity.state.IllagerRenderState state, float partialTick) {
            super.extractRenderState(entity, state, partialTick);
        }

        @Override
        public net.minecraft.resources.Identifier getTextureLocation(net.minecraft.client.renderer.entity.state.IllagerRenderState state) {
            return PILLAGER_TEXTURE;
        }
    }

    public static class EconomyNpcRenderer extends net.minecraft.client.renderer.entity.MobRenderer<EconomyNpc, net.minecraft.client.renderer.entity.state.VillagerRenderState, net.minecraft.client.model.npc.VillagerModel> {
        private static final net.minecraft.resources.Identifier VILLAGER_BASE_TEXTURE = net.minecraft.resources.Identifier.fromNamespaceAndPath("minecraft", "textures/entity/villager/villager.png");

        public EconomyNpcRenderer(net.minecraft.client.renderer.entity.EntityRendererProvider.Context context) {
            super(context, new net.minecraft.client.model.npc.VillagerModel(context.bakeLayer(net.minecraft.client.model.geom.ModelLayers.VILLAGER)), 0.5F);
        }

        @Override
        public net.minecraft.client.renderer.entity.state.VillagerRenderState createRenderState() {
            return new net.minecraft.client.renderer.entity.state.VillagerRenderState();
        }

        @Override
        public void extractRenderState(EconomyNpc entity, net.minecraft.client.renderer.entity.state.VillagerRenderState state, float partialTick) {
            super.extractRenderState(entity, state, partialTick);
        }

        @Override
        public net.minecraft.resources.Identifier getTextureLocation(net.minecraft.client.renderer.entity.state.VillagerRenderState state) {
            return VILLAGER_BASE_TEXTURE;
        }
    }
}
