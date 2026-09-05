package com.ogatamizuki.economy.data;

import com.ogatamizuki.economy.EconomyCommon;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** ETF 株価・履歴・ポートフォリオ（単体版）。 */
public final class EconomyEtfWorldSavedData extends SavedData {
    public record CategoryState(
            String code,
            String name,
            String description,
            int currentPrice,
            int seedPrice,
            boolean enabled
    ) {
        public static final Codec<CategoryState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("code").forGetter(CategoryState::code),
                Codec.STRING.fieldOf("name").forGetter(CategoryState::name),
                Codec.STRING.fieldOf("description").forGetter(CategoryState::description),
                Codec.INT.fieldOf("currentPrice").forGetter(CategoryState::currentPrice),
                Codec.INT.fieldOf("seedPrice").forGetter(CategoryState::seedPrice),
                Codec.BOOL.fieldOf("enabled").forGetter(CategoryState::enabled)
        ).apply(instance, CategoryState::new));

        public String id() {
            return code;
        }
    }

    public record HistoryEntry(int price, String source, long recordedAtEpochSec) {
        public static final Codec<HistoryEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.INT.fieldOf("price").forGetter(HistoryEntry::price),
                Codec.STRING.fieldOf("source").forGetter(HistoryEntry::source),
                Codec.LONG.fieldOf("recordedAtEpochSec").forGetter(HistoryEntry::recordedAtEpochSec)
        ).apply(instance, HistoryEntry::new));
    }

    private static final Codec<List<HistoryEntry>> HISTORY_LIST_CODEC = HistoryEntry.CODEC.listOf();

    private static final Codec<Map<String, CategoryState>> CATEGORY_CODEC = Codec.unboundedMap(
            Codec.STRING,
            CategoryState.CODEC
    );

    private static final Codec<Map<String, List<HistoryEntry>>> HISTORY_CODEC = Codec.unboundedMap(
            Codec.STRING,
            HISTORY_LIST_CODEC
    );

    private static final Codec<Map<String, Integer>> PORTFOLIO_CODEC = Codec.unboundedMap(
            Codec.STRING,
            Codec.INT
    );

    private static final Codec<Map<UUID, Map<String, Integer>>> PLAYER_PORTFOLIOS_CODEC = Codec.unboundedMap(
            Codec.STRING.xmap(UUID::fromString, UUID::toString),
            PORTFOLIO_CODEC
    );

    private static final Codec<EconomyEtfWorldSavedData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            CATEGORY_CODEC.fieldOf("categories").forGetter(data -> data.categories),
            HISTORY_CODEC.optionalFieldOf("history", Map.of()).forGetter(data -> data.history),
            PLAYER_PORTFOLIOS_CODEC.optionalFieldOf("portfolios", Map.of()).forGetter(data -> data.portfolios)
    ).apply(instance, (categories, history, portfolios) -> {
        EconomyEtfWorldSavedData data = new EconomyEtfWorldSavedData();
        data.categories.putAll(categories);
        // Codec の listOf / unboundedMap は不変コレクションを返すことがあるため、書き込み可能なコピーを保持する。
        history.forEach((code, entries) -> data.history.put(code, new ArrayList<>(entries)));
        portfolios.forEach((uuid, holdings) -> data.portfolios.put(uuid, new HashMap<>(holdings)));
        return data;
    }));

    private static final SavedDataType<EconomyEtfWorldSavedData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(EconomyCommon.MODID, "etf_world_data"),
            EconomyEtfWorldSavedData::new,
            CODEC,
            null
    );

    private static final CategoryState[] INITIAL_CATEGORIES = {
            new CategoryState("新興工業", "新興工業ETF", "工業資源の売買に連動するETF", 100, 100, true),
            new CategoryState("大衆バイオ", "大衆バイオETF", "大衆食材・バイオ資源の売買に連動するETF", 100, 100, true),
            new CategoryState("狩猟開拓", "狩猟開拓ETF", "狩猟素材・開拓資源の売買に連動するETF", 100, 100, true),
            new CategoryState("貴金属のみ", "貴金属ETF", "高級鉱石・貴金属に特化したETF（要アンロック）", 5000, 5000, false),
            new CategoryState("高級食料のみ", "高級食料ETF", "高級食料に特化したETF（要アンロック）", 5000, 5000, false),
            new CategoryState("魔物ドロップ", "魔物ドロップETF", "モンスタードロップ品の売買に連動するETF", 100, 100, true),
    };

    /** カテゴリあたりの履歴上限（永続化サイズの抑制）。 */
    private static final int HISTORY_MAX_ENTRIES = 500;

    private final Map<String, CategoryState> categories = new HashMap<>();
    private final Map<String, List<HistoryEntry>> history = new HashMap<>();
    private final Map<UUID, Map<String, Integer>> portfolios = new HashMap<>();

    public EconomyEtfWorldSavedData() {
    }

    public static EconomyEtfWorldSavedData get(MinecraftServer server) {
        ServerLevel level = server.overworld();
        EconomyEtfWorldSavedData data = level.getDataStorage().computeIfAbsent(TYPE);
        data.ensureSeeded();
        return data;
    }

    private void ensureSeeded() {
        long now = System.currentTimeMillis() / 1000L;
        if (categories.isEmpty()) {
            for (CategoryState category : INITIAL_CATEGORIES) {
                categories.put(category.code(), category);
                history.put(category.code(), new ArrayList<>(List.of(new HistoryEntry(category.seedPrice(), "SEED", now))));
            }
            setDirty();
            EconomyCommon.LOGGER.info("Seeded {} ETF categories", categories.size());
            return;
        }
        boolean changed = false;
        for (CategoryState category : categories.values()) {
            List<HistoryEntry> entries = history.get(category.code());
            if (entries != null && !entries.isEmpty()) {
                continue;
            }
            history.put(category.code(), new ArrayList<>(List.of(
                    new HistoryEntry(category.currentPrice(), "SEED", now))));
            changed = true;
        }
        if (changed) {
            setDirty();
            EconomyCommon.LOGGER.info("Backfilled missing ETF price history for {} categories", categories.size());
        }
    }

    public List<CategoryState> enabledCategories() {
        return categories.values().stream()
                .filter(CategoryState::enabled)
                .sorted((a, b) -> a.code().compareTo(b.code()))
                .toList();
    }

    public CategoryState category(String code) {
        return categories.get(code);
    }

    public int portfolioQuantity(UUID playerUuid, String code) {
        return portfolios.getOrDefault(playerUuid, Map.of()).getOrDefault(code, 0);
    }

    public Map<String, Integer> portfolio(UUID playerUuid) {
        return Map.copyOf(portfolios.getOrDefault(playerUuid, Map.of()));
    }

    public void setPortfolioQuantity(UUID playerUuid, String code, int quantity) {
        if (quantity == 0) {
            Map<String, Integer> current = portfolios.get(playerUuid);
            if (current != null) {
                current.remove(code);
                if (current.isEmpty()) {
                    portfolios.remove(playerUuid);
                }
            }
        } else {
            Map<String, Integer> current = portfolios.computeIfAbsent(playerUuid, k -> new HashMap<>());
            if (!(current instanceof HashMap)) {
                current = new HashMap<>(current);
                portfolios.put(playerUuid, current);
            }
            current.put(code, quantity);
        }
        setDirty();
    }

    public void updateCategoryPrice(String code, int newPrice, String source) {
        CategoryState current = categories.get(code);
        if (current == null || current.currentPrice() == newPrice) {
            return;
        }
        categories.put(code, new CategoryState(
                current.code(), current.name(), current.description(),
                newPrice, current.seedPrice(), current.enabled()
        ));
        long now = System.currentTimeMillis() / 1000L;
        List<HistoryEntry> entries = history.computeIfAbsent(code, k -> new ArrayList<>());
        if (!(entries instanceof ArrayList)) {
            entries = new ArrayList<>(entries);
            history.put(code, entries);
        }
        entries.add(new HistoryEntry(newPrice, source, now));
        trimHistory(entries);
        setDirty();
    }

    private static void trimHistory(List<HistoryEntry> entries) {
        if (entries.size() <= HISTORY_MAX_ENTRIES) {
            return;
        }
        entries.subList(0, entries.size() - HISTORY_MAX_ENTRIES).clear();
    }

    public List<HistoryEntry> priceHistory(String code, int limit) {
        List<HistoryEntry> entries = history.getOrDefault(code, List.of());
        if (entries.size() <= limit) {
            return List.copyOf(entries);
        }
        return List.copyOf(entries.subList(entries.size() - limit, entries.size()));
    }

    public void clearPortfolios() {
        portfolios.clear();
        setDirty();
    }

    public void resetPricesToSeed() {
        categories.clear();
        history.clear();
        ensureSeeded();
    }
}
