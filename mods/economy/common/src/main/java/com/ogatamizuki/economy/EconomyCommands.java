package com.ogatamizuki.economy;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class EconomyCommands {
    private EconomyCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        EconomyCommon.LOGGER.info("Registering Economy commands");
        dispatcher.register(
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

    public static int giveSpawnEgg(CommandSourceStack source, int shopId, String npcType) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();

        if (npcType == null) {
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
                        EconomyCommon.LOGGER.error("Failed to parse shop details: ", e);
                    }
                }

                final String finalType = resolvedType;
                final String finalModel = resolvedModel;
                final String finalName = resolvedName;
                MinecraftServer mcServer = EconomyCommon.getServer();
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

    public static void compileRanking(CommandSourceStack source) {
        source.sendSuccess(() -> Component.translatable("economy.chat.ranking_compile_start"), true);

        var server = source.getServer();
        final Map<String, String> onlinePlayerNames = new HashMap<>();
        for (var p : server.getPlayerList().getPlayers()) {
            String uuidStr = p.getUUID().toString();
            String username = EconomyNicknameBridge.resolvePlayerName(p);
            onlinePlayerNames.put(uuidStr, username);
            if (p instanceof ServerPlayer sp) {
                try {
                    sp.getStats().save();
                    EconomyCommon.LOGGER.info("Saved stats for player: {} ({})", username, uuidStr);
                } catch (Exception e) {
                    EconomyCommon.LOGGER.warn("Failed to save stats for online player {}: {}", username, e.getMessage());
                }
            }
        }
        EconomyCommon.LOGGER.info("Online players to compile: {}", onlinePlayerNames.size());

        final java.nio.file.Path statsPath;
        try {
            statsPath = server.getWorldPath(new net.minecraft.world.level.storage.LevelResource("players/stats"))
                    .toAbsolutePath().normalize();
            EconomyCommon.LOGGER.info("Stats directory path (normalized): {}", statsPath);
        } catch (Exception e) {
            source.sendFailure(Component.translatable("economy.chat.ranking_stats_path_fail", e.getMessage()));
            return;
        }

        java.util.concurrent.CompletableFuture.runAsync(() -> {
            try {
                Map<String, com.google.gson.JsonObject> playerStatsMap = new HashMap<>();

                try {
                    java.io.File statsDir = statsPath.toFile();
                    EconomyCommon.LOGGER.info("Stats dir exists: {}, is directory: {}", statsDir.exists(), statsDir.isDirectory());
                    if (statsDir.exists() && statsDir.isDirectory()) {
                        java.io.File[] files = statsDir.listFiles((dir, name) -> name.endsWith(".json"));
                        EconomyCommon.LOGGER.info("Stats files found: {}", files != null ? files.length : 0);
                        if (files != null) {
                            for (java.io.File file : files) {
                                String filename = file.getName();
                                String uuidStr = filename.substring(0, filename.length() - 5);

                                try {
                                    UUID.fromString(uuidStr);
                                } catch (Exception e) {
                                    continue;
                                }

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
                    EconomyCommon.LOGGER.warn("Failed to read offline player stats files: " + e.getMessage());
                }

                if (playerStatsMap.isEmpty()) {
                    source.sendFailure(Component.translatable("economy.chat.ranking_compile_no_players"));
                    return;
                }

                com.google.gson.JsonArray playersArray = new com.google.gson.JsonArray();
                for (var pStats : playerStatsMap.values()) {
                    playersArray.add(pStats);
                }

                com.google.gson.JsonObject payload = new com.google.gson.JsonObject();
                payload.add("players", playersArray);

                EconomyService.syncRanking(payload.toString()).thenAccept(res ->
                        EconomyPlatform.runOnServerThread(() -> {
                            if (res != null) {
                                source.sendSuccess(() -> Component.translatable("economy.chat.ranking_compile_done"), true);
                            } else {
                                source.sendFailure(Component.translatable("economy.chat.ranking_compile_sync_fail"));
                            }
                        })
                );

            } catch (Exception e) {
                EconomyCommon.LOGGER.error("Failed to compile ranking: ", e);
                source.sendFailure(Component.translatable("economy.chat.ranking_compile_error", String.valueOf(e.getMessage())));
            }
        });
    }

    public static void viewRanking(CommandSourceStack source, String metric) {
        EconomyService.fetchLatestRanking().thenAccept(res -> {
            MinecraftServer mcServer = EconomyCommon.getServer();
            if (mcServer != null) {
                mcServer.execute(() -> {
                    if (res == null) {
                        source.sendSuccess(() -> Component.translatable("economy.chat.ranking_no_data"), true);
                        return;
                    }
                    try {
                        com.google.gson.JsonObject json = com.google.gson.JsonParser.parseString(res).getAsJsonObject();
                        com.google.gson.JsonArray records = json.getAsJsonArray("records");
                        if (records == null || records.size() == 0) {
                            source.sendSuccess(() -> Component.translatable("economy.chat.ranking_empty"), true);
                            return;
                        }

                        RankingMetric rankingMetric = RankingMetric.resolve(metric);
                        String sortField = rankingMetric.sortField();
                        Component metricLabel = rankingMetric.labelComponent();

                        java.util.List<com.google.gson.JsonObject> list = new java.util.ArrayList<>();
                        for (int i = 0; i < records.size(); i++) {
                            list.add(records.get(i).getAsJsonObject());
                        }

                        list.sort((a, b) -> {
                            double valA = a.has(sortField) ? a.get(sortField).getAsDouble() : 0.0;
                            double valB = b.has(sortField) ? b.get(sortField).getAsDouble() : 0.0;
                            return Double.compare(valB, valA);
                        });

                        Component announcer = resolveRankingAnnouncer(source);

                        mcServer.getPlayerList().broadcastSystemMessage(
                                Component.translatable("economy.chat.ranking_announce", announcer, metricLabel), false);
                        mcServer.getPlayerList().broadcastSystemMessage(
                                Component.translatable("economy.chat.ranking_header", metricLabel), false);

                        int rank = 1;
                        for (com.google.gson.JsonObject record : list) {
                            if (rank > 10) break;
                            String username = record.get("username").getAsString();
                            double val = record.has(sortField) ? record.get(sortField).getAsDouble() : 0.0;
                            Component valComp = rankingMetric.formatValueComponent(val);

                            final int finalRank = rank;
                            mcServer.getPlayerList().broadcastSystemMessage(
                                    Component.translatable("economy.chat.ranking_entry", finalRank, username, valComp), false);
                            rank++;
                        }
                    } catch (Exception e) {
                        EconomyCommon.LOGGER.error("Failed to render ranking view: ", e);
                        source.sendFailure(Component.translatable("economy.chat.ranking_view_error"));
                    }
                });
            }
        });
    }

    private static Component resolveRankingAnnouncer(CommandSourceStack source) {
        if (source.getEntity() instanceof ServerPlayer serverPlayer) {
            return Component.literal(EconomyNicknameBridge.resolvePlayerName(serverPlayer));
        }
        return Component.translatable("economy.ranking.announcer_server");
    }
}
