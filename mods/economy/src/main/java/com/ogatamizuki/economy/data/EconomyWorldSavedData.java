package com.ogatamizuki.economy.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.ogatamizuki.economy.EconomyMod;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** ワールド単位の経済プレイヤーデータ（単体版 SSOT）。 */
public final class EconomyWorldSavedData extends SavedData {
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ISO_LOCAL_DATE;

    public record PlayerRecord(
            String username,
            int balance,
            int bankBalance,
            int debt,
            int totalEarnings,
            int totalLost,
            int etfBuyAmount,
            int etfShortAmount,
            int etfProfitAmount,
            int totalTradeCount
    ) {
        public static final Codec<PlayerRecord> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("username").forGetter(PlayerRecord::username),
                Codec.INT.fieldOf("balance").forGetter(PlayerRecord::balance),
                Codec.INT.fieldOf("bankBalance").forGetter(PlayerRecord::bankBalance),
                Codec.INT.fieldOf("debt").forGetter(PlayerRecord::debt),
                Codec.INT.optionalFieldOf("totalEarnings", 0).forGetter(PlayerRecord::totalEarnings),
                Codec.INT.optionalFieldOf("totalLost", 0).forGetter(PlayerRecord::totalLost),
                Codec.INT.optionalFieldOf("etfBuyAmount", 0).forGetter(PlayerRecord::etfBuyAmount),
                Codec.INT.optionalFieldOf("etfShortAmount", 0).forGetter(PlayerRecord::etfShortAmount),
                Codec.INT.optionalFieldOf("etfProfitAmount", 0).forGetter(PlayerRecord::etfProfitAmount),
                Codec.INT.optionalFieldOf("totalTradeCount", 0).forGetter(PlayerRecord::totalTradeCount)
        ).apply(instance, PlayerRecord::new));

        public PlayerRecord withUsername(String newUsername) {
            return new PlayerRecord(newUsername, balance, bankBalance, debt, totalEarnings, totalLost,
                    etfBuyAmount, etfShortAmount, etfProfitAmount, totalTradeCount);
        }
    }

    private static final Codec<Map<UUID, PlayerRecord>> PLAYERS_CODEC = Codec.unboundedMap(
            Codec.STRING.xmap(UUID::fromString, UUID::toString),
            PlayerRecord.CODEC
    );

    private static final Codec<Map<Integer, Integer>> INT_MAP_CODEC = Codec.unboundedMap(
            Codec.STRING.xmap(Integer::valueOf, Object::toString),
            Codec.INT
    );

    private static final Codec<Map<UUID, Map<Integer, Integer>>> PLAYER_SHOP_CODEC = Codec.unboundedMap(
            Codec.STRING.xmap(UUID::fromString, UUID::toString),
            INT_MAP_CODEC
    );

    private static final Codec<EconomyWorldSavedData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            PLAYERS_CODEC.fieldOf("players").forGetter(data -> data.players),
            Codec.STRING.optionalFieldOf("shopLimitDate", "").forGetter(data -> data.shopLimitDate),
            INT_MAP_CODEC.optionalFieldOf("globalShopPurchases", Map.of()).forGetter(data -> data.globalShopPurchases),
            PLAYER_SHOP_CODEC.optionalFieldOf("playerShopPurchases", Map.of()).forGetter(data -> data.playerShopPurchases)
    ).apply(instance, (players, shopLimitDate, globalShopPurchases, playerShopPurchases) -> {
        EconomyWorldSavedData data = new EconomyWorldSavedData();
        data.players.putAll(players);
        data.shopLimitDate = shopLimitDate;
        data.globalShopPurchases.putAll(globalShopPurchases);
        // Codec.unboundedMap は ImmutableMap を返すため、ネスト Map を mutable にコピーする
        for (Map.Entry<UUID, Map<Integer, Integer>> entry : playerShopPurchases.entrySet()) {
            data.playerShopPurchases.put(entry.getKey(), new HashMap<>(entry.getValue()));
        }
        return data;
    }));

    private static final SavedDataType<EconomyWorldSavedData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(EconomyMod.MODID, "world_data"),
            EconomyWorldSavedData::new,
            CODEC
    );

    private final Map<UUID, PlayerRecord> players = new HashMap<>();
    private String shopLimitDate = "";
    private final Map<Integer, Integer> globalShopPurchases = new HashMap<>();
    private final Map<UUID, Map<Integer, Integer>> playerShopPurchases = new HashMap<>();

    public EconomyWorldSavedData() {
    }

    public static EconomyWorldSavedData get(MinecraftServer server) {
        ServerLevel level = server.overworld();
        return level.getDataStorage().computeIfAbsent(TYPE);
    }

    private void ensureShopLimitDate() {
        String today = LocalDate.now().format(DATE_FMT);
        if (!today.equals(shopLimitDate)) {
            shopLimitDate = today;
            globalShopPurchases.clear();
            playerShopPurchases.clear();
            setDirty();
        }
    }

    public PlayerRecord getOrCreate(UUID playerUuid, String username) {
        PlayerRecord existing = players.get(playerUuid);
        if (existing == null) {
            String initialName = username == null ? "" : username;
            PlayerRecord created = new PlayerRecord(initialName, 0, 0, 0, 0, 0, 0, 0, 0, 0);
            players.put(playerUuid, created);
            setDirty();
            return created;
        }
        if (username == null || username.isBlank()) {
            return existing;
        }
        if (existing.username() == null || existing.username().isBlank() || !existing.username().equals(username)) {
            PlayerRecord updated = existing.withUsername(username);
            players.put(playerUuid, updated);
            setDirty();
            return updated;
        }
        return existing;
    }

    public void putPlayer(UUID playerUuid, PlayerRecord record) {
        players.put(playerUuid, record);
        setDirty();
    }

    public int getUserBoughtToday(UUID playerUuid, int shopItemId) {
        ensureShopLimitDate();
        return playerShopPurchases.getOrDefault(playerUuid, Map.of()).getOrDefault(shopItemId, 0);
    }

    public int getGlobalBoughtToday(int shopItemId) {
        ensureShopLimitDate();
        return globalShopPurchases.getOrDefault(shopItemId, 0);
    }

    public void addShopPurchase(UUID playerUuid, int shopItemId, int quantity) {
        ensureShopLimitDate();
        Map<Integer, Integer> purchases = playerShopPurchases.get(playerUuid);
        if (!(purchases instanceof HashMap<?, ?>)) {
            Map<Integer, Integer> mutable = new HashMap<>();
            if (purchases != null) {
                mutable.putAll(purchases);
            }
            playerShopPurchases.put(playerUuid, mutable);
            purchases = mutable;
        }
        purchases.merge(shopItemId, quantity, Integer::sum);
        globalShopPurchases.merge(shopItemId, quantity, Integer::sum);
        setDirty();
    }

    public List<PlayerRecord> allPlayers() {
        return List.copyOf(players.values());
    }

    public Map<UUID, PlayerRecord> allPlayerMap() {
        return Map.copyOf(players);
    }

    public int resetPlayers(boolean resetBalances, boolean resetRankingMetrics) {
        if (!resetBalances && !resetRankingMetrics) {
            return 0;
        }
        int count = 0;
        for (Map.Entry<UUID, PlayerRecord> entry : players.entrySet()) {
            PlayerRecord current = entry.getValue();
            PlayerRecord updated = new PlayerRecord(
                    current.username(),
                    resetBalances ? 0 : current.balance(),
                    resetBalances ? 0 : current.bankBalance(),
                    resetBalances ? 0 : current.debt(),
                    resetRankingMetrics ? 0 : current.totalEarnings(),
                    resetRankingMetrics ? 0 : current.totalLost(),
                    resetRankingMetrics ? 0 : current.etfBuyAmount(),
                    resetRankingMetrics ? 0 : current.etfShortAmount(),
                    resetRankingMetrics ? 0 : current.etfProfitAmount(),
                    resetRankingMetrics ? 0 : current.totalTradeCount()
            );
            players.put(entry.getKey(), updated);
            count++;
        }
        setDirty();
        return count;
    }

    public void clearShopLimits() {
        shopLimitDate = "";
        globalShopPurchases.clear();
        playerShopPurchases.clear();
        setDirty();
    }
}
