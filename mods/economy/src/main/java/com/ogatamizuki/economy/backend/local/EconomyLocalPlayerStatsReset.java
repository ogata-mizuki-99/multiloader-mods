package com.ogatamizuki.economy.backend.local;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.ogatamizuki.economy.EconomyMod;
import com.ogatamizuki.economy.data.EconomyWorldSavedData;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;

/** バニラ統計 JSON（ランキング集計ソース）の部分リセット。 */
public final class EconomyLocalPlayerStatsReset {
    private static final String[] TRAVEL_DISTANCE_KEYS = {
            "minecraft:walk_one_cm", "minecraft:crouch_one_cm", "minecraft:sprint_one_cm",
            "minecraft:swim_one_cm", "minecraft:fall_one_cm", "minecraft:fly_one_cm",
            "minecraft:climb_one_cm", "minecraft:dive_one_cm", "minecraft:walk_on_water_one_cm",
            "minecraft:walk_under_water_one_cm", "minecraft:strider_one_cm", "minecraft:aviate_one_cm"
    };

    private static final String[] HARVEST_BLOCK_SUFFIXES = {
            "wheat", "carrot", "potato", "beetroot", "melon", "pumpkin"
    };

    private EconomyLocalPlayerStatsReset() {
    }

    public record StatsResetOptions(
            boolean resetPlayTime,
            boolean resetTravelDistance,
            boolean resetBlocksBroken,
            boolean resetDeaths,
            boolean resetPlayerKills,
            boolean resetMobKills,
            boolean resetHarvests,
            boolean resetPotionsBrewed,
            boolean resetFishCaught
    ) {
        public boolean hasAny() {
            return resetPlayTime || resetTravelDistance || resetBlocksBroken || resetDeaths
                    || resetPlayerKills || resetMobKills || resetHarvests || resetPotionsBrewed
                    || resetFishCaught;
        }
    }

    public static int reset(MinecraftServer server, StatsResetOptions options) {
        if (!options.hasAny()) {
            return 0;
        }

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            try {
                player.getStats().save();
            } catch (Exception e) {
                EconomyMod.LOGGER.warn("Failed to save stats before reset for {}: {}", player.getUUID(), e.getMessage());
            }
        }

        Path statsDir;
        try {
            statsDir = server.getWorldPath(new LevelResource("players/stats")).toAbsolutePath().normalize();
        } catch (Exception e) {
            EconomyMod.LOGGER.error("Failed to resolve stats directory", e);
            return 0;
        }

        Set<String> targetUuids = new HashSet<>();
        for (UUID uuid : EconomyWorldSavedData.get(server).allPlayerMap().keySet()) {
            targetUuids.add(uuid.toString());
        }
        if (Files.isDirectory(statsDir)) {
            try (var stream = Files.list(statsDir)) {
                stream.filter(path -> path.getFileName().toString().endsWith(".json"))
                        .map(path -> path.getFileName().toString())
                        .map(name -> name.substring(0, name.length() - 5))
                        .forEach(targetUuids::add);
            } catch (Exception e) {
                EconomyMod.LOGGER.warn("Failed to list stats files: {}", e.getMessage());
            }
        }

        int filesUpdated = 0;
        for (String uuidStr : targetUuids) {
            Path file = statsDir.resolve(uuidStr + ".json");
            if (!Files.isRegularFile(file)) {
                continue;
            }
            try {
                JsonObject root = JsonParser.parseString(Files.readString(file)).getAsJsonObject();
                if (applyStatsReset(root, options)) {
                    Files.writeString(file, root.toString());
                    filesUpdated++;
                }
            } catch (Exception e) {
                EconomyMod.LOGGER.warn("Failed to reset stats file {}: {}", file, e.getMessage());
            }
        }

        // オンラインプレイヤーのメモリ上統計をファイルから再読込（直後のランキング集計で save() 上書きされないようにする）
        reloadOnlineStatsFromFiles(server, statsDir);
        return filesUpdated;
    }

    private static void reloadOnlineStatsFromFiles(MinecraftServer server, Path statsDir) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            Path file = statsDir.resolve(player.getUUID() + ".json");
            if (!Files.isRegularFile(file)) {
                continue;
            }
            try {
                player.getStats().parse(server.getFixerUpper(), JsonParser.parseString(Files.readString(file)));
                EconomyMod.LOGGER.info("Reloaded stats into memory for {}", player.getUUID());
            } catch (Exception e) {
                EconomyMod.LOGGER.warn(
                        "Failed to reload stats into memory for {}: {}",
                        player.getUUID(),
                        e.getMessage()
                );
            }
        }
    }

    static boolean applyStatsReset(JsonObject root, StatsResetOptions options) {
        if (!root.has("stats")) {
            return false;
        }
        JsonObject statsObj = root.getAsJsonObject("stats");
        boolean changed = false;

        if (statsObj.has("minecraft:custom")) {
            JsonObject custom = statsObj.getAsJsonObject("minecraft:custom");
            if (options.resetPlayTime() && zero(custom, "minecraft:play_time")) {
                changed = true;
            }
            if (options.resetDeaths() && zero(custom, "minecraft:deaths")) {
                changed = true;
            }
            if (options.resetPlayerKills() && zero(custom, "minecraft:player_kills")) {
                changed = true;
            }
            if (options.resetMobKills() && zero(custom, "minecraft:mob_kills")) {
                changed = true;
            }
            if (options.resetPotionsBrewed() && zero(custom, "minecraft:potions_brewed")) {
                changed = true;
            }
            if (options.resetFishCaught() && zero(custom, "minecraft:fish_caught")) {
                changed = true;
            }
            if (options.resetTravelDistance()) {
                for (String key : TRAVEL_DISTANCE_KEYS) {
                    if (zero(custom, key)) {
                        changed = true;
                    }
                }
            }
        }

        if (statsObj.has("minecraft:mined")) {
            JsonObject mined = statsObj.getAsJsonObject("minecraft:mined");
            if (options.resetBlocksBroken()) {
                for (Map.Entry<String, JsonElement> entry : mined.entrySet()) {
                    if (entry.getValue().getAsInt() != 0) {
                        entry.setValue(new com.google.gson.JsonPrimitive(0));
                        changed = true;
                    }
                }
            } else if (options.resetHarvests()) {
                for (Map.Entry<String, JsonElement> entry : mined.entrySet()) {
                    String key = entry.getKey();
                    if (isHarvestBlock(key) && entry.getValue().getAsInt() != 0) {
                        entry.setValue(new com.google.gson.JsonPrimitive(0));
                        changed = true;
                    }
                }
            }
        }

        return changed;
    }

    private static boolean isHarvestBlock(String blockKey) {
        for (String suffix : HARVEST_BLOCK_SUFFIXES) {
            if (blockKey.contains(suffix)) {
                return true;
            }
        }
        return false;
    }

    private static boolean zero(JsonObject obj, String key) {
        if (!obj.has(key) || obj.get(key).getAsInt() == 0) {
            return false;
        }
        obj.addProperty(key, 0);
        return true;
    }
}
